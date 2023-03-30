package threads.magnet.magnet;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;

import threads.magnet.bencoding.BEInteger;
import threads.magnet.bencoding.BEMap;
import threads.magnet.bencoding.BEObject;
import threads.magnet.bencoding.BEParser;
import threads.magnet.net.buffer.ByteBufferView;
import threads.magnet.protocol.DecodingContext;
import threads.magnet.protocol.EncodingContext;
import threads.magnet.protocol.handler.MessageHandler;

public class UtMetadataMessageHandler implements MessageHandler<UtMetadata> {
    private final Collection<Class<? extends UtMetadata>> supportedTypes = Collections.singleton(UtMetadata.class);

    @Override
    public boolean encode(EncodingContext context, UtMetadata message, ByteBuffer buffer) {
        boolean encoded = false;
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            writeMessage(message, bos);
            byte[] payload = bos.toByteArray();
            if (buffer.remaining() >= payload.length) {
                buffer.put(payload);
                encoded = true;
            }
        } catch (IOException e) {
            // can't happen
        }
        return encoded;
    }

    private void writeMessage(UtMetadata message, OutputStream out) throws IOException {
        BEMap m = new BEMap(null, new HashMap<String, BEObject<?>>() {{
            put(UtMetadata.messageTypeField(), new BEInteger(null,
                    BigInteger.valueOf(message.getType().id())));
            put(UtMetadata.pieceIndexField(), new BEInteger(null,
                    BigInteger.valueOf(message.getPieceIndex())));
            if (message.getData().isPresent()) {
                put(UtMetadata.totalSizeField(), new BEInteger(null,
                        BigInteger.valueOf(message.getTotalSize().get())));
            }
        }});
        m.writeTo(out);
        if (message.getData().isPresent()) {
            out.write(message.getData().get());
        }
    }

    @Override
    public int decode(DecodingContext context, ByteBufferView buffer) {
        byte[] payload = new byte[buffer.remaining()];
        buffer.get(payload);
        try (BEParser parser = new BEParser(payload)) {
            BEMap m = parser.readMap();
            int length = m.getContent().length;
            UtMetadata.Type messageType = getMessageType(m);
            int pieceIndex = getPieceIndex(m);
            switch (messageType) {
                case REQUEST: {
                    context.setMessage(UtMetadata.request(pieceIndex));
                    return length;
                }
                case DATA: {
                    byte[] data = Arrays.copyOfRange(payload, length, payload.length);
                    context.setMessage(UtMetadata.data(pieceIndex, getTotalSize(m), data));
                    return payload.length;
                }
                case REJECT: {
                    context.setMessage(UtMetadata.reject(pieceIndex));
                    return length;
                }
                default: {
                    throw new IllegalStateException("Unknown message type: " + messageType.name());
                }
            }
        }
    }

    private UtMetadata.Type getMessageType(BEMap m) {
        BEInteger type = (BEInteger) m.getValue().get(UtMetadata.messageTypeField());
        int typeId = type.getValue().intValue();
        return UtMetadata.Type.forId(typeId);
    }

    private int getPieceIndex(BEMap m) {
        return getIntAttribute(UtMetadata.pieceIndexField(), m);
    }

    private int getTotalSize(BEMap m) {
        return getIntAttribute(UtMetadata.totalSizeField(), m);
    }

    private int getIntAttribute(String name, BEMap m) {
        BEInteger value = ((BEInteger) m.getValue().get(name));
        if (value == null) {
            throw new IllegalStateException("Message attribute is missing: " + name);
        }
        return value.getValue().intValue();
    }

    @Override
    public Collection<Class<? extends UtMetadata>> getSupportedTypes() {
        return supportedTypes;
    }

    @Override
    public Class<? extends UtMetadata> readMessageType(ByteBufferView buffer) {
        return UtMetadata.class;
    }
}
