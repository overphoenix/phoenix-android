package threads.lite.holepunch;

import android.util.Pair;

import androidx.annotation.NonNull;

import com.google.protobuf.ByteString;

import net.luminis.quic.QuicConnection;
import net.luminis.quic.QuicStream;
import net.luminis.quic.server.Server;

import java.io.OutputStream;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import holepunch.pb.Holepunch;
import threads.lite.IPFS;
import threads.lite.LogUtils;
import threads.lite.cid.Multiaddr;
import threads.lite.cid.PeerId;
import threads.lite.cid.Protocol;
import threads.lite.host.Dialer;
import threads.lite.host.LiteHost;
import threads.lite.host.PeerInfo;
import threads.lite.ident.IdentityService;
import threads.lite.relay.RelayConnection;
import threads.lite.utils.DataHandler;
import threads.lite.utils.ReaderHandler;

public class HolePunchService {
    public static final String TAG = HolePunchService.class.getSimpleName();
    private static final int STREAM_TIMEOUT = 30; // original is 60 sec
    private static final int DIAL_TIMEOUT = 10;

    @NonNull
    public static QuicConnection directConnect(@NonNull LiteHost liteHost,
                                               @NonNull PeerId peerId, @NonNull Multiaddr multiaddr,
                                               int maxIdleTimeoutInSeconds, int initialMaxStreams, int initialMaxStreamData,
                                               boolean keepConnection) throws Exception {

        synchronized (peerId.toBase58().intern()) {
            if (Objects.equals(peerId, liteHost.self())) {
                throw new RuntimeException("No connection to yourself");
            }

            if (liteHost.hasConnection(peerId)) {
                throw new RuntimeException("Connection exists already to " + peerId.toBase58());
            }

            boolean hasCircuit = multiaddr.isCircuitAddress();
            if (!hasCircuit) {
                throw new RuntimeException("usage error");
            }

            boolean hasIpfsType = multiaddr.has(Protocol.Type.IPFS);
            if (!hasIpfsType) {
                throw new RuntimeException("wrong format");
            }

            String relay = multiaddr.getStringComponent(Protocol.Type.IPFS);
            Objects.requireNonNull(relay);
            PeerId relayId = PeerId.fromBase58(relay);

            String host = multiaddr.getHost();
            int port = multiaddr.getPort();

            InetSocketAddress relayAddress = new InetSocketAddress(host, port);


            Server server = liteHost.getServer();
            if (server == null) {
                throw new ConnectException("Server is not defined");
            }

            QuicConnection conn = null;
            boolean close = false;
            try {
                conn = liteHost.getConnection(relayId);
                if (conn == null) {
                    conn = Dialer.dial(liteHost, relayId, Multiaddr.transform(relayAddress),
                            DIAL_TIMEOUT, IPFS.GRACE_PERIOD, IPFS.MAX_STREAMS,
                            IPFS.MESSAGE_SIZE_MAX, false);
                    close = true;
                }

                // check if RELAY protocols HOP is supported
                PeerInfo peerInfo = IdentityService.getPeerInfo(conn);

                if (!peerInfo.hasProtocol(IPFS.RELAY_PROTOCOL_HOP)) {
                    throw new ConnectException("does not support relay hop");
                }

                Multiaddr observed = peerInfo.getObserved();
                if (observed == null) {
                    throw new RuntimeException("does not return observed address");
                }

                RelayConnection connection = RelayConnection.createRelayConnection(conn, peerId);

                // Upon observing the new connection, the inbound peer (here B) checks the
                // addresses advertised by A via identify. If that set includes public addresses,
                // then A may be reachable by a direct connection, in which case B attempts a
                // unilateral connection upgrade by initiating a direct connection to A.

                // Upon observing the new connection, the inbound peer (here B) checks the
                // addresses advertised by A via identify. If that set includes public addresses,
                // then A may be reachable by a direct connection, in which case B attempts a
                // unilateral connection upgrade by initiating a direct connection to A.


                // B opens a stream to A using the /libp2p/dcutr protocol.
                // B sends to A a Connect message containing its observed (and possibly predicted)
                // addresses from identify and starts a timer to measure RTT of the relay connection.
                //
                // Upon receiving the Connect, A responds back with a Connect message
                // containing its observed (and possibly predicted) addresses.
                //
                // Upon receiving the Connect, B sends a Sync message and starts a
                // timer for half the RTT measured from the time between sending the
                // initial Connect and receiving the response. The purpose of the
                // Sync message and B's timer is to allow the two peers to synchronize
                // so that they perform a simultaneous open that allows hole punching to succeed.

                Pair<List<Multiaddr>, Long> result = initiateHolePunch(liteHost,
                        connection, observed);
                LogUtils.debug(TAG, result.first.toString());

                long rtt = result.second;
                // wait for sync to reach the other peer and then punch a hole for it in our NAT
                // by attempting a connect to it.
                Thread.sleep(rtt / 2);
                List<Multiaddr> list = liteHost.supported(result.first);

                return specialConnect(liteHost, peerId, list,
                        maxIdleTimeoutInSeconds, initialMaxStreams,
                        initialMaxStreamData, keepConnection);
            } finally {
                if (close) {
                    if (liteHost.isNotProtected(relayId)) {
                        conn.close();
                    }
                }
            }
        }
    }

