package threads.magnet.protocol.handler;

import static threads.magnet.protocol.Protocols.readInt;

import java.nio.ByteBuffer;
import java.util.Objects;

import threads.magnet.net.buffer.ByteBufferView;
import threads.magnet.protocol.DecodingContext;
import threads.magnet.protocol.EncodingContext;
import threads.magnet.protocol.Piece;

public final class PieceHandler extends UniqueMessageHandler<Piece> {

    public PieceHandler() {
        super(Piece.class);
    }

    // piece: <len=0009+X><id=7><index><begin><block>
    private static boolean writePiece(Piece message, ByteBuffer buffer) {
        int pieceIndex = message.getPieceIndex();
        int offset = message.getOffset();
        int length = message.getLength();
        if (buffer.remaining() < Integer.BYTES * 2 + length) {
            return false;
        }

        buffer.putInt(pieceIndex);
        buffer.putInt(offset);

        return message.writeBlockTo(buffer);
    }

    private static int decodePiece(DecodingContext context, ByteBufferView buffer, int length) {

        int consumed = 0;

        if (buffer.remaining() >= length) {

            int pieceIndex = Objects.requireNonNull(readInt(buffer));
            int blockOffset = Objects.requireNonNull(readInt(buffer));
            int blockLength = length - Integer.BYTES * 2;
            buffer.position(buffer.position() + blockLength);

            context.setMessage(new Piece(pieceIndex, blockOffset, blockLength));
            consumed = length;
        }

        return consumed;
    }

    @Override
    public int doDecode(DecodingContext context, ByteBufferView buffer) {
        return decodePiece(context, buffer, buffer.remaining());
    }

    @Override
    public boolean doEncode(EncodingContext context, Piece message, ByteBuffer buffer) {
        return writePiece(message, buffer);
    }
}
