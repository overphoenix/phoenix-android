package threads.magnet.protocol.handler;

import static threads.magnet.protocol.Protocols.getShortBytes;
import static threads.magnet.protocol.Protocols.verifyPayloadHasLength;

import java.nio.ByteBuffer;

import threads.magnet.net.buffer.ByteBufferView;
import threads.magnet.protocol.DecodingContext;
import threads.magnet.protocol.EncodingContext;
import threads.magnet.protocol.InvalidMessageException;
import threads.magnet.protocol.Port;
import threads.magnet.protocol.Protocols;


public final class PortMessageHandler extends UniqueMessageHandler<Port> {

    public static final int PORT_ID = 9;

    private static final int EXPECTED_PAYLOAD_LENGTH = Short.BYTES;

    public PortMessageHandler() {
        super(Port.class);
    }

    // port: <len=0003><id=9><listen-port>
    private static boolean writePort(int port, ByteBuffer buffer) {
        if (port < 0 || port > Short.MAX_VALUE * 2 + 1) {
            throw new RuntimeException("Invalid port: " + port);
        }
        if (buffer.remaining() < Short.BYTES) {
            return false;
        }

        buffer.put(getShortBytes(port));
        return true;
    }

    private static int decodePort(DecodingContext context, ByteBufferView buffer) throws InvalidMessageException {
        int consumed = 0;
        int length = Short.BYTES;

        Short s;
        if ((s = Protocols.readShort(buffer)) != null) {
            int port = s & 0x0000FFFF;
            context.setMessage(new Port(port));
            consumed = length;
        }

        return consumed;
    }

    @Override
    public int doDecode(DecodingContext context, ByteBufferView buffer) {
        verifyPayloadHasLength(Port.class, EXPECTED_PAYLOAD_LENGTH, buffer.remaining());
        return decodePort(context, buffer);
    }

    @Override
    public boolean doEncode(EncodingContext context, Port message, ByteBuffer buffer) {
        return writePort(message.getPort(), buffer);
    }
}
