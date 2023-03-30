package threads.magnet.torrent;

import threads.magnet.net.ConnectionKey;

public class PeerWorkerFactory {

    private final MessageRouter router;

    public PeerWorkerFactory(MessageRouter router) {
        this.router = router;
    }

    public PeerWorker createPeerWorker(ConnectionKey connectionKey) {
        return new RoutingPeerWorker(connectionKey, router);
    }
}
