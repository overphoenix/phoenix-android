package threads.lite.host;


import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.common.primitives.Bytes;
import com.google.protobuf.ByteString;

import net.luminis.quic.QuicConnection;
import net.luminis.quic.Version;
import net.luminis.quic.server.ApplicationProtocolConnection;
import net.luminis.quic.server.ApplicationProtocolConnectionFactory;
import net.luminis.quic.server.Server;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.ConnectException;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.UnknownHostException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import bitswap.pb.MessageOuterClass;
import circuit.pb.Circuit;
import identify.pb.IdentifyOuterClass;
import threads.lite.IPFS;
import threads.lite.LogUtils;
import threads.lite.bitswap.BitSwap;
import threads.lite.bitswap.BitSwapMessage;
import threads.lite.cid.Cid;
import threads.lite.cid.Multiaddr;
import threads.lite.cid.Peer;
import threads.lite.cid.PeerId;
import threads.lite.core.Closeable;
import threads.lite.crypto.PrivKey;
import threads.lite.crypto.PubKey;
import threads.lite.dht.KadDht;
import threads.lite.dht.Routing;
import threads.lite.format.BlockStore;
import threads.lite.ident.IdentityService;
import threads.lite.ipns.Ipns;
import threads.lite.push.Push;
import threads.lite.relay.RelayConnection;
import threads.lite.relay.RelayService;
import threads.lite.relay.Reservation;


public class LiteHost {


    @NonNull
    private static final String TAG = LiteHost.class.getSimpleName();
    @NonNull
    private static final Duration DefaultRecordEOL = Duration.ofHours(24);
    @NonNull
    public final AtomicReference<Protocol> protocol = new AtomicReference<>(Protocol.IPv4_IPv6);
    /* NOT YET REQUIRED
    @NonNull

    @NonNull
    private static final TrustManager tm = new X509TrustManager() {
        @Override
        public void checkClientTrusted(X509Certificate[] chain, String s) {
            try {
                if (IPFS.EVALUATE_PEER) {
                    for (X509Certificate cert : chain) {
                        PubKey pubKey = LiteHostCertificate.extractPublicKey(cert);
                        Objects.requireNonNull(pubKey);
                        PeerId peerId = PeerId.fromPubKey(pubKey);
                        Objects.requireNonNull(peerId);
                    }
                }
            } catch (Throwable throwable) {
                throw new RuntimeException(throwable);
            }
        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String s) {

            try {
                if (IPFS.EVALUATE_PEER) {
                    for (X509Certificate cert : chain) {
                        PubKey pubKey = LiteHostCertificate.extractPublicKey(cert);
                        Objects.requireNonNull(pubKey);
                        PeerId peerId = PeerId.fromPubKey(pubKey);
                        Objects.requireNonNull(peerId);
                        remotes.put(peerId, pubKey);
                    }
                }
            } catch (Throwable throwable) {
                throw new RuntimeException(throwable);
            }
        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[0];
        }
    };*/
    @NonNull
    private final ConcurrentHashMap<PeerId, Reservation> reservations = new ConcurrentHashMap<>();
    @NonNull
    private final ConcurrentHashMap<PeerId, QuicConnection> connections = new ConcurrentHashMap<>();
    @NonNull
    private final ConcurrentSkipListSet<InetAddress> addresses = new ConcurrentSkipListSet<>(
            Comparator.comparing(InetAddress::getHostAddress)
    );
    @NonNull
    private final ConcurrentHashMap<PeerId, Set<Multiaddr>> addressBook = new ConcurrentHashMap<>();
    @NonNull
    private final Routing routing;
    @NonNull
    private final PrivKey privKey;
    @NonNull
    private final BitSwap bitSwap;
    @NonNull
    private final PeerId peerId;
    private final int port;
    @NonNull
    private final LiteHostCertificate selfSignedCertificate;
    @NonNull
    private final Set<PeerId> swarm = ConcurrentHashMap.newKeySet();
    @Nullable
    private Push push;
    @Nullable
    private Server server;

