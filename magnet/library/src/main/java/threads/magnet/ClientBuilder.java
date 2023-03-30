package threads.magnet;

import java.util.Objects;

import threads.magnet.data.Storage;
import threads.magnet.magnet.MagnetUri;
import threads.magnet.magnet.MagnetUriParser;
import threads.magnet.processor.ListenerSource;
import threads.magnet.processor.MagnetContext;
import threads.magnet.processor.ProcessingEvent;
import threads.magnet.processor.TorrentProcessorFactory;
import threads.magnet.torrent.PieceSelector;
import threads.magnet.torrent.RarestFirstSelector;


public class ClientBuilder {

    private final PieceSelector pieceSelector;
    private Storage storage;
    private MagnetUri magnetUri;
    private Runtime runtime;

    public ClientBuilder() {
        this.pieceSelector = RarestFirstSelector.randomizedRarest();
    }


    public ClientBuilder storage(Storage storage) {
        this.storage = Objects.requireNonNull(storage, "Missing data storage");
        return this;
    }


    public ClientBuilder magnet(String magnetUri) {
        this.magnetUri = MagnetUriParser.lenientParser().parse(magnetUri);
        return this;
    }

    public ClientBuilder runtime(Runtime runtime) {
        this.runtime = Objects.requireNonNull(runtime, "Missing runtime");
        return this;
    }


    public Client build() {
        Objects.requireNonNull(runtime, "Missing runtime");
        ListenerSource listenerSource = new ListenerSource();
        listenerSource.addListener(ProcessingEvent.DOWNLOAD_COMPLETE, (context, next) -> null);

        return new Client(runtime,
                TorrentProcessorFactory.createMagnetProcessor(runtime),
                new MagnetContext(magnetUri, pieceSelector, storage), listenerSource);
    }


}
