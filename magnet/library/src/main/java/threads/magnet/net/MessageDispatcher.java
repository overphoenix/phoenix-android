package threads.magnet.net;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.Supplier;

import threads.magnet.LogUtils;
import threads.magnet.Settings;
import threads.magnet.metainfo.TorrentId;
import threads.magnet.protocol.Message;
import threads.magnet.service.RuntimeLifecycleBinder;
import threads.magnet.torrent.TorrentRegistry;

public class MessageDispatcher {

    private static final String TAG = MessageDispatcher.class.getSimpleName();
    private final Map<TorrentId, Map<ConnectionKey, Collection<Consumer<Message>>>> consumers;
    private final Map<TorrentId, Map<ConnectionKey, Collection<Supplier<Message>>>> suppliers;

    private final TorrentRegistry torrentRegistry;
    private final Object modificationLock;

    public MessageDispatcher(RuntimeLifecycleBinder lifecycleBinder,
                             PeerConnectionPool pool,
                             TorrentRegistry torrentRegistry) {

        this.consumers = new ConcurrentHashMap<>();
        this.suppliers = new ConcurrentHashMap<>();
        this.torrentRegistry = torrentRegistry;
        this.modificationLock = new Object();

        initializeMessageLoop(lifecycleBinder, pool);
    }

    private void initializeMessageLoop(RuntimeLifecycleBinder lifecycleBinder,
                                       PeerConnectionPool pool) {
        ExecutorService executor = Executors.newSingleThreadExecutor(r -> new Thread(r, "bt.net.message-dispatcher"));
        LoopControl loopControl = new LoopControl(Settings.maxMessageProcessingInterval.toMillis());
        MessageDispatchingLoop loop = new MessageDispatchingLoop(pool, loopControl);
        lifecycleBinder.onStartup("Initialize message dispatcher", () -> executor.execute(loop));
        lifecycleBinder.onShutdown("Shutdown message dispatcher", () -> {
            try {
                loop.shutdown();
            } finally {
                executor.shutdownNow();
            }
        });
    }

    public void addMessageConsumer(ConnectionKey connectionKey, Consumer<Message> messageConsumer) {
        synchronized (modificationLock) {
            Map<ConnectionKey, Collection<Consumer<Message>>> consumerMapByPeer =
                    consumers.computeIfAbsent(connectionKey.getTorrentId(), it -> new ConcurrentHashMap<>());

            Collection<Consumer<Message>> peerConsumers =
                    consumerMapByPeer.computeIfAbsent(connectionKey, it -> ConcurrentHashMap.newKeySet());

            peerConsumers.add(messageConsumer);
        }
    }

    public void addMessageSupplier(ConnectionKey connectionKey, Supplier<Message> messageSupplier) {
        synchronized (modificationLock) {
            Map<ConnectionKey, Collection<Supplier<Message>>> supplierMapByPeer =
                    suppliers.computeIfAbsent(connectionKey.getTorrentId(), it -> new ConcurrentHashMap<>());

            Collection<Supplier<Message>> peerSuppliers =
                    supplierMapByPeer.computeIfAbsent(connectionKey, it -> ConcurrentHashMap.newKeySet());

            peerSuppliers.add(messageSupplier);
        }
    }

    /**
     * Controls the amount of time to sleep after each iteration of the main message processing loop.
     * It implements an adaptive strategy and increases the amount of time for the dispatcher to sleep
     * after each iteration during which no messages were either received or sent.
     * This strategy greatly reduces CPU load when there is little network activity.
     */
    private static class LoopControl {

        private final long maxTimeToSleep;
        private int messagesProcessed;
        private long timeToSleep;

        LoopControl(long maxTimeToSleep) {
            this.maxTimeToSleep = maxTimeToSleep;
            reset();
        }

        private void reset() {
            messagesProcessed = 0;
            timeToSleep = 1;
        }

        void incrementProcessed() {
            messagesProcessed++;
        }

        synchronized void iterationFinished() {
            if (messagesProcessed > 0) {
                reset();
            } else {
                try {
                    wait(timeToSleep);
                } catch (InterruptedException e) {
                    LogUtils.error(TAG, "Wait interrupted");
                }

                if (timeToSleep < maxTimeToSleep) {
                    timeToSleep = Math.min(timeToSleep << 1, maxTimeToSleep);
                } else {
                    timeToSleep = maxTimeToSleep;
                }
            }
        }
    }

    private class MessageDispatchingLoop implements Runnable {
        private final PeerConnectionPool pool;
        private final LoopControl loopControl;

        private volatile boolean shutdown;

        MessageDispatchingLoop(PeerConnectionPool pool, LoopControl loopControl) {
            this.pool = pool;
            this.loopControl = loopControl;
        }