    public LiteHost(@NonNull LiteHostCertificate selfSignedCertificate,
                    @NonNull PrivKey privKey,
                    @NonNull BlockStore blockstore,
                    int port, int alpha) {
        this.selfSignedCertificate = selfSignedCertificate;
        this.privKey = privKey;

        this.peerId = PeerId.fromPubKey(privKey.publicKey());

        this.routing = new KadDht(this,
                new Ipns(), alpha, IPFS.DHT_BUCKET_SIZE);

        this.bitSwap = new BitSwap(blockstore, this);

        if (port >= 0 && !isLocalPortFree(port)) {
            this.port = nextFreePort();
        } else {
            this.port = port;
        }
        if (this.port >= 0) {
            try {
                List<Version> supportedVersions = new ArrayList<>();
                supportedVersions.add(Version.IETF_draft_29);
                supportedVersions.add(Version.QUIC_version_1);

                server = new Server(port, IPFS.APRN,
                        new FileInputStream(selfSignedCertificate.certificate()),
                        new FileInputStream(selfSignedCertificate.privateKey()),
                        supportedVersions, false,
                        new ApplicationProtocolConnectionFactory() {
                            @Override
                            public ApplicationProtocolConnection createConnection(
                                    String protocol, QuicConnection quicConnection) {
                                return new ServerHandler(LiteHost.this, quicConnection);

                            }
                        });
                server.start();
            } catch (Throwable throwable) {
                LogUtils.error(TAG, throwable);
            }
        }

        updateListenAddresses();
    }

    public static int nextFreePort() {
        int port = ThreadLocalRandom.current().nextInt(4001, 65535);
        while (true) {
            if (isLocalPortFree(port)) {
                return port;
            } else {
                port = ThreadLocalRandom.current().nextInt(4001, 65535);
            }
        }
    }

