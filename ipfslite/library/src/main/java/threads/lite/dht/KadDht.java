package threads.lite.dht;

import android.annotation.SuppressLint;
import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.protobuf.ByteString;

import net.luminis.quic.QuicConnection;
import net.luminis.quic.QuicStream;

import java.io.OutputStream;
import java.net.ConnectException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import dht.pb.Dht;
import record.pb.RecordOuterClass;
import threads.lite.IPFS;
import threads.lite.LogUtils;
import threads.lite.cid.Cid;
import threads.lite.cid.Multiaddr;
import threads.lite.cid.Peer;
import threads.lite.cid.PeerId;
import threads.lite.cid.Protocol;
import threads.lite.core.Closeable;
import threads.lite.core.ClosedException;
import threads.lite.core.RecordIssue;
import threads.lite.host.DnsResolver;
import threads.lite.host.LiteHost;
import threads.lite.ipns.Ipns;
import threads.lite.ipns.Validator;
import threads.lite.utils.DataHandler;
import threads.lite.utils.ReaderHandler;


public class KadDht implements Routing {

    private static final String TAG = KadDht.class.getSimpleName();
    public final LiteHost host;
    public final PeerId self;

    public final int bucketSize;
    public final int alpha;
    @NonNull
    public final RoutingTable routingTable;
    private final Validator validator;


    public KadDht(@NonNull LiteHost host, @NonNull Validator validator,
                  int alpha, int bucketSize) {
        this.host = host;
        this.validator = validator;
        this.self = host.self();
        this.bucketSize = bucketSize;
        this.routingTable = new RoutingTable(bucketSize, ID.convertPeerID(self));
        this.alpha = alpha;
    }


    void bootstrap() {
        // Fill routing table with currently connected peers that are DHT servers
        if (routingTable.isEmpty()) {
            synchronized (TAG.intern()) {
                try {
                    Set<String> addresses = new HashSet<>(IPFS.DHT_BOOTSTRAP_NODES);

                    for (String multiAddress : addresses) {
                        try {
                            Multiaddr multiaddr = new Multiaddr(multiAddress);
                            String name = multiaddr.getStringComponent(Protocol.Type.P2P);
                            Objects.requireNonNull(name);
                            PeerId peerId = PeerId.fromBase58(name);
                            Objects.requireNonNull(peerId);

                            Set<Multiaddr> result = DnsResolver.resolveDnsAddress(multiaddr);
                            peerFound(new Peer(peerId, result), false);

                        } catch (Throwable throwable) {
                            LogUtils.error(TAG, throwable);
                        }
                    }
                } catch (Throwable throwable) {
                    LogUtils.error(TAG, throwable);
                }
            }
        }
    }

    void peerFound(Peer p, boolean isReplaceable) {
        try {
            routingTable.addPeer(p, isReplaceable);
        } catch (Throwable throwable) {
            LogUtils.error(TAG, throwable);
        }
    }


    @NonNull
    private Set<Peer> evalClosestPeers(@NonNull Dht.Message pms) {
        Set<Peer> peers = new HashSet<>();
        List<Dht.Message.Peer> list = pms.getCloserPeersList();
        for (Dht.Message.Peer entry : list) {
            PeerId peerId = new PeerId(entry.getId().toByteArray());


            Set<Multiaddr> multiAddresses = new HashSet<>();
            List<ByteString> addresses = entry.getAddrsList();
            for (ByteString address : addresses) {
                Multiaddr multiaddr = preFilter(address);
                if (multiaddr != null) {
                    if (multiaddr.isSupported()) {
                        if (!multiaddr.isAnyLocalAddress()) {
                            if (!multiaddr.isCircuitAddress()) {
                                multiAddresses.add(multiaddr);
                            }
                        }
                    }
                }
            }

            if (!multiAddresses.isEmpty()) {
                peers.add(new Peer(peerId, multiAddresses));
            } else {
                LogUtils.info(TAG, "Ignore evalClosestPeers : " + multiAddresses);
            }
        }
        return peers;
    }


