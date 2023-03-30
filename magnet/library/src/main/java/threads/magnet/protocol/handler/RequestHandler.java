package threads.magnet.protocol.handler;

import static threads.magnet.protocol.Protocols.readInt;
import static threads.magnet.protocol.Protocols.verifyPayloadHasLength;

import java.nio.ByteBuffer;
import java.util.Objects;

import threads.magnet.net.buffer.ByteBufferView;
import threads.magnet.protocol.DecodingContext;
import threads.magnet.protocol.EncodingContext;
import threads.magnet.protocol.InvalidMessageException;
import threads.magnet.protocol.Request;

public final class RequestHandler extends UniqueMessageHandler<Request> {

    public RequestHandler() {
        super(Request.class);
    }

    // request: <len=0013><id=6><index><begin><length>
    private static boolean writeRequest(int pieceIndex, int offset, int length, ByteBuffer buffer) {

        if (pieceIndex < 0 || offset < 0 || length <= 0) {
            throw new InvalidMessageException("Invalid arguments: pieceIndex (" + pieceIndex
                    + "), offset (" + offset + "), length (" + length + ")");
        }
        if (buffer.remaining() < Integer.BYTES * 3) {
            return false;
        }

        buffer.putInt(pieceIndex);
        buffer.putInt(offset);
        buffer.putInt(length);

        return true;
    }

    private static int decodeRequest(DecodingContext context, ByteBufferView buffer) {

        int consumed = 0;
        int length = Integer.BYTES * 3;

        if (buffer.remaining() >= length) {

            int pieceIndex = Objects.requireNonNull(readInt(buffer));
            int blockOffset = Objects.requireNonNull(readInt(buffer));
            int blockLength = Objects.requireNonNull(readInt(buffer));

            context.setMessage(new Request(pieceIndex, blockOffset, blockLength));
            consumed = length;
        }

        return consumed;
    }

    @Override
    public int doDecode(DecodingContext context, ByteBufferView buffer) {
        verifyPayloadHasLength(Request.class, 12, buffer.remaining());
        return decodeRequest(context, buffer);
    }

    @Override
    public boolean doEncode(EncodingContext context, Request message, ByteBuffer buffer) {
        return writeRequest(message.getPieceIndex(), message.getOffset(), message.getLength(), buffer);
    }
}
