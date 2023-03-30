package threads.magnet;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.Selector;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import threads.magnet.data.ChunkVerifier;
import threads.magnet.data.DataDescriptorFactory;
import threads.magnet.data.digest.JavaSecurityDigester;
import threads.magnet.dht.DHTHandshakeHandler;
import threads.magnet.dht.DHTPeerSourceFactory;
import threads.magnet.dht.DHTService;
import threads.magnet.event.EventBus;
import threads.magnet.event.EventSource;
import threads.magnet.magnet.UtMetadataMessageHandler;
import threads.magnet.net.BitfieldConnectionHandler;
import threads.magnet.net.ConnectionHandlerFactory;
import threads.magnet.net.ConnectionSource;
import threads.magnet.net.DataReceiver;
import threads.magnet.net.HandshakeHandler;
import threads.magnet.net.MessageDispatcher;
import threads.magnet.net.PeerConnectionFactory;
import threads.magnet.net.PeerConnectionPool;
import threads.magnet.net.PeerId;
import threads.magnet.net.SharedSelector;
import threads.magnet.net.SocketChannelConnectionAcceptor;
import threads.magnet.net.buffer.BufferManager;
import threads.magnet.net.buffer.IBufferManager;
import threads.magnet.net.extended.ExtendedProtocolHandshakeHandler;
import threads.magnet.net.pipeline.BufferedPieceRegistry;
import threads.magnet.net.pipeline.ChannelPipelineFactory;
import threads.magnet.net.portmapping.PortMapper;
import threads.magnet.net.portmapping.PortMappingInitializer;
import threads.magnet.peer.PeerRegistry;
import threads.magnet.peerexchange.PeerExchangeConfig;
import threads.magnet.peerexchange.PeerExchangeMessageHandler;
import threads.magnet.peerexchange.PeerExchangePeerSourceFactory;
import threads.magnet.protocol.HandshakeFactory;
import threads.magnet.protocol.Message;
import threads.magnet.protocol.StandardBittorrentProtocol;
import threads.magnet.protocol.extended.AlphaSortedMapping;
import threads.magnet.protocol.extended.ExtendedHandshakeFactory;
import threads.magnet.protocol.extended.ExtendedMessage;
import threads.magnet.protocol.extended.ExtendedMessageTypeMapping;
import threads.magnet.protocol.extended.ExtendedProtocol;
import threads.magnet.protocol.handler.MessageHandler;
import threads.magnet.protocol.handler.PortMessageHandler;
import threads.magnet.service.LifecycleBinding;
import threads.magnet.service.RuntimeLifecycleBinder;
import threads.magnet.torrent.BlockCache;
import threads.magnet.torrent.DataWorker;
import threads.magnet.torrent.DefaultDataWorker;
import threads.magnet.torrent.TorrentRegistry;

public class Runtime {


    private static final String TAG = Runtime.class.getSimpleName();

    public final MessageDispatcher mMessageDispatcher;
    public final ConnectionSource mConnectionSource;
    public final PeerRegistry mPeerRegistry;
    public final TorrentRegistry mTorrentRegistry;
    public final Set<IAgent> mMessagingAgents;
    public final DataWorker mDataWorker;
    public final PeerConnectionPool mConnectionPool;
    public final BufferedPieceRegistry mBufferedPieceRegistry;
    private final Object lock;
    private final ExecutorService mExecutor;
    private final EventBus mEventBus;
    private final RuntimeLifecycleBinder mRuntimeLifecycleBinder;
    private final Set<Client> knownClients;
    private final AtomicBoolean started;


