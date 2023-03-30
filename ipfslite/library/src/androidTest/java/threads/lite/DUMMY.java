package threads.lite;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.common.primitives.Bytes;
import com.google.protobuf.ByteString;

import net.luminis.quic.QuicConnection;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ConnectException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import identify.pb.IdentifyOuterClass;
import threads.lite.cid.Cid;
import threads.lite.cid.Multiaddr;
import threads.lite.cid.PeerId;
import threads.lite.core.Closeable;
import threads.lite.core.ClosedException;
import threads.lite.core.Progress;
import threads.lite.crypto.PrivKey;
import threads.lite.crypto.Rsa;
import threads.lite.format.Block;
import threads.lite.format.BlockStore;
import threads.lite.format.Node;
import threads.lite.host.LiteHost;
import threads.lite.host.LiteHostCertificate;
import threads.lite.host.PeerInfo;
import threads.lite.ident.IdentityService;
import threads.lite.ipns.Ipns;
import threads.lite.push.Push;
import threads.lite.push.PushService;
import threads.lite.utils.Link;
import threads.lite.utils.LinkCloseable;
import threads.lite.utils.ProgressStream;
import threads.lite.utils.Reader;
import threads.lite.utils.ReaderProgress;
import threads.lite.utils.ReaderStream;
import threads.lite.utils.Resolver;
import threads.lite.utils.Stream;
import threads.lite.utils.WriterStream;

public class DUMMY {

    public static final String IPFS_PATH = "/ipfs/";
    public static final String IPNS_PATH = "/ipns/";


    public static final long RESOLVE_MAX_TIME = 30000; // 30 sec
    public static final int TIMEOUT_BOOTSTRAP = 10;
    public static final int RESOLVE_TIMEOUT = 1000; // 1 sec

    private static final String TAG = DUMMY.class.getSimpleName();

    @NonNull
    private final LiteHost host;
    @NonNull
    private final PrivKey privateKey;
    @NonNull
    private final BlockStore blockstore;

    private DUMMY(@NonNull Context context) throws Exception {

        KeyPair keypair = getKeyPair(context);
        privateKey = new Rsa.RsaPrivateKey(keypair.getPrivate(), keypair.getPublic());
        LiteHostCertificate selfSignedCertificate = new LiteHostCertificate(context,
                privateKey, keypair);

        this.blockstore = new BlockStore() {
            private final ConcurrentHashMap<Cid, Block> blocks = new ConcurrentHashMap<>();

            @Override
            public boolean hasBlock(@NonNull Cid cid) {
                return blocks.containsKey(cid);
            }

            @Override
            public Block getBlock(@NonNull Cid cid) {
                return blocks.get(cid);
            }

            @Override
            public void deleteBlock(@NonNull Cid cid) {
                blocks.remove(cid);
            }

            @Override
            public void deleteBlocks(@NonNull List<Cid> cids) {
                for (Cid cid : cids) {
                    deleteBlock(cid);
                }
            }

            @Override
            public void putBlock(@NonNull Block block) {
                blocks.put(block.getCid(), block);
            }

            @Override
            public int getSize(@NonNull Cid cid) {
                Block block = getBlock(cid);
                if (block != null) {
                    return block.getRawData().length;
                }
                return -1;
            }

            @Override
            public void clear() {
                blocks.clear();
            }
        };

        this.host = new LiteHost(selfSignedCertificate, privateKey, blockstore,
                LiteHost.nextFreePort(), 25);

    }


    @SuppressWarnings("UnusedReturnValue")
    public static long copy(InputStream source, OutputStream sink) throws IOException {
        long nread = 0L;
        byte[] buf = new byte[4096];
        int n;
        while ((n = source.read(buf)) > 0) {
            sink.write(buf, 0, n);
            nread += n;
        }
        return nread;
    }

    public static void copy(@NonNull InputStream source, @NonNull OutputStream sink, @NonNull ReaderProgress progress) throws IOException {
        long nread = 0L;
        byte[] buf = new byte[4096];
        int remember = 0;
        int n;
        while ((n = source.read(buf)) > 0) {
            sink.write(buf, 0, n);
            nread += n;

            if (progress.doProgress()) {
                if (progress.getSize() > 0) {
                    int percent = (int) ((nread * 100.0f) / progress.getSize());
                    if (remember < percent) {
                        remember = percent;
                        progress.setProgress(percent);
                    }
                }
            }
        }
    }


