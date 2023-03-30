package threads.magnet.processor;

import java.util.Objects;

import threads.magnet.data.Bitfield;
import threads.magnet.event.EventSink;
import threads.magnet.metainfo.Torrent;
import threads.magnet.metainfo.TorrentId;
import threads.magnet.net.PeerConnectionPool;
import threads.magnet.net.extended.ExtendedHandshakeConsumer;
import threads.magnet.net.pipeline.BufferedPieceRegistry;
import threads.magnet.torrent.BitfieldConsumer;
import threads.magnet.torrent.DataWorker;
import threads.magnet.torrent.GenericConsumer;
import threads.magnet.torrent.MetadataProducer;
import threads.magnet.torrent.PeerRequestConsumer;
import threads.magnet.torrent.PieceConsumer;
import threads.magnet.torrent.PieceStatistics;
import threads.magnet.torrent.RequestProducer;
import threads.magnet.torrent.TorrentDescriptor;
import threads.magnet.torrent.TorrentRegistry;

public class InitializeTorrentProcessingStage extends TerminateOnErrorProcessingStage {

    private final PeerConnectionPool connectionPool;
    private final TorrentRegistry torrentRegistry;
    private final DataWorker dataWorker;
    private final BufferedPieceRegistry bufferedPieceRegistry;
    private final EventSink eventSink;


    public InitializeTorrentProcessingStage(ProcessingStage next,
                                            PeerConnectionPool connectionPool,
                                            TorrentRegistry torrentRegistry,
                                            DataWorker dataWorker,
                                            BufferedPieceRegistry bufferedPieceRegistry,
                                            EventSink eventSink) {
        super(next);
        this.connectionPool = connectionPool;
        this.torrentRegistry = torrentRegistry;
        this.dataWorker = dataWorker;
        this.bufferedPieceRegistry = bufferedPieceRegistry;
        this.eventSink = eventSink;
    }

    @Override
    protected void doExecute(MagnetContext context) {
        Torrent torrent = context.getTorrent();
        Objects.requireNonNull(torrent);
        TorrentDescriptor descriptor = torrentRegistry.register(torrent, context.getStorage());

        TorrentId torrentId = torrent.getTorrentId();
        Bitfield bitfield = descriptor.getDataDescriptor().getBitfield();
        PieceStatistics pieceStatistics = createPieceStatistics(bitfield);

        context.getRouter().registerMessagingAgent(GenericConsumer.consumer());
        context.getRouter().registerMessagingAgent(new BitfieldConsumer(bitfield, pieceStatistics, eventSink));
        context.getRouter().registerMessagingAgent(new ExtendedHandshakeConsumer(connectionPool));
        context.getRouter().registerMessagingAgent(new PieceConsumer(torrentId, bitfield, dataWorker, bufferedPieceRegistry, eventSink));
        context.getRouter().registerMessagingAgent(new PeerRequestConsumer(torrentId, dataWorker));
        context.getRouter().registerMessagingAgent(new RequestProducer(descriptor.getDataDescriptor()));
        context.getRouter().registerMessagingAgent(new MetadataProducer(context.getTorrent()));

        context.setBitfield(bitfield);
        context.setPieceStatistics(pieceStatistics);
    }

    private PieceStatistics createPieceStatistics(Bitfield bitfield) {
        return new PieceStatistics(bitfield);
    }

    @Override
    public ProcessingEvent after() {
        return null;
    }
}