    private void getClosestPeers(@NonNull Closeable closeable, @NonNull byte[] key,
                                 @NonNull Consumer<Peer> channel) {
        if (key.length == 0) {
            throw new RuntimeException("can't lookup empty key");
        }

        runLookupWithFollowup(closeable, key, (ctx1, p) -> {

            Dht.Message pms = findPeerSingle(ctx1, p, key);

            Set<Peer> peers = evalClosestPeers(pms);

            for (Peer peer : peers) {
                channel.accept(peer);
            }

            return peers;
        }, closeable::isClosed, true);


    }

    @Override
    public void putValue(@NonNull Closeable ctx, @NonNull byte[] key, @NonNull byte[] value) {

        bootstrap();

        // don't allow local users to put bad values.
        try {
            Ipns.Entry entry = validator.validate(key, value);
            Objects.requireNonNull(entry);
        } catch (Throwable throwable) {
            throw new RuntimeException(throwable);
        }

        long start = System.currentTimeMillis();

        @SuppressLint("SimpleDateFormat") String format = new SimpleDateFormat(
                IPFS.TimeFormatIpfs).format(new Date());
        RecordOuterClass.Record rec = RecordOuterClass.Record.newBuilder().setKey(ByteString.copyFrom(key))
                .setValue(ByteString.copyFrom(value))
                .setTimeReceived(format).build();

        ConcurrentSkipListSet<PeerId> handled = new ConcurrentSkipListSet<>();

        try {
            getClosestPeers(ctx, key, peer -> {
                if (!handled.contains(peer.getPeerId())) {
                    handled.add(peer.getPeerId());
                    putValueToPeer(ctx, peer, rec);
                }
            });
        } finally {
            LogUtils.verbose(TAG, "Finish putValue at " + (System.currentTimeMillis() - start));
        }

    }

    private void putValueToPeer(@NonNull Closeable ctx, @NonNull Peer peer,
                                @NonNull RecordOuterClass.Record rec) {

        try {
            Dht.Message pms = Dht.Message.newBuilder()
                    .setType(Dht.Message.MessageType.PUT_VALUE)
                    .setKey(rec.getKey())
                    .setRecord(rec)
                    .setClusterLevelRaw(0).build();

            Dht.Message rimes = sendRequest(ctx, peer, pms);

            if (!Arrays.equals(rimes.getRecord().getValue().toByteArray(),
                    pms.getRecord().getValue().toByteArray())) {
                throw new RuntimeException("value not put correctly put-message  " +
                        pms + " get-message " + rimes);
            }
            LogUtils.verbose(TAG, "PutValue Success to " + peer.getPeerId().toBase58());
        } catch (ClosedException | ConnectException ignore) {
        } catch (Throwable throwable) {
            LogUtils.error(TAG, throwable);
        }

    }

    @Override
    public void findProviders(@NonNull Closeable closeable, @NonNull Consumer<Peer> providers,
                              @NonNull Cid cid, boolean acceptLocalAddress) {
        if (!cid.isDefined()) {
            throw new RuntimeException("Cid invalid");
        }

        bootstrap();

        byte[] key = cid.getHash();

        long start = System.currentTimeMillis();

        try {

            runLookupWithFollowup(closeable, key, (ctx, p) -> {

                Dht.Message pms = findProvidersSingle(ctx, p, key);
                Set<Peer> result = evalClosestPeers(pms);

                List<Dht.Message.Peer> list = pms.getProviderPeersList();
                for (Dht.Message.Peer entry : list) {

                    PeerId peerId = new PeerId(entry.getId().toByteArray());

                    Set<Multiaddr> multiAddresses = new HashSet<>();
                    List<ByteString> addresses = entry.getAddrsList();
                    for (ByteString address : addresses) {
                        Multiaddr multiaddr = preFilter(address);
                        if (multiaddr != null) {
                            if (multiaddr.isSupported()) {
                                if (acceptLocalAddress) {
                                    multiAddresses.add(multiaddr);
                                } else {
                                    if (!multiaddr.isLocalAddress()) {
                                        multiAddresses.add(multiaddr);
                                    }
                                }
                            }
                        }
                    }

                    LogUtils.debug(TAG, "findProviders " + peerId.toBase58() + " "
                            + multiAddresses + " for " +
                            cid.String() + " " + cid.getVersion());


                    providers.accept(new Peer(peerId, multiAddresses));

                }

                return result;

            }, closeable::isClosed, false);
        } finally {
            LogUtils.debug(TAG, "Finish findProviders at " +
                    (System.currentTimeMillis() - start));
        }
    }


