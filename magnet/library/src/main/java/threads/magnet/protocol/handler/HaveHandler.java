package threads.magnet.protocol.handler;

import static threads.magnet.protocol.Protocols.readInt;
import static threads.magnet.protocol.Protocols.verifyPayloadHasLength;

import java.nio.ByteBuffer;
import java.util.Objects;

import threads.magnet.net.buffer.ByteBufferView;
import threads.magnet.protocol.DecodingContext;
import threads.magnet.protocol.EncodingContext;
import threads.magnet.protocol.Have;
import threads.magnet.protocol.InvalidMessageException;

public final class HaveHandler extends UniqueMessageHandler<Have> {

    public HaveHandler() {
        super(Have.class);
    }

    // have: <len=0005><id=4><piece index>
    private static boolean writeHave(int pieceIndex, ByteBuffer buffer) {
        if (pieceIndex < 0) {
            throw new InvalidMessageException("Invalid piece index: " + pieceIndex);
        }
        if (buffer.remaining() < Integer.BYTES) {
            return false;
        }

        buffer.putInt(pieceIndex);
        return true;
    }

    private static int decodeHave(DecodingContext context, ByteBufferView buffer) {

        int consumed = 0;
        int length = Integer.BYTES;

        if (buffer.remaining() >= length) {
            Integer pieceIndex = Objects.requireNonNull(readInt(buffer));
            context.setMessage(new Have(pieceIndex));
            consumed = length;
        }

        return consumed;
    }

    @Override
    public int doDecode(DecodingContext context, ByteBufferView buffer) {
        verifyPayloadHasLength(Have.class, 4, buffer.remaining());
        return decodeHave(context, buffer);
    }

    @Override
    public boolean doEncode(EncodingContext context, Have message, ByteBuffer buffer) {
        return writeHave(message.getPieceIndex(), buffer);
    }
}