        @Override
        public void run() {
            while (!shutdown) {
                if (!consumers.isEmpty()) {
                    Iterator<Map.Entry<TorrentId, Map<ConnectionKey, Collection<Consumer<Message>>>>> iter = consumers.entrySet().iterator();
                    while (iter.hasNext()) {
                        Map.Entry<TorrentId, Map<ConnectionKey, Collection<Consumer<Message>>>> consumerMapByTorrent = iter.next();
                        Map<ConnectionKey, Collection<Consumer<Message>>> consumerMapByPeer = consumerMapByTorrent.getValue();
                        if (consumerMapByPeer.isEmpty()) {
                            synchronized (modificationLock) {
                                if (consumerMapByPeer.isEmpty()) {
                                    iter.remove();
                                }
                            }
                        }
                        TorrentId torrentId = consumerMapByTorrent.getKey();
                        if (torrentRegistry.isSupportedAndActive(torrentId)) {
                            processConsumerMap(torrentId);
                        }
                    }
                }

                if (!suppliers.isEmpty()) {
                    Iterator<Map.Entry<TorrentId, Map<ConnectionKey, Collection<Supplier<Message>>>>> iter = suppliers.entrySet().iterator();
                    while (iter.hasNext()) {
                        Map.Entry<TorrentId, Map<ConnectionKey, Collection<Supplier<Message>>>> supplierMapByTorrent = iter.next();
                        Map<ConnectionKey, Collection<Supplier<Message>>> supplierMapByPeer = supplierMapByTorrent.getValue();
                        if (supplierMapByPeer.isEmpty()) {
                            synchronized (modificationLock) {
                                if (supplierMapByPeer.isEmpty()) {
                                    iter.remove();
                                }
                            }
                        }
                        TorrentId torrentId = supplierMapByTorrent.getKey();
                        if (torrentRegistry.isSupportedAndActive(torrentId)) {
                            processSupplierMap(torrentId);
                        }
                    }
                }

                loopControl.iterationFinished();
            }
        }

        private void processConsumerMap(TorrentId torrentId) {
            Map<ConnectionKey, Collection<Consumer<Message>>> consumerMap = consumers.get(torrentId);
            if (consumerMap.isEmpty()) {
                return;
            }

            Iterator<Map.Entry<ConnectionKey, Collection<Consumer<Message>>>> iter = consumerMap.entrySet().iterator();
            while (iter.hasNext()) {
                Map.Entry<ConnectionKey, Collection<Consumer<Message>>> e = iter.next();
                ConnectionKey connectionKey = e.getKey();
                Collection<Consumer<Message>> peerConsumers = e.getValue();
                if (peerConsumers.isEmpty()) {
                    synchronized (modificationLock) {
                        if (peerConsumers.isEmpty()) {
                            iter.remove();
                        }
                    }
                } else {
                    PeerConnection connection = pool.getConnection(connectionKey);
                    if (connection != null && !connection.isClosed()) {
                        Message message;
                        for (; ; ) {
                            try {
                                message = connection.readMessageNow();
                            } catch (Exception ex) {
                                LogUtils.error(TAG, "Error when reading message from peer connection: " + connectionKey.getPeer(), ex);
                                break;
                            }

                            if (message == null) {
                                break;
                            }

                            loopControl.incrementProcessed();
                            for (Consumer<Message> consumer : peerConsumers) {
                                try {
                                    consumer.accept(message);
                                } catch (Exception ex) {
                                    LogUtils.error(TAG, "Error in message consumer", ex);
                                }
                            }
                        }
                    }
                }
            }
        }

        private void processSupplierMap(TorrentId torrentId) {
            Map<ConnectionKey, Collection<Supplier<Message>>> supplierMap = suppliers.get(torrentId);
            if (supplierMap.isEmpty()) {
                return;
            }

            Iterator<Map.Entry<ConnectionKey, Collection<Supplier<Message>>>> iter = supplierMap.entrySet().iterator();
            while (iter.hasNext()) {
                Map.Entry<ConnectionKey, Collection<Supplier<Message>>> e = iter.next();
                ConnectionKey connectionKey = e.getKey();
                Collection<Supplier<Message>> peerSuppliers = e.getValue();
                if (peerSuppliers.isEmpty()) {
                    synchronized (modificationLock) {
                        if (peerSuppliers.isEmpty()) {
                            iter.remove();
                        }
                    }
                } else {
                    PeerConnection connection = pool.getConnection(connectionKey);
                    if (connection != null && !connection.isClosed()) {
                        for (Supplier<Message> messageSupplier : peerSuppliers) {
                            Message message;
                            try {
                                message = messageSupplier.get();
                            } catch (Exception ex) {
                                LogUtils.error(TAG, "Error in message supplier", ex);
                                continue;
                            }

                            if (message == null) {
                                continue;
                            }

                            loopControl.incrementProcessed();
                            try {
                                connection.postMessage(message);
                            } catch (Exception ex) {
                                LogUtils.error(TAG, "Error when writing message", ex);
                            }
                        }
                    }
                }
            }
        }

        void shutdown() {
            shutdown = true;
        }
    }
}