    public void removeFromRouting(Peer p) {
        boolean result = routingTable.removePeer(p);
        if (result) {
            LogUtils.debug(TAG, "Remove from routing " + p.getPeerId().toBase58());
        }
    }


    private Dht.Message makeProvRecord(@NonNull byte[] key) {

        List<Multiaddr> addresses = host.listenAddresses(false);

        if (addresses.isEmpty()) {
            throw new RuntimeException("no known addresses for self, cannot put provider");
        }

        Dht.Message.Builder builder = Dht.Message.newBuilder()
                .setType(Dht.Message.MessageType.ADD_PROVIDER)
                .setKey(ByteString.copyFrom(key))
                .setClusterLevelRaw(0);

        Dht.Message.Peer.Builder peerBuilder = Dht.Message.Peer.newBuilder()
                .setId(ByteString.copyFrom(self.getBytes()));
        for (Multiaddr ma : addresses) {
            peerBuilder.addAddrs(ByteString.copyFrom(ma.getBytes()));
        }
        builder.addProviderPeers(peerBuilder.build());

        return builder.build();
    }

    @Override
    public void provide(@NonNull Closeable closeable, @NonNull Cid cid) {

        if (!cid.isDefined()) {
            throw new RuntimeException("invalid cid: undefined");
        }

        bootstrap();

        byte[] key = cid.getHash();

        final Dht.Message mes = makeProvRecord(key);

        ConcurrentSkipListSet<PeerId> handled = new ConcurrentSkipListSet<>();
        getClosestPeers(closeable, key, peer -> {
            if (!handled.contains(peer.getPeerId())) {
                handled.add(peer.getPeerId());
                sendMessage(closeable, peer, mes);
            }
        });

    }

    private void sendMessage(@NonNull Closeable closeable, @NonNull Peer peer,
                             @NonNull Dht.Message message) {

        synchronized (peer.getPeerId().toBase58().intern()) {
            QuicConnection conn = null;
            try {
                if (closeable.isClosed()) {
                    return;
                }
                conn = connectPeer(peer);
                if (closeable.isClosed()) {
                    return;
                }
                sendMessage(conn, message);
            } catch (ConnectException ignore) {
                // ignore
            } catch (Throwable throwable) {
                LogUtils.error(TAG, throwable);
            } finally {
                if (conn != null) {
                    if (host.isNotProtected(peer.getPeerId())) {
                        conn.close();
                    }
                }
            }
        }
    }

    private void sendMessage(@NonNull QuicConnection conn, @NonNull Dht.Message message) {
        long time = System.currentTimeMillis();
        boolean success = false;
        try {

            QuicStream quicStream = conn.createStream(true,
                    IPFS.CREATE_STREAM_TIMEOUT, TimeUnit.SECONDS);

            OutputStream outputStream = quicStream.getOutputStream();
            outputStream.write(DataHandler.writeToken(IPFS.STREAM_PROTOCOL, IPFS.DHT_PROTOCOL));

            CompletableFuture<Boolean> done = new CompletableFuture<>();

            ReaderHandler.reading(quicStream,
                    (token) -> {
                        if (!Arrays.asList(IPFS.STREAM_PROTOCOL, IPFS.DHT_PROTOCOL).contains(token)) {
                            done.completeExceptionally(
                                    new RuntimeException("Token " + token + " not supported"));
                            return;
                        }
                        try {
                            if (Objects.equals(token, IPFS.DHT_PROTOCOL)) {
                                outputStream.write(DataHandler.encode(message));
                                outputStream.close();
                                done.complete(true);
                            }
                        } catch (Throwable throwable) {
                            done.completeExceptionally(throwable);
                        }
                    },
                    (data) -> done.complete(true),
                    (fin) -> done.complete(true),
                    done::completeExceptionally);

            success = done.get(IPFS.DHT_SEND_READ_TIMEOUT, TimeUnit.SECONDS);
        } catch (Exception exception) {
            LogUtils.debug(TAG, "Send " + exception.getClass().getSimpleName() +
                    " : " + exception.getMessage());
        } finally {
            LogUtils.debug(TAG, "Send " + success + " took " +
                    (System.currentTimeMillis() - time));
        }
    }