    public Runtime(@NonNull PeerId peerId, @NonNull EventBus eventBus, int acceptorPort) {
        java.lang.Runtime.getRuntime().addShutdownHook(new Thread("bt.runtime.shutdown-manager") {
            @Override
            public void run() {
                shutdown();
            }
        });
        this.mEventBus = eventBus;
        this.mRuntimeLifecycleBinder = new RuntimeLifecycleBinder();
        new PeerExchangePeerSourceFactory(
                mEventBus, mRuntimeLifecycleBinder, new PeerExchangeConfig());


        this.knownClients = ConcurrentHashMap.newKeySet();

        this.mExecutor = Executors.newSingleThreadExecutor();

        SharedSelector mSelector = provideSelector(mRuntimeLifecycleBinder);

        JavaSecurityDigester digester = provideDigester();
        ChunkVerifier chunkVerifier = provideVerifier(eventBus, digester);


        DataDescriptorFactory dataDescriptorFactory = provideDataDescriptorFactory(chunkVerifier);

        this.mTorrentRegistry = new TorrentRegistry(
                dataDescriptorFactory, mRuntimeLifecycleBinder);

        this.mPeerRegistry = new PeerRegistry(mRuntimeLifecycleBinder,
                mTorrentRegistry, mEventBus, peerId, acceptorPort);


        Set<PortMapper> portMappers = new HashSet<>();

        PortMappingInitializer.portMappingInitializer(portMappers, mRuntimeLifecycleBinder, acceptorPort);


        DataReceiver dataReceiver = new DataReceiver(mSelector, mRuntimeLifecycleBinder);
        BufferManager bufferManager = new BufferManager();
        mBufferedPieceRegistry = new BufferedPieceRegistry();
        ChannelPipelineFactory channelPipelineFactory =
                new ChannelPipelineFactory(bufferManager, mBufferedPieceRegistry);

        Set<HandshakeHandler> boundHandshakeHandlers = new HashSet<>();
        Map<String, MessageHandler<? extends ExtendedMessage>> handlersByTypeName = new HashMap<>();
        handlersByTypeName.put("ut_pex", new PeerExchangeMessageHandler());
        handlersByTypeName.put("ut_metadata", new UtMetadataMessageHandler());
        ExtendedMessageTypeMapping messageTypeMapping =
                provideExtendedMessageTypeMapping(handlersByTypeName);

        ExtendedProtocol extendedProtocol = new ExtendedProtocol(messageTypeMapping, handlersByTypeName);
        PortMessageHandler portMessageHandler = new PortMessageHandler();
        Map<Integer, MessageHandler<?>> extraHandlers = new HashMap<>();
        extraHandlers.put(PortMessageHandler.PORT_ID, portMessageHandler);
        extraHandlers.put(ExtendedProtocol.EXTENDED_MESSAGE_ID, extendedProtocol);
        MessageHandler<Message> bittorrentProtocol = new StandardBittorrentProtocol(extraHandlers);

        HandshakeFactory handshakeFactory = new HandshakeFactory(mPeerRegistry);


        ExtendedHandshakeFactory extendedHandshakeFactory = new ExtendedHandshakeFactory(
                mTorrentRegistry,
                messageTypeMapping,
                acceptorPort);

        ConnectionHandlerFactory connectionHandlerFactory =
                provideConnectionHandlerFactory(handshakeFactory, mTorrentRegistry,
                        boundHandshakeHandlers, extendedHandshakeFactory);


        PeerConnectionFactory peerConnectionFactory = providePeerConnectionFactory(
                mSelector,
                connectionHandlerFactory,
                bittorrentProtocol,
                mTorrentRegistry,
                channelPipelineFactory,
                bufferManager,
                dataReceiver,
                mEventBus
        );

        mConnectionPool = new PeerConnectionPool(mEventBus, mRuntimeLifecycleBinder);


        Set<SocketChannelConnectionAcceptor> connectionAcceptors = new HashSet<>();
        connectionAcceptors.add(
                provideSocketChannelConnectionAcceptor(mSelector,
                        peerConnectionFactory, acceptorPort));


        DHTService mDHTService = new DHTService(mRuntimeLifecycleBinder, peerId,
                portMappers, mTorrentRegistry, mEventBus, acceptorPort);
        DHTHandshakeHandler dHTHandshakeHandler = new DHTHandshakeHandler(mDHTService.getPort());
        boundHandshakeHandlers.add(dHTHandshakeHandler);

        DHTPeerSourceFactory mDHTPeerSourceFactory =
                new DHTPeerSourceFactory(mRuntimeLifecycleBinder, mDHTService);
        mPeerRegistry.addPeerSourceFactory(mDHTPeerSourceFactory);


        mDataWorker = provideDataWorker(
                mRuntimeLifecycleBinder, mTorrentRegistry,
                chunkVerifier,
                new BlockCache(mTorrentRegistry, mEventBus));


        mConnectionSource = new ConnectionSource(connectionAcceptors,
                peerConnectionFactory, mConnectionPool, mRuntimeLifecycleBinder);

        this.mMessageDispatcher = new MessageDispatcher(
                mRuntimeLifecycleBinder, mConnectionPool, mTorrentRegistry);


        this.mMessagingAgents = new HashSet<>();


        this.started = new AtomicBoolean(false);
        this.lock = new Object();


    }

