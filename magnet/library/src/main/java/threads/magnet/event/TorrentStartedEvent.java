package threads.magnet.event;

import androidx.annotation.NonNull;

import threads.magnet.metainfo.TorrentId;

public class TorrentStartedEvent extends BaseEvent {

    private final TorrentId torrentId;

    TorrentStartedEvent(long id, long timestamp, TorrentId torrentId) {
        super(id, timestamp);
        this.torrentId = torrentId;
    }


    public TorrentId getTorrentId() {
        return torrentId;
    }

    @NonNull
    @Override
    public String toString() {
        return "[" + this.getClass().getSimpleName() + "]  threads.torrent {" + torrentId + "}";
    }
}
