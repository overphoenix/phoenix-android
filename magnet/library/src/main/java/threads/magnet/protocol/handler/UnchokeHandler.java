package threads.magnet.protocol.handler;

import static threads.magnet.protocol.Protocols.verifyPayloadHasLength;

import java.nio.ByteBuffer;

import threads.magnet.net.buffer.ByteBufferView;
import threads.magnet.protocol.DecodingContext;
import threads.magnet.protocol.EncodingContext;
import threads.magnet.protocol.Unchoke;

public final class UnchokeHandler extends UniqueMessageHandler<Unchoke> {

    public UnchokeHandler() {
        super(Unchoke.class);
    }

    @Override
    public int doDecode(DecodingContext context, ByteBufferView buffer) {
        verifyPayloadHasLength(Unchoke.class, 0, buffer.remaining());
        context.setMessage(Unchoke.instance());
        return 0;
    }

    @Override
    public boolean doEncode(EncodingContext context, Unchoke message, ByteBuffer buffer) {
        return true;
    }
}