    private static ConnectionHandlerFactory provideConnectionHandlerFactory(
            HandshakeFactory handshakeFactory, TorrentRegistry torrentRegistry,
            Set<HandshakeHandler> boundHandshakeHandlers,
            ExtendedHandshakeFactory extendedHandshakeFactory) {

        List<HandshakeHandler> handshakeHandlers = new ArrayList<>(boundHandshakeHandlers);
        // add default handshake handlers to the beginning of the connection handling chain
        handshakeHandlers.add(new BitfieldConnectionHandler(torrentRegistry));
        handshakeHandlers.add(new ExtendedProtocolHandshakeHandler(extendedHandshakeFactory));

        return new ConnectionHandlerFactory(handshakeFactory, torrentRegistry, handshakeHandlers);
    }

    private static ExtendedMessageTypeMapping provideExtendedMessageTypeMapping(
            Map<String, MessageHandler<? extends ExtendedMessage>> handlersByTypeName) {
        return new AlphaSortedMapping(handlersByTypeName);
    }

    private static JavaSecurityDigester provideDigester() {
        int step = 2 << 22; // 8 MB
        return new JavaSecurityDigester("SHA-1", step);
    }

    private static ChunkVerifier provideVerifier(EventBus eventBus, JavaSecurityDigester digester) {
        return new ChunkVerifier(eventBus, digester);
    }

    private static DataDescriptorFactory provideDataDescriptorFactory(ChunkVerifier verifier) {
        return new DataDescriptorFactory(verifier);
    }

    private static DataWorker provideDataWorker(
            RuntimeLifecycleBinder lifecycleBinder,
            TorrentRegistry torrentRegistry,
            ChunkVerifier verifier,
            BlockCache blockCache) {
        return new DefaultDataWorker(lifecycleBinder, torrentRegistry, verifier, blockCache);
    }

    public static EventBus provideEventBus() {
        return new EventBus();
    }

    private static SharedSelector provideSelector(RuntimeLifecycleBinder lifecycleBinder) {
        SharedSelector selector;
        try {
            selector = new SharedSelector(Selector.open());
        } catch (IOException e) {
            throw new RuntimeException("Failed to get I/O selector", e);
        }

        Runnable shutdownRoutine = () -> {
            try {
                selector.close();
            } catch (IOException e) {
                throw new RuntimeException("Failed to close selector", e);
            }
        };
        lifecycleBinder.addBinding(RuntimeLifecycleBinder.LifecycleEvent.SHUTDOWN,
                LifecycleBinding.bind(shutdownRoutine).description("Shutdown selector").build());

        return selector;
    }

    private static PeerConnectionFactory providePeerConnectionFactory(
            SharedSelector selector,
            ConnectionHandlerFactory connectionHandlerFactory,
            MessageHandler<Message> bittorrentProtocol,
            TorrentRegistry torrentRegistry,
            ChannelPipelineFactory channelPipelineFactory,
            IBufferManager bufferManager,
            DataReceiver dataReceiver,
            EventSource eventSource) {
        return new PeerConnectionFactory(selector, connectionHandlerFactory, channelPipelineFactory,
                bittorrentProtocol, torrentRegistry, bufferManager, dataReceiver, eventSource);
    }

    private static SocketChannelConnectionAcceptor provideSocketChannelConnectionAcceptor(
            SharedSelector selector,
            PeerConnectionFactory connectionFactory,
            int acceptorPort) {
        InetSocketAddress localAddress = new InetSocketAddress(
                Settings.acceptorAddress, acceptorPort);
        return new SocketChannelConnectionAcceptor(selector, connectionFactory, localAddress);
    }


    public ExecutorService getExecutor() {
        return mExecutor;
    }

    public EventBus getEventBus() {
        return mEventBus;
    }


    /**
     * @return true if this runtime is up and running
     * @since 1.0
     */
    public boolean isRunning() {
        return started.get();
    }