    @NonNull
    public static DUMMY getInstance(@NonNull Context context) {

        synchronized (DUMMY.class) {

            try {
                return new DUMMY(context);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

        }

    }

    private KeyPair getKeyPair(@NonNull Context context) throws NoSuchAlgorithmException, InvalidKeySpecException {

        String algorithm = "RSA";
        final KeyPair keypair;

        KeyPairGenerator keyGen = KeyPairGenerator.getInstance(algorithm);
        keyGen.initialize(2048, LiteHostCertificate.ThreadLocalInsecureRandom.current());
        keypair = keyGen.generateKeyPair();

        return keypair;
    }


    @NonNull
    public PeerId getPeerId(@NonNull String name) {
        return PeerId.decodeName(name);
    }

    @NonNull
    public PeerInfo getIdentity() {

        IdentifyOuterClass.Identify identity = host.createIdentity(null);


        String agent = identity.getAgentVersion();
        String version = identity.getProtocolVersion();
        Multiaddr observedAddr = null;
        if (identity.hasObservedAddr()) {
            observedAddr = new Multiaddr(identity.getObservedAddr().toByteArray());
        }

        List<String> protocols = new ArrayList<>();
        List<Multiaddr> addresses = new ArrayList<>();
        List<ByteString> entries = identity.getProtocolsList().asByteStringList();
        for (ByteString entry : entries) {
            protocols.add(entry.toStringUtf8());
        }
        entries = identity.getListenAddrsList();
        for (ByteString entry : entries) {
            addresses.add(new Multiaddr(entry.toByteArray()));
        }

        return new PeerInfo(agent, version, addresses, protocols, observedAddr);

    }

    @NonNull
    public List<Multiaddr> listenAddresses() {
        return host.listenAddresses(false);
    }

    public int getPort() {
        return host.getPort();
    }

    @NonNull
    public Cid storeFile(@NonNull File target) throws Exception {
        try (FileInputStream inputStream = new FileInputStream(target)) {
            return storeInputStream(inputStream);
        }
    }

    public void provide(@NonNull Cid cid, @NonNull Closeable closable) {
        try {
            host.getRouting().provide(closable, cid);
        } catch (Throwable throwable) {
            LogUtils.error(TAG, throwable);
        }
    }

    /**
     * Note: be carefully on using it on a directory
     * (recursion problem using same file in different locations)
     *
     * @param cid Cid object
     */
    public void rm(@NonNull Cid cid) {
        try {
            List<Cid> cids = getBlocks(cid);
            cids.add(cid);
            blockstore.deleteBlocks(cids);
        } catch (Throwable throwable) {
            LogUtils.error(TAG, throwable);
        }
    }

    public List<Cid> getBlocks(@NonNull Cid cid) throws ClosedException {
        List<Cid> result = new ArrayList<>();
        List<Link> links = ls(cid, false, () -> false);
        if (links != null) {
            for (Link link : links) {
                result.add(link.getCid());
                if (link.isRaw() || link.isUnknown()) {
                    result.addAll(getBlocks(link.getCid()));
                }
            }
        }
        return result;
    }

    @NonNull
    public Cid storeData(@NonNull byte[] data) throws IOException {

        try (InputStream inputStream = new ByteArrayInputStream(data)) {
            return storeInputStream(inputStream);
        }
    }

    @NonNull
    public Cid storeText(@NonNull String content) throws IOException {

        try (InputStream inputStream = new ByteArrayInputStream(content.getBytes())) {
            return storeInputStream(inputStream);
        }
    }

    public void storeToFile(@NonNull File file, @NonNull Cid cid, @NonNull Closeable closeable) throws Exception {

        try (FileOutputStream fileOutputStream = new FileOutputStream(file)) {
            storeToOutputStream(fileOutputStream, cid, closeable);
        }
    }

    public void storeToFile(@NonNull File file, @NonNull Cid cid, @NonNull Progress progress) throws Exception {

        try (FileOutputStream fileOutputStream = new FileOutputStream(file)) {
            storeToOutputStream(fileOutputStream, cid, progress);
        }
    }

    @NonNull
    public Cid storeInputStream(@NonNull InputStream inputStream,
                                @NonNull Progress progress, long size) {

        return Stream.write(blockstore, new WriterStream(inputStream, progress, size));

    }

    @NonNull
    public Cid storeInputStream(@NonNull InputStream inputStream) {

        return storeInputStream(inputStream, new Progress() {
            @Override
            public boolean isClosed() {
                return false;
            }

            @Override
            public void setProgress(int progress) {
            }

            @Override
            public boolean doProgress() {
                return false;
            }


        }, 0);
    }

    @Nullable
    public String getText(@NonNull Cid cid, @NonNull Closeable closeable) throws IOException {

        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            getToOutputStream(outputStream, cid, closeable);
            return outputStream.toString();
        }
    }

