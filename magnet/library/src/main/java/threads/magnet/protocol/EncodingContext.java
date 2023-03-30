package threads.magnet.protocol;

import threads.magnet.net.Peer;

public class EncodingContext {

    private final Peer peer;

    public EncodingContext(Peer peer) {
        this.peer = peer;
    }

    public Peer getPeer() {
        return peer;
    }
}
