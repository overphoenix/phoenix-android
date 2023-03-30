package threads.magnet.net;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import threads.magnet.Settings;
import threads.magnet.metainfo.TorrentId;
import threads.magnet.protocol.HandshakeFactory;
import threads.magnet.torrent.TorrentRegistry;

public class ConnectionHandlerFactory {

    private final HandshakeFactory handshakeFactory;
    private final ConnectionHandler incomingHandler;


    private final Collection<HandshakeHandler> handshakeHandlers;

    private final Map<TorrentId, ConnectionHandler> outgoingHandlers;

    public ConnectionHandlerFactory(HandshakeFactory handshakeFactory,
                                    TorrentRegistry torrentRegistry,
                                    Collection<HandshakeHandler> handshakeHandlers) {
        this.handshakeFactory = handshakeFactory;
        this.incomingHandler = new IncomingHandshakeHandler(handshakeFactory, torrentRegistry,
                handshakeHandlers);

        this.outgoingHandlers = new ConcurrentHashMap<>();
        this.handshakeHandlers = handshakeHandlers;

    }

    public ConnectionHandler getIncomingHandler() {
        return incomingHandler;
    }

    public ConnectionHandler getOutgoingHandler(TorrentId torrentId) {
        Objects.requireNonNull(torrentId, "Missing threads.torrent ID");
        ConnectionHandler outgoing = outgoingHandlers.get(torrentId);
        if (outgoing == null) {
            outgoing = new OutgoingHandshakeHandler(handshakeFactory, torrentId,
                    handshakeHandlers, Settings.peerHandshakeTimeout.toMillis());
            ConnectionHandler existing = outgoingHandlers.putIfAbsent(torrentId, outgoing);
            if (existing != null) {
                outgoing = existing;
            }
        }
        return outgoing;
    }
}