    public void startup() {
        if (started.compareAndSet(false, true)) {
            synchronized (lock) {
                runHooks(RuntimeLifecycleBinder.LifecycleEvent.STARTUP, e -> LogUtils.error(TAG, "Error on runtime startup", e));
            }
        }
    }


    public void attachClient(Client client) {
        knownClients.add(client);
    }

    public void detachClient(Client client) {
        if (knownClients.remove(client)) {
            if (knownClients.isEmpty()) {
                shutdown();
            }
        } else {
            throw new IllegalArgumentException("Unknown client: " + client);
        }
    }


    /**
     * Manually initiate the runtime shutdown procedure, which includes:
     * - stopping all attached clients
     * - stopping all workers and executors, that were created inside this runtime
     *
     * @since 1.0
     */
    private void shutdown() {
        if (started.compareAndSet(true, false)) {
            synchronized (lock) {
                knownClients.forEach(client -> {
                    try {
                        client.stop();
                    } catch (Throwable e) {
                        LogUtils.error(TAG, "Error when stopping client", e);
                    }
                });

                runHooks(RuntimeLifecycleBinder.LifecycleEvent.SHUTDOWN, this::onShutdownHookError);
                mExecutor.shutdownNow();
            }
        }
    }

    private void runHooks(RuntimeLifecycleBinder.LifecycleEvent event, Consumer<Throwable> errorConsumer) {
        ExecutorService executor = createLifecycleExecutor(event);

        Map<LifecycleBinding, CompletableFuture<Void>> futures = new HashMap<>();
        List<LifecycleBinding> syncBindings = new ArrayList<>();

        mRuntimeLifecycleBinder.visitBindings(
                event,
                binding -> {
                    if (binding.isAsync()) {
                        futures.put(binding, CompletableFuture.runAsync(toRunnable(binding), executor));
                    } else {
                        syncBindings.add(binding);
                    }
                });

        syncBindings.forEach(binding -> {
            String errorMessage = createErrorMessage(event, binding);
            try {
                toRunnable(binding).run();
            } catch (Throwable e) {
                errorConsumer.accept(new RuntimeException(errorMessage, e));
            }
        });

        // if the app is shutting down, then we must wait for the futures to complete
        if (event == RuntimeLifecycleBinder.LifecycleEvent.SHUTDOWN) {
            futures.forEach((binding, future) -> {
                String errorMessage = createErrorMessage(event, binding);
                try {
                    future.get(Settings.shutdownHookTimeout.toMillis(), TimeUnit.MILLISECONDS);
                } catch (InterruptedException | ExecutionException | TimeoutException e) {
                    errorConsumer.accept(new RuntimeException(errorMessage, e));
                }
            });
        }

        shutdownGracefully(executor);
    }

    private String createErrorMessage(RuntimeLifecycleBinder.LifecycleEvent event, LifecycleBinding binding) {
        Optional<String> descriptionOptional = binding.getDescription();
        String errorMessage = "Failed to execute " + event.name().toLowerCase() + " hook: ";
        errorMessage += ": " + (descriptionOptional.orElseGet(() -> binding.getRunnable().toString()));
        return errorMessage;
    }

    private ExecutorService createLifecycleExecutor(RuntimeLifecycleBinder.LifecycleEvent event) {
        AtomicInteger threadCount = new AtomicInteger();
        return Executors.newCachedThreadPool(r -> {
            Thread t = new Thread(r, "bt.runtime." + event.name().toLowerCase() + "-worker-" + threadCount.incrementAndGet());
            t.setDaemon(true);
            return t;
        });
    }

    private void shutdownGracefully(ExecutorService executor) {
        executor.shutdown();
        try {
            long timeout = Settings.shutdownHookTimeout.toMillis();
            boolean terminated = executor.awaitTermination(timeout, TimeUnit.MILLISECONDS);
            if (!terminated) {
                LogUtils.warning(TAG, "Failed to shutdown executor in {} millis");
            }
        } catch (InterruptedException e) {
            // ignore
            executor.shutdownNow();
        }
    }

    private Runnable toRunnable(LifecycleBinding binding) {
        return () -> {
            Runnable r = binding.getRunnable();

            Optional<String> descriptionOptional = binding.getDescription();
            descriptionOptional.orElseGet(r::toString);

            r.run();
        };
    }

    private void onShutdownHookError(Throwable e) {
        LogUtils.error(TAG, e);
    }
}
