package threads.magnet.processor;

import androidx.annotation.NonNull;

import threads.magnet.data.Storage;
import threads.magnet.magnet.MagnetUri;
import threads.magnet.metainfo.TorrentId;
import threads.magnet.torrent.BitfieldCollectingConsumer;
import threads.magnet.torrent.PieceSelector;

public class MagnetContext extends TorrentContext {

    private final MagnetUri magnetUri;
    private volatile BitfieldCollectingConsumer bitfieldConsumer;

    public MagnetContext(MagnetUri magnetUri, PieceSelector pieceSelector, Storage storage) {
        super(pieceSelector, storage);
        this.magnetUri = magnetUri;
    }

    public MagnetUri getMagnetUri() {
        return magnetUri;
    }

    @NonNull
    public TorrentId getTorrentId() {
        return magnetUri.getTorrentId();
    }


    public BitfieldCollectingConsumer getBitfieldConsumer() {
        return bitfieldConsumer;
    }

    public void setBitfieldConsumer(BitfieldCollectingConsumer bitfieldConsumer) {
        this.bitfieldConsumer = bitfieldConsumer;
    }
}
