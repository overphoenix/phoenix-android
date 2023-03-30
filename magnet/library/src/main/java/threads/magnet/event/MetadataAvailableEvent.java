package threads.magnet.event;

import androidx.annotation.NonNull;

import threads.magnet.metainfo.TorrentId;

public class MetadataAvailableEvent extends BaseEvent {

    private final TorrentId torrentId;

    MetadataAvailableEvent(long id, long timestamp, TorrentId torrentId) {
        super(id, timestamp);
        this.torrentId = torrentId;
    }

    @NonNull
    @Override
    public String toString() {
        return "[" + this.getClass().getSimpleName() + "]  threads.torrent {" + torrentId + "}";
    }
}