    private QuicConnection connectPeer(@NonNull Peer peer)
            throws ConnectException {

        return host.connect(peer, IPFS.CONNECT_TIMEOUT, IPFS.CONNECT_TIMEOUT,
                0, 20480,
                false);

    }

    private Dht.Message sendRequest(@NonNull Closeable closeable, @NonNull Peer peer,
                                    @NonNull Dht.Message message)
            throws ClosedException, ConnectException {

        synchronized (peer.getPeerId().toBase58().intern()) {
            long time = System.currentTimeMillis();
            boolean success = false;

            if (closeable.isClosed()) {
                throw new ClosedException();
            }

            QuicConnection conn = connectPeer(peer);

            if (closeable.isClosed()) {
                throw new ClosedException();
            }

            try {

                time = System.currentTimeMillis();

                QuicStream quicStream = conn.createStream(true,
                        IPFS.CREATE_STREAM_TIMEOUT, TimeUnit.SECONDS);

                OutputStream outputStream = quicStream.getOutputStream();

                outputStream.write(DataHandler.writeToken(IPFS.STREAM_PROTOCOL, IPFS.DHT_PROTOCOL));

                CompletableFuture<Dht.Message> store = new CompletableFuture<>();

                ReaderHandler.reading(quicStream, (token) -> {
                    if (!Arrays.asList(IPFS.STREAM_PROTOCOL, IPFS.DHT_PROTOCOL).contains(token)) {
                        store.completeExceptionally(
                                new RuntimeException("Token " + token + " not supported"));
                        return;
                    }
                    try {
                        if (Objects.equals(token, IPFS.DHT_PROTOCOL)) {
                            outputStream.write(DataHandler.encode(message));
                            outputStream.close();
                        }
                    } catch (Throwable throwable) {
                        store.completeExceptionally(throwable);
                    }
                }, (data) -> {
                    try {
                        store.complete(Dht.Message.parseFrom(data));
                    } catch (Throwable throwable) {
                        store.completeExceptionally(throwable);
                    }
                }, store::completeExceptionally);

                Dht.Message msg = store.get(IPFS.DHT_REQUEST_READ_TIMEOUT, TimeUnit.SECONDS);
                Objects.requireNonNull(msg);
                success = true;
                peer.setLatency(System.currentTimeMillis() - time);

                return msg;
            } catch (Exception exception) {
                LogUtils.debug(TAG, "Request " + exception.getClass().getSimpleName() +
                        " : " + exception.getMessage());
                throw new ConnectException(exception.getClass().getSimpleName());
            } finally {
                if (host.isNotProtected(peer.getPeerId())) {
                    conn.close();
                }
                LogUtils.debug(TAG, "Request " + success + " took " +
                        (System.currentTimeMillis() - time));
            }
        }
    }


    private Dht.Message getValueSingle(@NonNull Closeable ctx, @NonNull Peer p, @NonNull byte[] key)
            throws ClosedException, ConnectException {
        Dht.Message pms = Dht.Message.newBuilder()
                .setType(Dht.Message.MessageType.GET_VALUE)
                .setKey(ByteString.copyFrom(key))
                .setClusterLevelRaw(0).build();
        return sendRequest(ctx, p, pms);
    }

    private Dht.Message findPeerSingle(@NonNull Closeable ctx, @NonNull Peer p, @NonNull byte[] key)
            throws ClosedException, ConnectException {
        Dht.Message pms = Dht.Message.newBuilder()
                .setType(Dht.Message.MessageType.FIND_NODE)
                .setKey(ByteString.copyFrom(key))
                .setClusterLevelRaw(0).build();

        return sendRequest(ctx, p, pms);
    }

