package threads.magnet.torrent;

import java.util.concurrent.CompletableFuture;

import threads.magnet.metainfo.TorrentId;
import threads.magnet.net.Peer;
import threads.magnet.net.buffer.BufferedData;

public interface DataWorker {


    CompletableFuture<BlockRead> addBlockRequest(TorrentId torrentId, Peer peer, int pieceIndex, int offset, int length);

    CompletableFuture<BlockWrite> addBlock(TorrentId torrentId, int pieceIndex, int offset, BufferedData buffer);
}
