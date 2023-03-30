package threads.magnet.processor;

import threads.magnet.event.EventSink;
import threads.magnet.torrent.TorrentDescriptor;
import threads.magnet.torrent.TorrentRegistry;

public class TorrentContextFinalizer implements ContextFinalizer {

    private final TorrentRegistry torrentRegistry;
    private final EventSink eventSink;

    public TorrentContextFinalizer(TorrentRegistry torrentRegistry, EventSink eventSink) {
        this.torrentRegistry = torrentRegistry;
        this.eventSink = eventSink;
    }

    @Override
    public void finalizeContext(MagnetContext context) {
        torrentRegistry.getDescriptor(context.getTorrentId()).ifPresent(TorrentDescriptor::stop);
        eventSink.fireTorrentStopped(context.getTorrentId());
    }
}
