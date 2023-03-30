package threads.magnet.protocol;

import threads.magnet.metainfo.TorrentId;
import threads.magnet.peer.PeerRegistry;


public final class HandshakeFactory {

    private static final int HANDSHAKE_RESERVED_LENGTH = 8;

    private final PeerRegistry peerRegistry; // TODO: workaround for circular DI deps, maybe get rid of this completely?


    public HandshakeFactory(PeerRegistry peerRegistry) {
        this.peerRegistry = peerRegistry;
    }

    public Handshake createHandshake(TorrentId torrentId) {
        try {
            return new Handshake(new byte[HANDSHAKE_RESERVED_LENGTH], torrentId,
                    peerRegistry.getLocalPeer().getPeerId().orElseThrow(() -> new RuntimeException("Local peer is missing ID")));
        } catch (InvalidMessageException e) {
            throw new RuntimeException("Failed to create handshake", e);
        }
    }
}
