package threads.magnet.protocol.handler;

import static threads.magnet.protocol.Protocols.verifyPayloadHasLength;

import java.nio.ByteBuffer;

import threads.magnet.net.buffer.ByteBufferView;
import threads.magnet.protocol.DecodingContext;
import threads.magnet.protocol.EncodingContext;
import threads.magnet.protocol.NotInterested;

public final class NotInterestedHandler extends UniqueMessageHandler<NotInterested> {

    public NotInterestedHandler() {
        super(NotInterested.class);
    }

    @Override
    public int doDecode(DecodingContext context, ByteBufferView buffer) {
        verifyPayloadHasLength(NotInterested.class, 0, buffer.remaining());
        context.setMessage(NotInterested.instance());
        return 0;
    }

    @Override
    public boolean doEncode(EncodingContext context, NotInterested message, ByteBuffer buffer) {
        return true;
    }
}
