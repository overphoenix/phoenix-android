package threads.magnet.processor;

import threads.magnet.event.EventSink;
import threads.magnet.torrent.TorrentRegistry;

public class ProcessMagnetTorrentStage extends ProcessTorrentStage {

    public ProcessMagnetTorrentStage(ProcessingStage next,
                                     TorrentRegistry torrentRegistry,
                                     EventSink eventSink) {
        super(next, torrentRegistry, eventSink);
    }
}