    private Dht.Message findProvidersSingle(@NonNull Closeable ctx, @NonNull Peer p, @NonNull byte[] key)
            throws ClosedException, ConnectException {
        Dht.Message pms = Dht.Message.newBuilder()
                .setType(Dht.Message.MessageType.GET_PROVIDERS)
                .setKey(ByteString.copyFrom(key))
                .setClusterLevelRaw(0).build();
        return sendRequest(ctx, p, pms);
    }


    @Nullable
    private Multiaddr preFilter(@NonNull ByteString address) {
        try {
            return new Multiaddr(address.toByteArray());
        } catch (Throwable ignore) {
            LogUtils.error(TAG, address.toStringUtf8());
        }
        return null;
    }

    @Override
    public void findPeer(@NonNull Closeable closeable, @NonNull Consumer<Peer> consumer, @NonNull PeerId id) {

        bootstrap();

        byte[] key = id.getBytes();
        long start = System.currentTimeMillis();
        try {
            runLookupWithFollowup(closeable, key, (ctx, p) -> {

                Dht.Message pms = findPeerSingle(ctx, p, key);

                Set<Peer> peers = evalClosestPeers(pms);
                for (Peer peer : peers) {
                    if (Objects.equals(peer.getPeerId(), id)) {
                        LogUtils.debug(TAG, "findPeer " + peer.getPeerId().toBase58() + " " +
                                peer.getMultiaddrs());
                        consumer.accept(peer);
                    }
                }

                if (ctx.isClosed()) {
                    return Collections.emptySet();
                }
                return peers;

            }, closeable::isClosed, false);
        } finally {
            LogUtils.debug(TAG, "Finish findPeer " + id.toBase58() +
                    " at " + (System.currentTimeMillis() - start));
        }
    }

    private Map<Peer, PeerState> runQuery(@NonNull Closeable ctx, @NonNull byte[] target,
                                          @NonNull QueryFunc queryFn, @NonNull StopFunc stopFn) {
        // pick the K closest peers to the key in our Routing table.
        ID targetKadID = ID.convertKey(target);
        List<Peer> seedPeers = routingTable.NearestPeers(targetKadID, bucketSize);
        if (seedPeers.size() == 0) {
            return Collections.emptyMap();
        }

        Query q = new Query(this, target, seedPeers, queryFn, stopFn);

        try {
            q.run(ctx);
        } catch (InterruptedException | ClosedException interruptedException) {
            return Collections.emptyMap();
        }

        return q.constructLookupResult(targetKadID);
    }

    // runLookupWithFollowup executes the lookup on the target using the given query function and stopping when either the
    // context is cancelled or the stop function returns true. Note: if the stop function is not sticky, i.e. it does not
    // return true every time after the first time it returns true, it is not guaranteed to cause a stop to occur just
    // because it momentarily returns true.
    //
    // After the lookup is complete the query function is run (unless stopped) against all of the top K peers from the
    // lookup that have not already been successfully queried.
    private void runLookupWithFollowup(@NonNull Closeable closeable, @NonNull byte[] target,
                                       @NonNull QueryFunc queryFn, @NonNull StopFunc stopFn,
                                       boolean runFollowUp) {


        Map<Peer, PeerState> lookupRes = runQuery(closeable, target, queryFn, stopFn);


        // query all of the top K peers we've either Heard about or have outstanding queries we're Waiting on.
        // This ensures that all of the top K results have been queried which adds to resiliency against churn for query
        // functions that carry state (e.g. FindProviders and GetValue) as well as establish connections that are needed
        // by stateless query functions (e.g. GetClosestPeers and therefore Provide and PutValue)

        List<Peer> queryPeers = new ArrayList<>();
        for (Map.Entry<Peer, PeerState> entry : lookupRes.entrySet()) {
            PeerState state = entry.getValue();
            if (state == PeerState.PeerHeard || state == PeerState.PeerWaiting) {
                queryPeers.add(entry.getKey());
            }
        }

        if (queryPeers.size() == 0) {
            return;
        }

        if (stopFn.stop()) {
            return;
        }

        if (runFollowUp) {
            List<Future<Void>> futures = new ArrayList<>();
            ExecutorService executor = Executors.newFixedThreadPool(4);
            for (Peer peerId : queryPeers) {
                Future<Void> future = executor.submit(() -> invokeQuery(closeable, queryFn, peerId));
                futures.add(future);
            }
            for (Future<Void> future : futures) {
                try {
                    future.get();
                } catch (Throwable ignore) {
                    // ignore
                }
            }
        }
    }

