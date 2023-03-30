package threads.magnet.net.pipeline;

import java.nio.ByteBuffer;

import threads.magnet.net.Peer;
import threads.magnet.protocol.EncodingContext;
import threads.magnet.protocol.Message;
import threads.magnet.protocol.handler.MessageHandler;

class MessageSerializer {

    private final EncodingContext context;
    private final MessageHandler<Message> protocol;

    public MessageSerializer(Peer peer,
                             MessageHandler<Message> protocol) {
        this.context = new EncodingContext(peer);
        this.protocol = protocol;
    }

    public boolean serialize(Message message, ByteBuffer buffer) {
        return protocol.encode(context, message, buffer);
    }
}
