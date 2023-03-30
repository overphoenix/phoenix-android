package threads.magnet.net;

import java.io.IOException;
import java.util.Optional;

import threads.magnet.data.Bitfield;
import threads.magnet.protocol.BitOrder;
import threads.magnet.protocol.Handshake;
import threads.magnet.torrent.TorrentDescriptor;
import threads.magnet.torrent.TorrentRegistry;

public class BitfieldConnectionHandler implements HandshakeHandler {

    private final TorrentRegistry torrentRegistry;


    public BitfieldConnectionHandler(TorrentRegistry torrentRegistry) {
        this.torrentRegistry = torrentRegistry;
    }

    @Override
    public void processIncomingHandshake(PeerConnection connection, Handshake peerHandshake) {
        Optional<TorrentDescriptor> descriptorOptional = torrentRegistry.getDescriptor(connection.getTorrentId());
        if (descriptorOptional.isPresent() && descriptorOptional.get().isActive()
                && descriptorOptional.get().getDataDescriptor() != null) {
            Bitfield bitfield = descriptorOptional.get().getDataDescriptor().getBitfield();

            if (bitfield.getPiecesComplete() > 0) {
                Peer peer = connection.getRemotePeer();
                threads.magnet.protocol.Bitfield bitfieldMessage = new threads.magnet.protocol.Bitfield(bitfield.toByteArray(BitOrder.LITTLE_ENDIAN));
                try {
                    connection.postMessage(bitfieldMessage);
                } catch (IOException e) {
                    throw new RuntimeException("Failed to send bitfield to peer: " + peer, e);
                }
            }
        }
    }

    @Override
    public void processOutgoingHandshake(Handshake handshake) {
        // do nothing
    }
}