    private Void invokeQuery(@NonNull Closeable closeable,
                             @NonNull QueryFunc queryFn,
                             @NonNull Peer peerId) {
        if (closeable.isClosed()) {
            return null;
        }

        try {
            queryFn.query(closeable, peerId);
        } catch (Throwable ignore) {
            // ignore
        }
        return null;
    }


    private Pair<Ipns.Entry, Set<Peer>> getValueOrPeers(
            @NonNull Closeable ctx, @NonNull Peer p, @NonNull byte[] key)
            throws ClosedException, ConnectException {


        Dht.Message pms = getValueSingle(ctx, p, key);

        Set<Peer> peers = evalClosestPeers(pms);

        if (pms.hasRecord()) {

            RecordOuterClass.Record rec = pms.getRecord();
            try {
                byte[] record = rec.getValue().toByteArray();
                if (record != null && record.length > 0) {
                    Ipns.Entry entry = validator.validate(rec.getKey().toByteArray(), record);
                    return Pair.create(entry, peers);
                }
            } catch (RecordIssue issue) {
                LogUtils.debug(TAG, issue.getMessage());
            } catch (Throwable throwable) {
                LogUtils.error(TAG, throwable);
            }
        }

        if (peers.size() > 0) {
            return Pair.create(null, peers);
        }
        return Pair.create(null, Collections.emptySet());
    }

    private void getValues(@NonNull Closeable ctx, @NonNull RecordValueFunc recordFunc,
                           @NonNull byte[] key, @NonNull StopFunc stopQuery) {


        runLookupWithFollowup(ctx, key, (ctx1, p) -> {

            Pair<Ipns.Entry, Set<Peer>> result = getValueOrPeers(ctx1, p, key);
            Ipns.Entry entry = result.first;
            Set<Peer> peers = result.second;

            if (entry != null) {
                recordFunc.record(entry);
            }

            return peers;
        }, stopQuery, true);

    }


    private void processValues(@NonNull Closeable ctx, @Nullable Ipns.Entry best,
                               @NonNull Ipns.Entry v, @NonNull RecordReportFunc reporter) {

        if (best != null) {
            if (Objects.equals(best, v)) {
                reporter.report(ctx, v, false);
            } else {
                int value = validator.compare(best, v);

                if (value == -1) {
                    reporter.report(ctx, v, false);
                }
            }
        } else {
            reporter.report(ctx, v, true);
        }
    }


    @Override
    public void searchValue(@NonNull Closeable closeable, @NonNull Consumer<Ipns.Entry> resolveInfo,
                            @NonNull byte[] key) {

        bootstrap();

        AtomicReference<Ipns.Entry> best = new AtomicReference<>();
        long start = System.currentTimeMillis();
        try {
            getValues(closeable, entry -> processValues(closeable, best.get(),
                    entry, (ctx1, v, better) -> {
                        if (better) {
                            resolveInfo.accept(v);
                            best.set(v);
                        }
                    }), key, closeable::isClosed);
        } finally {
            LogUtils.info(TAG, "Finish searchValue at " + (System.currentTimeMillis() - start));
        }
    }

    public interface StopFunc {
        boolean stop();
    }

    public interface QueryFunc {
        @NonNull
        Set<Peer> query(@NonNull Closeable ctx, @NonNull Peer peerId)
                throws ClosedException, ConnectException;
    }


    public interface RecordValueFunc {
        void record(@NonNull Ipns.Entry entry);
    }

    public interface RecordReportFunc {
        void report(@NonNull Closeable ctx, @NonNull Ipns.Entry entry, boolean better);
    }


}
