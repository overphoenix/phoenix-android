package threads.magnet.net.extended;

import java.util.ArrayList;
import java.util.List;

import threads.magnet.IConsumers;
import threads.magnet.bencoding.BEInteger;
import threads.magnet.net.InetPeer;
import threads.magnet.net.PeerConnectionPool;
import threads.magnet.protocol.Message;
import threads.magnet.protocol.extended.ExtendedHandshake;
import threads.magnet.torrent.MessageConsumer;
import threads.magnet.torrent.MessageContext;

public class ExtendedHandshakeConsumer implements IConsumers {

    private final PeerConnectionPool connectionPool;

    public ExtendedHandshakeConsumer(PeerConnectionPool connectionPool) {
        this.connectionPool = connectionPool;
    }

    public void doConsume(Message message, MessageContext messageContext) {
        if (message instanceof ExtendedHandshake) {
            consume((ExtendedHandshake) message, messageContext);
        }
    }

    @Override
    public List<MessageConsumer<? extends Message>> getConsumers() {
        List<MessageConsumer<? extends Message>> list = new ArrayList<>();
        list.add(new MessageConsumer<ExtendedHandshake>() {
            @Override
            public Class<ExtendedHandshake> getConsumedType() {
                return ExtendedHandshake.class;
            }

            @Override
            public void consume(ExtendedHandshake message, MessageContext context) {
                doConsume(message, context);
            }
        });
        return list;
    }

    private void consume(ExtendedHandshake message, MessageContext messageContext) {
        BEInteger peerListeningPort = message.getPort();
        if (peerListeningPort != null) {
            InetPeer peer = (InetPeer) messageContext.getConnectionKey().getPeer();
            int listeningPort = peerListeningPort.getValue().intValue();
            peer.setPort(listeningPort);

            connectionPool.checkDuplicateConnections(messageContext.getConnectionKey().getTorrentId(), peer);
        }
    }
}
