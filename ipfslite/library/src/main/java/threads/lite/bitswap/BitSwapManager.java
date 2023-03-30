package threads.lite.bitswap;

import androidx.annotation.NonNull;

import net.luminis.quic.QuicConnection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import threads.lite.IPFS;
import threads.lite.LogUtils;
import threads.lite.cid.Cid;
import threads.lite.cid.Peer;
import threads.lite.cid.PeerId;
import threads.lite.core.Closeable;
import threads.lite.core.ClosedException;
import threads.lite.format.Block;
import threads.lite.format.BlockStore;
import threads.lite.host.LiteHost;


public class BitSwapManager {

    private static final String TAG = BitSwapManager.class.getSimpleName();

    private final LiteHost host;
    private final BlockStore blockStore;
    private final ScheduledThreadPoolExecutor providers = new ScheduledThreadPoolExecutor(6);
    private final ExecutorService connector = Executors.newFixedThreadPool(6);
    private final ConcurrentHashMap<PeerId, QuicConnection> peers = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Cid, ConcurrentLinkedDeque<QuicConnection>> matches = new ConcurrentHashMap<>();
    private final Blocker blocker = new Blocker();
    private final BitSwap bitSwap;

    public BitSwapManager(@NonNull BitSwap bitSwap, @NonNull BlockStore blockStore, @NonNull LiteHost host) {
        this.bitSwap = bitSwap;
        this.blockStore = blockStore;
        this.host = host;
    }

    private void addPeer(@NonNull PeerId peerId, @NonNull QuicConnection conn) {
        peers.put(peerId, conn);
    }

    public void haveReceived(@NonNull QuicConnection conn, @NonNull List<Cid> cids) {

        for (Cid cid : cids) {
            ConcurrentLinkedDeque<QuicConnection> res = matches.get(cid);
            if (res != null) {
                res.add(conn);
            }
        }
    }

    public void reset() {

        LogUtils.debug(TAG, "Reset");
        try {
            for (Map.Entry<PeerId, QuicConnection> entry : peers.entrySet()) {
                PeerId peerId = entry.getKey();
                QuicConnection conn = entry.getValue();
                if (host.isNotProtected(peerId)) {
                    conn.close();
                }
            }
        } catch (Throwable throwable) {
            LogUtils.error(TAG, throwable);
        }
        try {
            peers.clear();
            matches.clear();
        } catch (Throwable throwable) {
            LogUtils.error(TAG, throwable);
        }
    }

    public void connectPeer(@NonNull Closeable closeable, @NonNull Peer peer) {
        try {
            if (closeable.isClosed()) {
                return;
            }

            QuicConnection conn = host.connect(peer, IPFS.CONNECT_TIMEOUT, IPFS.GRACE_PERIOD,
                    IPFS.MAX_STREAMS, IPFS.MESSAGE_SIZE_MAX,
                    true);

            LogUtils.debug(TAG, "New connection " + peer.getPeerId().toBase58());

            if (closeable.isClosed()) {
                return;
            }

            addPeer(peer.getPeerId(), conn);

        } catch (Throwable ignore) {
            // ignore
        }
    }


    public void runHaveMessage(@NonNull Closeable closeable, QuicConnection conn,
                               @NonNull List<Cid> cids) {
        new Thread(() -> {
            long start = System.currentTimeMillis();
            boolean success = false;
            try {
                if (closeable.isClosed()) {
                    return;
                }
                bitSwap.sendHaveMessage(conn, cids);
                success = true;
            } catch (Throwable throwable) {
                LogUtils.error(TAG, "runHaveMessage " + throwable.getClass().getName());
            } finally {
                LogUtils.debug(TAG, "runHaveMessage " + success + " " +
                        " took " + (System.currentTimeMillis() - start));
            }
        }).start();
    }


