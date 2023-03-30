package threads.magnet.processor;

import java.util.Set;
import java.util.function.Supplier;

import threads.magnet.IAgent;
import threads.magnet.data.Bitfield;
import threads.magnet.event.EventSource;
import threads.magnet.metainfo.TorrentId;
import threads.magnet.net.ConnectionSource;
import threads.magnet.net.MessageDispatcher;
import threads.magnet.torrent.Assignments;
import threads.magnet.torrent.DefaultMessageRouter;
import threads.magnet.torrent.MessageRouter;
import threads.magnet.torrent.PeerWorkerFactory;
import threads.magnet.torrent.PieceStatistics;
import threads.magnet.torrent.TorrentDescriptor;
import threads.magnet.torrent.TorrentRegistry;
import threads.magnet.torrent.TorrentSessionState;
import threads.magnet.torrent.TorrentWorker;

public class CreateSessionStage extends TerminateOnErrorProcessingStage {

    private final TorrentRegistry torrentRegistry;
    private final EventSource eventSource;
    private final ConnectionSource connectionSource;
    private final MessageDispatcher messageDispatcher;
    private final Set<IAgent> messagingAgents;


    public CreateSessionStage(ProcessingStage next,
                              TorrentRegistry torrentRegistry,
                              EventSource eventSource,
                              ConnectionSource connectionSource,
                              MessageDispatcher messageDispatcher,
                              Set<IAgent> messagingAgents) {
        super(next);
        this.torrentRegistry = torrentRegistry;
        this.eventSource = eventSource;
        this.connectionSource = connectionSource;
        this.messageDispatcher = messageDispatcher;
        this.messagingAgents = messagingAgents;
    }

    @Override
    protected void doExecute(MagnetContext context) {
        TorrentId torrentId = context.getTorrentId();
        TorrentDescriptor descriptor = torrentRegistry.register(torrentId);

        MessageRouter router = new DefaultMessageRouter(messagingAgents);
        PeerWorkerFactory peerWorkerFactory = new PeerWorkerFactory(router);

        Supplier<Bitfield> bitfieldSupplier = context::getBitfield;
        Supplier<Assignments> assignmentsSupplier = context::getAssignments;
        Supplier<PieceStatistics> statisticsSupplier = context::getPieceStatistics;


        new TorrentWorker(torrentId, messageDispatcher,
                connectionSource, peerWorkerFactory,
                bitfieldSupplier, assignmentsSupplier, statisticsSupplier, eventSource);

        context.setState(new TorrentSessionState(descriptor));
        context.setRouter(router);
    }

    @Override
    public ProcessingEvent after() {
        return null;
    }
}