    @NonNull
    private static QuicConnection specialConnect(
            @NonNull LiteHost liteHost, @NonNull PeerId peerId, @NonNull List<Multiaddr> multiaddrs,
            int maxIdleTimeoutInSeconds, int initialMaxStreams, int initialMaxStreamData,
            boolean keepConnection) throws ConnectException {

        List<Multiaddr> supported = liteHost.supported(multiaddrs);
        CompletableFuture<QuicConnection> done = new CompletableFuture<>();
        if (!supported.isEmpty()) {
            ExecutorService executor = Executors.newFixedThreadPool(supported.size());
            for (Multiaddr address : supported) {
                executor.execute(() -> {
                    try {
                        done.complete(Dialer.dial(liteHost, peerId, address, DIAL_TIMEOUT,
                                maxIdleTimeoutInSeconds, initialMaxStreams,
                                initialMaxStreamData, keepConnection));
                    } catch (Throwable ignore) {
                        // ignore
                    }
                });
            }
            try {
                return done.get(DIAL_TIMEOUT, TimeUnit.SECONDS);
            } catch (Throwable throwable) {
                throw new ConnectException(throwable.getMessage());
            } finally {
                try {
                    executor.shutdownNow();
                } catch (Throwable throwable) {
                    LogUtils.error(TAG, throwable);
                }
            }
        }
        throw new ConnectException("no addresses left");
    }

    @NonNull
    private static Pair<List<Multiaddr>, Long> initiateHolePunch(@NonNull LiteHost liteHost,
                                                                 @NonNull RelayConnection conn,
                                                                 @NonNull Multiaddr observed)
            throws Exception {

        Holepunch.HolePunch.Builder builder = Holepunch.HolePunch.newBuilder()
                .setType(Holepunch.HolePunch.Type.CONNECT);

        LinkedHashSet<Multiaddr> list = new LinkedHashSet<>();
        list.add(observed);

        try {
            list.add(liteHost.defaultListenAddress(false));
        } catch (Throwable throwable) {
            LogUtils.error(TAG, throwable);
        }

        LogUtils.debug(TAG, "Server (this) " + list);

        for (Multiaddr addr : list) {
            boolean relayConnection = addr.isCircuitAddress();
            if (!relayConnection) {
                builder.addObsAddrs(ByteString.copyFrom(addr.getBytes()));
            }
        }
        Holepunch.HolePunch message = builder.build();

        long time = System.currentTimeMillis();

        QuicStream quicStream = conn.createStream(true);

        OutputStream outputStream = quicStream.getOutputStream();

        outputStream.write(DataHandler.writeToken(
                IPFS.STREAM_PROTOCOL, IPFS.RELAY_PROTOCOL_HOLE_PUNCH));

        CompletableFuture<Holepunch.HolePunch> store = new CompletableFuture<>();

        ReaderHandler.reading(quicStream,
                (token) -> {
                    if (!Arrays.asList(IPFS.STREAM_PROTOCOL, IPFS.RELAY_PROTOCOL_HOLE_PUNCH).contains(token)) {
                        store.completeExceptionally(
                                new RuntimeException("Token " + token + " not supported"));
                        return;
                    }
                    try {
                        if (Objects.equals(token, IPFS.RELAY_PROTOCOL_HOLE_PUNCH)) {
                            outputStream.write(DataHandler.encode(message));
                        }
                    } catch (Throwable throwable) {
                        store.completeExceptionally(throwable);
                    }
                },
                (data) -> {
                    try {
                        store.complete(Holepunch.HolePunch.parseFrom(data));
                    } catch (Throwable throwable) {
                        store.completeExceptionally(throwable);
                    }

                }, store::completeExceptionally);

        Holepunch.HolePunch msg = store.get(STREAM_TIMEOUT, TimeUnit.SECONDS);
        long rtt = System.currentTimeMillis() - time;
        LogUtils.info(TAG, "Request took " + rtt);
        Objects.requireNonNull(msg);


        if (msg.getType() != Holepunch.HolePunch.Type.CONNECT) {
            outputStream.close();
            return Pair.create(Collections.emptyList(), 0L);
        }

        List<Multiaddr> addresses = new ArrayList<>();
        List<ByteString> entries = msg.getObsAddrsList();
        if (entries.size() == 0) {
            return Pair.create(Collections.emptyList(), 0L);
        }

        for (ByteString entry : entries) {
            Multiaddr multiaddr = new Multiaddr(entry.toByteArray());
            boolean relayConnection = multiaddr.isCircuitAddress();
            if (!relayConnection) {
                addresses.add(multiaddr);
            }
        }

        msg = Holepunch.HolePunch.newBuilder()
                .setType(Holepunch.HolePunch.Type.SYNC).build();

        outputStream.write(DataHandler.encode(msg));
        outputStream.close();

        LogUtils.debug(TAG, "Client (other) " + addresses);
        return Pair.create(addresses, rtt);


    }

}