    public Block runWantHaves(@NonNull Closeable closeable, @NonNull Cid cid) throws ClosedException {

        matches.put(cid, new ConcurrentLinkedDeque<>());


        loadProviders(closeable, cid, IPFS.BITSWAP_LOAD_PROVIDERS_DELAY, TimeUnit.SECONDS);

        Set<QuicConnection> haves = new HashSet<>();

        Set<Peer> swarm = host.getPeers();

        for (Peer peer : swarm) {
            connector.execute(() -> connectPeer(closeable, peer));
        }

        while (matches.containsKey(cid)) {

            if (closeable.isClosed()) {
                throw new ClosedException();
            }

            for (QuicConnection peer : peers.values()) {
                if (!haves.contains(peer)) {
                    haves.add(peer);
                    runHaveMessage(closeable, peer, Collections.singletonList(cid));
                }
            }

            ConcurrentLinkedDeque<QuicConnection> set = matches.get(cid);
            if (set != null) {
                QuicConnection conn = set.poll();
                if (conn != null) {

                    long start = System.currentTimeMillis();
                    try {
                        if (matches.containsKey(cid)) {
                            bitSwap.sendWantsMessage(conn, Collections.singletonList(cid));

                            blocker.subscribe(cid, closeable);
                        }
                    } catch (Throwable throwable) {
                        LogUtils.error(TAG, throwable);
                    } finally {
                        LogUtils.debug(TAG, "Match CID " + cid.String() +
                                " took " + (System.currentTimeMillis() - start));
                    }
                }

            }

            if (closeable.isClosed()) {
                throw new ClosedException();
            }
        }
        return blockStore.getBlock(cid);
    }


    public void blockReceived(@NonNull Block block) {

        try {
            Cid cid = block.getCid();
            blockStore.putBlock(block);
            matches.remove(cid);
            blocker.release(cid);
        } catch (Throwable throwable) {
            LogUtils.error(TAG, throwable);
        }
    }


    public void loadBlocks(@NonNull Closeable closeable, @NonNull List<Cid> cids) {

        LogUtils.verbose(TAG, "LoadBlocks " + cids.size());

        List<QuicConnection> handled = new ArrayList<>();

        for (QuicConnection conn : peers.values()) {
            if (!handled.contains(conn)) {
                handled.add(conn);
                runHaveMessage(closeable, conn, cids);
            }
        }
    }

    public Block getBlock(@NonNull Closeable closeable, @NonNull Cid cid, boolean root) throws ClosedException {
        try {
            synchronized (cid.String().intern()) {
                Block block = blockStore.getBlock(cid);
                if (block == null) {
                    AtomicBoolean done = new AtomicBoolean(false);
                    LogUtils.info(TAG, "Block Get " + cid.String());

                    if (root) {
                        loadProviders(() -> closeable.isClosed() || done.get(), cid, 1,
                                TimeUnit.MILLISECONDS);
                    }
                    try {
                        return runWantHaves(() -> closeable.isClosed() || done.get(), cid);
                    } finally {
                        done.set(true);
                    }
                }
                return block;
            }
        } finally {
            blocker.release(cid);
            LogUtils.info(TAG, "Block Release  " + cid.String());
        }
    }

    private void loadProviders(@NonNull Closeable closeable, @NonNull Cid cid,
                               long delay, @NonNull TimeUnit delayUnit) {

        if (IPFS.BITSWAP_SUPPORT_FIND_PROVIDERS) {

            providers.schedule(() -> {

                long start = System.currentTimeMillis();
                try {

                    LogUtils.debug(TAG, "Load Provider Start " + cid.String());

                    if (closeable.isClosed()) {
                        return;
                    }


                    host.findProviders(closeable, (peer) -> {
                        if (peer.hasAddresses()) {
                            connector.execute(() -> connectPeer(closeable, peer));
                        }
                    }, cid, false);
                } catch (Throwable throwable) {
                    LogUtils.error(TAG, throwable.getMessage());
                } finally {
                    LogUtils.info(TAG, "Load Provider Finish " + cid.String() +
                            " onStart [" + (System.currentTimeMillis() - start) + "]...");
                }
            }, delay, delayUnit);
        }
    }

}
