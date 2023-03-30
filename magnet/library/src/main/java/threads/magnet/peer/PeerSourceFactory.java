package threads.magnet.peer;

import threads.magnet.metainfo.TorrentId;

public interface PeerSourceFactory {


    PeerSource getPeerSource(TorrentId torrentId);
}