    private static boolean isLocalPortFree(int port) {
        try {
            new ServerSocket(port).close();
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    @Nullable
    public Server getServer() {
        return server;
    }

    @NonNull
    public LiteHostCertificate getSelfSignedCertificate() {
        return selfSignedCertificate;
    }

    @NonNull
    public ConcurrentHashMap<PeerId, Reservation> reservations() {
        return reservations;
    }

    @NonNull
    public Routing getRouting() {
        return routing;
    }

    @NonNull
    public BitSwap getBitSwap() {
        return bitSwap;
    }

    public PeerId self() {
        return peerId;
    }

    public void message(@NonNull QuicConnection conn, @NonNull MessageOuterClass.Message msg) {
        BitSwapMessage message = BitSwapMessage.newMessageFromProto(msg);
        bitSwap.receiveMessage(conn, message);
    }

    public void findProviders(@NonNull Closeable closeable, @NonNull Consumer<Peer> providers,
                              @NonNull Cid cid, boolean acceptLocalAddress) {
        routing.findProviders(closeable, providers, cid, acceptLocalAddress);
    }

    @NonNull
    private Multiaddr findFirstValid(@NonNull List<Multiaddr> all,
                                     @NonNull threads.lite.cid.Protocol.Type type) throws Exception {
        if (all.size() == 0) {
            throw new Exception("No default listen addresses");
        }
        Multiaddr multiaddr = all.get(0);
        for (Multiaddr ma : all) {
            if (ma.has(type)) {
                multiaddr = ma;
                break;
            }
        }
        return multiaddr;
    }

    @NonNull
    public Multiaddr defaultListenAddress(boolean enhancePeerId) throws Exception {
        int port = getPort();
        if (port <= 0) {
            throw new Exception("Port is not defined");
        }
        List<Multiaddr> multiaddrs = defaultListenAddresses(enhancePeerId);
        return findFirstValid(multiaddrs, getDefaultProtocol());
    }

    public boolean hasAddresses(@NonNull PeerId peerId) {
        Set<Multiaddr> res = addressBook.get(peerId);
        if (res == null) {
            return false;
        }
        return !res.isEmpty();
    }

    @NonNull
    public Set<Multiaddr> getAddresses(@NonNull PeerId peerId) {
        try {
            Collection<Multiaddr> multiaddrs = addressBook.get(peerId);
            if (multiaddrs != null) {
                return new HashSet<>(multiaddrs);
            }
        } catch (Throwable throwable) {
            LogUtils.error(TAG, throwable);
        }
        return Collections.emptySet();
    }

    @NonNull
    private List<Multiaddr> prepareAddresses(@NonNull Set<Multiaddr> set) {
        List<Multiaddr> all = new ArrayList<>();
        for (Multiaddr ma : set) {
            try {
                if (ma.has(threads.lite.cid.Protocol.Type.DNS)) {
                    all.add(DnsResolver.resolveDns(ma));
                } else if (ma.has(threads.lite.cid.Protocol.Type.DNS6)) {
                    all.add(DnsResolver.resolveDns6(ma));
                } else if (ma.has(threads.lite.cid.Protocol.Type.DNS4)) {
                    all.add(DnsResolver.resolveDns4(ma));
                } else if (ma.has(threads.lite.cid.Protocol.Type.DNSADDR)) {
                    all.addAll(DnsResolver.resolveDnsAddress(ma));
                } else {
                    all.add(ma);
                }
            } catch (Throwable throwable) {
                LogUtils.error(TAG, ma.toString() + " prepareAddresses " + throwable);
            }
        }
        return supported(all);
    }

    @NonNull
    public List<Multiaddr> supported(@NonNull List<Multiaddr> all) {
        List<Multiaddr> result = new ArrayList<>();
        for (Multiaddr ma : all) {
            if (ma.isSupported()) {
                result.add(ma);
            }
        }
        return result;
    }

    @NonNull
    public List<Multiaddr> sortOutNonLocals(@NonNull List<Multiaddr> all,
                                            @NonNull Protocol protocol) {
        List<Multiaddr> list = new ArrayList<>();
        for (Multiaddr address : all) {

            // accept all local addresses
            if (address.isAnyLocalAddress()) {
                list.add(address);
                continue;
            }

            // sort out IPv6 Addresses in case of non ipv6 network
            if (protocol == Protocol.IPv4) {
                if (address.has(threads.lite.cid.Protocol.Type.IP6)) {
                    continue;
                }
            }
            if (protocol == Protocol.IPv6) {
                if (address.has(threads.lite.cid.Protocol.Type.IP4)) {
                    continue;
                }
            }
            list.add(address);
        }
        return list;
    }

    public void findPeer(@NonNull Closeable closeable,
                         @NonNull Consumer<Peer> consumer,
                         @NonNull PeerId peerId) {
        routing.findPeer(closeable, consumer, peerId);

    }

    public void publishName(@NonNull Closeable closable, @NonNull PrivKey privKey,
                            @NonNull String name, @NonNull PeerId id, int sequence) {


        Date eol = Date.from(new Date().toInstant().plus(DefaultRecordEOL));

        Duration duration = Duration.ofHours(IPFS.IPNS_DURATION);
        ipns.pb.Ipns.IpnsEntry
                record = Ipns.create(privKey, name.getBytes(), sequence, eol, duration);

        PubKey pk = privKey.publicKey();

        record = Ipns.embedPublicKey(pk, record);

        byte[] bytes = record.toByteArray();

        byte[] ipns = IPFS.IPNS_PATH.getBytes();
        byte[] ipnsKey = Bytes.concat(ipns, id.getBytes());
        routing.putValue(closable, ipnsKey, bytes);
    }

    public void swarmReduce(@NonNull PeerId peerId) {
        swarm.remove(peerId);
    }


    public void addToAddressBook(@NonNull PeerId peerId, @NonNull Collection<Multiaddr> addresses) {
        synchronized (peerId.toBase58().intern()) {
            Set<Multiaddr> info = addressBook.computeIfAbsent(peerId, k -> new HashSet<>());
            for (Multiaddr ma : addresses) {
                if (ma.isSupported()) {
                    info.add(ma);
                }
            }
        }
    }

    public void swarmEnhance(@NonNull PeerId peerId) {
        swarm.add(peerId);
    }

    @NonNull
    public Set<Peer> getPeers() {
        HashSet<Peer> peers = new HashSet<>();
        for (PeerId peerId : swarm) {
            Set<Multiaddr> set = addressBook.get(peerId);
            if (set != null) {
                if (!set.isEmpty()) {
                    peers.add(new Peer(peerId, set));
                }
            }
        }
        return peers;
    }

    @Nullable
    public Reservation getReservation(@NonNull PeerId relayId) {
        return reservations.get(relayId);
    }

    @NonNull
    public List<Multiaddr> listenAddresses(boolean enhancePeerId) {
        try {
            List<Multiaddr> list = new ArrayList<>();
            if (port > 0) {
                list.addAll(defaultListenAddresses(enhancePeerId));
            }
            return list;
        } catch (Throwable throwable) {
            LogUtils.error(TAG, throwable);
        }
        return Collections.emptyList();

    }

    public List<Multiaddr> defaultListenAddresses(boolean enhancePeerId) {
        List<Multiaddr> result = new ArrayList<>();
        if (port > 0) {
            for (InetAddress inetAddress : getAddresses()) {
                Multiaddr multiaddr = Multiaddr.transform(new InetSocketAddress(inetAddress, port));
                if (enhancePeerId) {
                    result.add(new Multiaddr(
                            multiaddr.toString().concat("/p2p/").concat(self().toBase58())));
                } else {
                    result.add(multiaddr);
                }
            }
        }
        return result;
    }

    public ConcurrentSkipListSet<InetAddress> getAddresses() {
        try {
            evaluateDefaultHost();
        } catch (Throwable throwable) {
            LogUtils.error(TAG, throwable);
        }
        return addresses;
    }

    private void evaluateDefaultHost() throws UnknownHostException {
        if (addresses.isEmpty()) {
            addresses.add(InetAddress.getByName("127.0.0.1"));
            addresses.add(InetAddress.getByName("::1"));
        }
    }

    @NonNull
    public threads.lite.cid.Protocol.Type getDefaultProtocol() {
        if (protocol.get() == Protocol.IPv6) {
            return threads.lite.cid.Protocol.Type.IP6;
        }
        return threads.lite.cid.Protocol.Type.IP4;
    }

    @Nullable
    public QuicConnection find(@NonNull PeerId peerId, int timeout, int initialMaxStreams,
                               int initialMaxStreamData, boolean keepConnection,
                               @NonNull Closeable closeable) {
        try {
            return connect(peerId, timeout, IPFS.GRACE_PERIOD, initialMaxStreams,
                    initialMaxStreamData, keepConnection);
        } catch (Throwable ignore) {
            AtomicReference<QuicConnection> found = new AtomicReference<>();
            findPeer(() -> closeable.isClosed() || found.get() != null, peer -> {
                try {
                    found.set(connect(peer, timeout, IPFS.GRACE_PERIOD, initialMaxStreams,
                            initialMaxStreamData, keepConnection));
                } catch (Throwable throwable) {
                    // ignore exception again
                }
            }, peerId);
            return found.get();
        }
    }

    @NonNull
    public QuicConnection connect(@NonNull PeerId peerId, int timeout, int maxIdleTimeoutInSeconds,
                                  int initialMaxStreams, int initialMaxStreamData,
                                  boolean keepConnection) throws ConnectException {

        return connect(peerId, getAddresses(peerId), timeout, maxIdleTimeoutInSeconds,
                initialMaxStreams, initialMaxStreamData, keepConnection);
    }

    @NonNull
    public QuicConnection connect(@NonNull Peer peer, int timeout, int maxIdleTimeoutInSeconds,
                                  int initialMaxStreams, int initialMaxStreamData,
                                  boolean keepConnection) throws ConnectException {

        return connect(peer.getPeerId(), peer.getMultiaddrs(), timeout, maxIdleTimeoutInSeconds,
                initialMaxStreams, initialMaxStreamData, keepConnection);
    }

    @NonNull
    public QuicConnection connect(@NonNull PeerId peerId, @NonNull Set<Multiaddr> set,
                                  int timeout, int maxIdleTimeoutInSeconds,
                                  int initialMaxStreams, int initialMaxStreamData,
                                  boolean keepConnection) throws ConnectException {


        if (keepConnection) {
            QuicConnection conn = getConnection(peerId);
            if (conn != null) {
                return conn;
            }
        }
        List<Multiaddr> multiaddr = prepareAddresses(set);
        List<Multiaddr> list = sortOutNonLocals(multiaddr, protocol.get());
        int addresses = list.size();
        if (addresses == 0) {
            throw new ConnectException("No addresses");
        }

        return Dialer.connect(this, peerId, list, timeout, maxIdleTimeoutInSeconds,
                initialMaxStreams, initialMaxStreamData, keepConnection);
    }

    public boolean hasReservation(@NonNull PeerId relayId) {
        return reservations.containsKey(relayId);
    }

    public void push(@NonNull QuicConnection connection, @NonNull byte[] content) {
        try {
            Objects.requireNonNull(peerId);
            Objects.requireNonNull(connection);
            Objects.requireNonNull(content);

            if (push != null) {
                ExecutorService executor = Executors.newSingleThreadExecutor();
                executor.submit(() -> push.push(connection, new String(content)));
            }
        } catch (Throwable throwable) {
            LogUtils.error(TAG, throwable);
        }

    }

    public void setPush(@Nullable Push push) {
        this.push = push;
    }

    public IdentifyOuterClass.Identify createIdentity(@Nullable InetSocketAddress inetSocketAddress) {

        IdentifyOuterClass.Identify.Builder builder = IdentifyOuterClass.Identify.newBuilder()
                .setAgentVersion(IPFS.AGENT)
                .setPublicKey(ByteString.copyFrom(privKey.publicKey().bytes()))
                .setProtocolVersion(IPFS.PROTOCOL_VERSION);

        List<Multiaddr> addresses = listenAddresses(false);
        for (Multiaddr addr : addresses) {
            builder.addListenAddrs(ByteString.copyFrom(addr.getBytes()));
        }

        List<String> protocols = getProtocols();
        for (String protocol : protocols) {
            builder.addProtocols(protocol);
        }

        if (inetSocketAddress != null) {
            Multiaddr observed = Multiaddr.transform(inetSocketAddress);
            builder.setObservedAddr(ByteString.copyFrom(observed.getBytes()));
        }

        return builder.build();
    }

    private List<String> getProtocols() {
        return Arrays.asList(IPFS.STREAM_PROTOCOL, IPFS.PUSH_PROTOCOL, IPFS.BITSWAP_PROTOCOL,
                IPFS.IDENTITY_PROTOCOL, IPFS.DHT_PROTOCOL, IPFS.RELAY_PROTOCOL_HOLE_PUNCH,
                IPFS.RELAY_PROTOCOL_STOP);
    }

    public void shutdown() {
        try {
            if (server != null) {
                server.shutdown();
            }
        } catch (Throwable throwable) {
            LogUtils.error(TAG, throwable);
        } finally {
            server = null;
        }
    }

    public boolean swarmContains(@NonNull PeerId peerId) {
        return swarm.contains(peerId);
    }

    public void updateNetwork() {
        updateListenAddresses();
    }

    public void updateListenAddresses() {

        try {
            List<InetAddress> locals = new ArrayList<>();
            List<InetAddress> externals = new ArrayList<>();
            List<NetworkInterface> interfaces = Collections.list(
                    NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface networkInterface : interfaces) {

                List<InetAddress> addresses =
                        Collections.list(networkInterface.getInetAddresses());
                for (InetAddress inetAddress : addresses) {

                    if (!(inetAddress.isAnyLocalAddress() ||
                            inetAddress.isLinkLocalAddress() ||
                            inetAddress.isLoopbackAddress())) {

                        if (IPFS.PREFER_IPV6_PROTOCOL) {
                            if (inetAddress.isSiteLocalAddress()) {
                                locals.add(inetAddress);
                            } else {
                                externals.add(inetAddress);
                            }
                        } else {
                            externals.add(inetAddress);
                        }
                    }
                }

            }
            synchronized (TAG.intern()) {
                if (!externals.isEmpty()) {
                    protocol.set(getProtocol(externals));
                    addresses.addAll(externals);
                } else {
                    protocol.set(getProtocol(locals));
                    addresses.addAll(locals);
                }
            }
        } catch (Throwable throwable) {
            LogUtils.error(TAG, throwable);
        }
    }

    @NonNull
    private Protocol getProtocol(@NonNull List<InetAddress> addresses) {
        boolean ipv4 = false;
        boolean ipv6 = false;
        for (InetAddress inet : addresses) {
            if (inet instanceof Inet6Address) {
                ipv6 = true;
            } else {
                ipv4 = true;
            }
        }

        if (ipv4 && ipv6) {
            return Protocol.IPv4_IPv6;
        } else if (ipv4) {
            return Protocol.IPv4;
        } else if (ipv6) {
            return Protocol.IPv6;
        } else {
            return Protocol.IPv4_IPv6;
        }

    }

    public int getPort() {
        return port;
    }

    public boolean isNotProtected(@NonNull PeerId peerId) {
        return !hasReservation(peerId) && !swarmContains(peerId);
    }

    public boolean hasConnection(@NonNull PeerId peerId) {
        QuicConnection conn = connections.get(peerId);
        if (conn != null && conn.isConnected()) {
            return true;
        } else {
            connections.remove(peerId);
            try {
                if (conn != null) {
                    if (LogUtils.isDebug()) {
                        if (!isNotProtected(peerId)) {
                            LogUtils.error(TAG, "Protected Peer " + peerId +
                                    " is closed");
                        }
                    }

                    conn.close();
                }
            } catch (Throwable throwable) {
                LogUtils.error(TAG, throwable);
            }
        }
        return false;
    }

    @NonNull
    public Reservation doReservation(@NonNull PeerId relayId, @NonNull Multiaddr multiaddr)
            throws Exception {

        if (!multiaddr.has(threads.lite.cid.Protocol.Type.QUIC)) {
            throw new Exception("only quic addresses are supported");
        }

        QuicConnection conn = Dialer.dial(this, relayId, multiaddr,
                IPFS.CONNECT_TIMEOUT, (int) TimeUnit.HOURS.toSeconds(1),
                IPFS.MAX_STREAMS, IPFS.MESSAGE_SIZE_MAX, true);
        Objects.requireNonNull(conn);

        // check if RELAY protocols HOP is supported
        PeerInfo peerInfo = IdentityService.getPeerInfo(conn);

        if (!peerInfo.hasProtocol(IPFS.RELAY_PROTOCOL_HOP)) {
            conn.close();
            throw new Exception("does not support relay hop");
        }

        Multiaddr observed = peerInfo.getObserved();
        if (observed == null) {
            conn.close();
            throw new RuntimeException("does not return observed address");
        }

        Circuit.Reservation reservation = RelayService.reserve(conn);
        Reservation done = new Reservation(relayId, multiaddr, observed, reservation);
        reservations.put(relayId, done);
        swarmEnhance(relayId); // for now it might be ok

        return done;
    }

    public void addConnection(@NonNull PeerId peerId, @NonNull QuicConnection conn) {
        if (LogUtils.isDebug()) {
            if (conn instanceof RelayConnection) {
                throw new RuntimeException("not allowed");
            }
        }

        connections.put(peerId, conn);
    }

    @Nullable
    public QuicConnection getConnection(@NonNull PeerId peerId) {
        if (hasConnection(peerId)) {
            return connections.get(peerId);
        }
        return null;
    }

    public RelayConnection createRelayConnection(@NonNull PeerId peerId,
                                                 @NonNull Multiaddr multiaddr,
                                                 boolean keepConnection) throws Exception {
        return RelayService.createRelayConnection(this, peerId, multiaddr,
                IPFS.CONNECT_TIMEOUT, IPFS.CONNECT_TIMEOUT, IPFS.MAX_STREAMS, IPFS.MESSAGE_SIZE_MAX,
                keepConnection);
    }

    public boolean hasReservations() {
        return reservations.size() > 0;
    }

    public void clearSwarm() {
        swarm.clear();
    }

    private enum Protocol {
        IPv4, IPv6, IPv4_IPv6
    }
}