    public void storeToOutputStream(@NonNull OutputStream os, @NonNull Cid cid,
                                    @NonNull Progress progress) throws IOException {

        long totalRead = 0L;
        int remember = 0;

        Reader reader = getReader(cid, progress);
        long size = reader.getSize();
        byte[] buf = reader.loadNextData();
        while (buf != null && buf.length > 0) {

            if (progress.isClosed()) {
                throw new ClosedException();
            }

            // calculate progress
            totalRead += buf.length;
            if (progress.doProgress()) {
                if (size > 0) {
                    int percent = (int) ((totalRead * 100.0f) / size);
                    if (remember < percent) {
                        remember = percent;
                        progress.setProgress(percent);
                    }
                }
            }

            os.write(buf, 0, buf.length);

            buf = reader.loadNextData();

        }
    }

    public void storeToOutputStream(@NonNull OutputStream os, @NonNull Cid cid,
                                    @NonNull Closeable closeable) throws IOException {

        Reader reader = getReader(cid, closeable);
        byte[] buf = reader.loadNextData();
        while (buf != null && buf.length > 0) {

            os.write(buf, 0, buf.length);
            buf = reader.loadNextData();
        }
    }

    @NonNull
    public byte[] getData(@NonNull Cid cid, @NonNull Progress progress) throws IOException {

        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            storeToOutputStream(outputStream, cid, progress);
            return outputStream.toByteArray();
        }
    }

    @NonNull
    public byte[] getData(@NonNull Cid cid, @NonNull Closeable closeable) throws IOException {

        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            storeToOutputStream(outputStream, cid, closeable);
            return outputStream.toByteArray();
        }
    }

    @Nullable
    public Cid rmLinkFromDir(@NonNull Cid dir, String name) {
        try {
            return Stream.removeLinkFromDir(blockstore, () -> false, dir, name);
        } catch (Throwable throwable) {
            LogUtils.error(TAG, throwable);
        }
        return null;
    }

    @Nullable
    public Cid addLinkToDir(@NonNull Cid dir, @NonNull String name, @NonNull Cid link) {
        try {
            return Stream.addLinkToDir(blockstore, () -> false, dir, name, link);
        } catch (Throwable throwable) {
            LogUtils.error(TAG, throwable);
        }
        return null;
    }

    @Nullable
    public Cid createEmptyDir() {
        try {
            return Stream.createEmptyDir(blockstore);
        } catch (Throwable throwable) {
            LogUtils.error(TAG, throwable);
        }
        return null;
    }

    @NonNull
    public String decodeName(@NonNull String name) {
        try {
            PeerId peerId = PeerId.decodeName(name);
            return peerId.toBase58();
        } catch (Throwable ignore) {
            // common use case to fail
        }
        return "";
    }

    @NonNull
    public PeerId getPeerID() {
        return host.self();
    }

    @NonNull
    public PeerInfo getPeerInfo(@NonNull QuicConnection conn)
            throws Exception {
        return IdentityService.getPeerInfo(conn);
    }

    public void shutdown() {
        try {
            host.shutdown();
        } catch (Throwable throwable) {
            LogUtils.error(TAG, throwable);
        }
    }


    @Nullable
    public QuicConnection connect(@NonNull PeerId peerId, int minStreams, boolean keepConnection) {
        try {
            return host.connect(peerId, IPFS.CONNECT_TIMEOUT, IPFS.GRACE_PERIOD, minStreams,
                    IPFS.MESSAGE_SIZE_MAX, keepConnection);
        } catch (ConnectException ignore) {
            // ignore
        } catch (Throwable throwable) {
            LogUtils.error(TAG, throwable);
        }
        return null;
    }

    @Nullable
    public Node resolveNode(@NonNull Cid root, @NonNull List<String> path, @NonNull Closeable closeable) throws ClosedException {

        String resultPath = IPFS_PATH + root.String();
        for (String name : path) {
            resultPath = resultPath.concat("/").concat(name);
        }

        return resolveNode(resultPath, closeable);

    }

    @Nullable
    public Node resolveNode(@NonNull String path, @NonNull Closeable closeable) throws ClosedException {

        try {
            return Resolver.resolveNode(closeable, blockstore, host.getBitSwap(), path);
        } catch (ClosedException closedException) {
            throw closedException;
        } catch (Throwable ignore) {
            // common exception to not resolve a a path
        }
        return null;
    }


    public void publishName(@NonNull Cid cid, int sequence, @NonNull Closeable closeable) {

        try {
            host.publishName(closeable, privateKey, IPFS_PATH + cid.String(), getPeerID(), sequence);
        } catch (Throwable throwable) {
            LogUtils.error(TAG, throwable);
        }
    }

    public void publishName(@NonNull String name, int sequence, @NonNull Closeable closeable) {

        try {
            host.publishName(closeable, privateKey, name, getPeerID(), sequence);
        } catch (Throwable throwable) {
            LogUtils.error(TAG, throwable);
        }
    }

    public void clearDatabase() {
        blockstore.clear();
    }


    @NonNull
    public Multiaddr remoteAddress(@NonNull QuicConnection conn) {
        return Multiaddr.transform(conn.getRemoteAddress());
    }

    @Nullable
    public Cid resolve(@NonNull Cid root, @NonNull List<String> path,
                       @NonNull Closeable closeable) throws ClosedException {

        String resultPath = IPFS_PATH + root.String();
        for (String name : path) {
            resultPath = resultPath.concat("/").concat(name);
        }

        return resolve(resultPath, closeable);

    }

    @Nullable
    public Cid resolve(@NonNull String path, @NonNull Closeable closeable) throws ClosedException {

        try {
            Node node = Resolver.resolveNode(closeable, blockstore, host.getBitSwap(), path);
            if (node != null) {
                return node.getCid();
            }
        } catch (ClosedException closedException) {
            throw closedException;
        } catch (Throwable ignore) {
            // common use case not resolve a a path
        }
        return null;
    }

    public boolean resolve(@NonNull Cid cid, @NonNull String name,
                           @NonNull Closeable closeable) throws ClosedException {
        Cid res = resolve(IPFS_PATH + cid.String() + "/" + name, closeable);
        return res != null;
    }

    public boolean isDir(@NonNull Cid cid, @NonNull Closeable closeable) throws ClosedException {

        boolean result;
        try {
            result = Stream.isDir(closeable, blockstore, host.getBitSwap(), cid);
        } catch (ClosedException closedException) {
            throw closedException;
        } catch (Throwable e) {
            result = false;
        }
        return result;
    }

    public long getSize(@NonNull Cid cid, @NonNull Closeable closeable) throws ClosedException {
        List<Link> links = ls(cid, true, closeable);
        int size = -1;
        if (links != null) {
            for (Link info : links) {
                size += info.getSize();
            }
        }
        return size;
    }


    @Nullable
    public List<Link> getLinks(@NonNull Cid cid, boolean resolveChildren,
                               @NonNull Closeable closeable) throws ClosedException {

        List<Link> links = ls(cid, resolveChildren, closeable);
        if (links == null) {
            LogUtils.info(TAG, "no links");
            return null;
        }

        List<Link> result = new ArrayList<>();
        for (Link link : links) {

            if (!link.getName().isEmpty()) {
                result.add(link);
            }
        }
        return result;
    }

    @Nullable
    public List<Link> ls(@NonNull Cid cid, boolean resolveChildren,
                         @NonNull Closeable closeable) throws ClosedException {

        List<Link> infoList = new ArrayList<>();
        try {
            Stream.ls(new LinkCloseable() {

                @Override
                public boolean isClosed() {
                    return closeable.isClosed();
                }

                @Override
                public void info(@NonNull Link link) {
                    infoList.add(link);
                }
            }, blockstore, host.getBitSwap(), cid, resolveChildren);

        } catch (ClosedException closedException) {
            throw closedException;
        } catch (Throwable e) {
            return null;
        }
        return infoList;
    }

    @NonNull
    public Reader getReader(@NonNull Cid cid, @NonNull Closeable closeable) throws ClosedException {
        return Reader.getReader(closeable, blockstore, host.getBitSwap(), cid);
    }

    private void getToOutputStream(@NonNull OutputStream outputStream, @NonNull Cid cid,
                                   @NonNull Closeable closeable) throws IOException {
        try (InputStream inputStream = getInputStream(cid, closeable)) {
            DUMMY.copy(inputStream, outputStream);
        }
    }

    public void setPusher(@Nullable Push push) {
        this.host.setPush(push);
    }

    @NonNull
    public InputStream getLoaderStream(@NonNull Cid cid, @NonNull Closeable closeable) throws ClosedException {
        Reader loader = getReader(cid, closeable);
        return new ReaderStream(loader);
    }

    @NonNull
    public InputStream getLoaderStream(@NonNull Cid cid, @NonNull Progress progress) throws ClosedException {
        Reader loader = getReader(cid, progress);
        return new ProgressStream(loader, progress);

    }


    @Nullable
    public Ipns.Entry resolveName(@NonNull String name, long last,
                                  @NonNull Closeable closeable) {

        return resolveName(PeerId.decodeName(name), last, closeable);
    }

    @Nullable
    private Ipns.Entry resolveName(@NonNull PeerId id, long last, @NonNull Closeable closeable) {

        long time = System.currentTimeMillis();

        AtomicReference<Ipns.Entry> resolvedName = new AtomicReference<>(null);
        try {
            AtomicLong timeout = new AtomicLong(System.currentTimeMillis() + RESOLVE_MAX_TIME);

            byte[] ipns = DUMMY.IPNS_PATH.getBytes();
            byte[] ipnsKey = Bytes.concat(ipns, id.getBytes());

            host.getRouting().searchValue(
                    () -> (timeout.get() < System.currentTimeMillis()) || closeable.isClosed(),
                    entry -> {

                        long sequence = entry.getSequence();

                        LogUtils.debug(TAG, "IpnsEntry : " + entry +
                                (System.currentTimeMillis() - time));

                        if (sequence < last) {
                            // newest value already available
                            LogUtils.debug(TAG, "newest value " + sequence);
                            timeout.set(System.currentTimeMillis());
                            return;
                        }


                        resolvedName.set(entry);
                        timeout.set(System.currentTimeMillis() + RESOLVE_TIMEOUT);

                    }, ipnsKey);

        } catch (Throwable throwable) {
            LogUtils.error(TAG, throwable);
        }


        LogUtils.debug(TAG, "Finished resolve name " + id.toBase58() + " " +
                (System.currentTimeMillis() - time));


        return resolvedName.get();
    }

    @NonNull
    public InputStream getInputStream(@NonNull Cid cid, @NonNull Closeable closeable) throws ClosedException {
        Reader reader = getReader(cid, closeable);
        return new ReaderStream(reader);
    }

    public boolean isValidCID(@NonNull String cid) {
        try {
            return !Cid.decode(cid).String().isEmpty();
        } catch (Throwable e) {
            return false;
        }
    }


    public void reset() {
        try {
            host.getBitSwap().reset();
        } catch (Throwable throwable) {
            LogUtils.error(TAG, throwable);
        }
    }

    public boolean notify(@NonNull QuicConnection conn, @NonNull String content) {
        try {
            PushService.notify(conn, content);
            return true;
        } catch (Throwable throwable) {
            LogUtils.error(TAG, throwable);
        }
        return false;
    }

    public void swarmReduce(@NonNull PeerId peerId) {
        host.swarmReduce(peerId);
    }

    public void swarmEnhance(@NonNull PeerId peerId) {
        host.swarmEnhance(peerId);
    }


    public void swarmEnhance(@NonNull PeerId[] peerIds) {
        for (PeerId peerId : peerIds) {
            swarmEnhance(peerId);
        }
    }


    public Set<Multiaddr> getAddresses(@NonNull PeerId peerId) {
        return host.getAddresses(peerId);
    }


    public void addMultiAddress(@NonNull PeerId peerId, @NonNull Multiaddr multiaddr) {
        host.addToAddressBook(peerId, Collections.singletonList(multiaddr));
    }


    @Nullable
    public QuicConnection find(@NonNull PeerId peerId, int initialMaxStreams,
                               boolean keepConnection, @NonNull Closeable closeable) {
        return host.find(peerId, IPFS.CONNECT_TIMEOUT, initialMaxStreams,
                IPFS.MESSAGE_SIZE_MAX, keepConnection, closeable);
    }

    @NonNull
    public LiteHost getHost() {
        return host;
    }


}
