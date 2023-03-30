package threads.magnet.processor;

import threads.magnet.event.EventSink;
import threads.magnet.metainfo.TorrentId;
import threads.magnet.torrent.TorrentDescriptor;
import threads.magnet.torrent.TorrentRegistry;

public class ProcessTorrentStage extends TerminateOnErrorProcessingStage {

    private final TorrentRegistry torrentRegistry;

    private final EventSink eventSink;

    public ProcessTorrentStage(ProcessingStage next,
                               TorrentRegistry torrentRegistry,
                               EventSink eventSink) {
        super(next);
        this.torrentRegistry = torrentRegistry;
        this.eventSink = eventSink;
    }

    @Override
    protected void doExecute(MagnetContext context) {
        TorrentId torrentId = context.getTorrentId();
        TorrentDescriptor descriptor = getDescriptor(torrentId);

        descriptor.start();

        eventSink.fireTorrentStarted(torrentId);

        while (descriptor.isActive()) {
            if (context.getState().getPiecesRemaining() == 0) {
                break;
            }
        }
    }


    private TorrentDescriptor getDescriptor(TorrentId torrentId) {
        return torrentRegistry.getDescriptor(torrentId)
                .orElseThrow(() -> new IllegalStateException("No descriptor present for threads.torrent ID: " + torrentId));
    }

    @Override
    public ProcessingEvent after() {
        return ProcessingEvent.DOWNLOAD_COMPLETE;
    }
}
