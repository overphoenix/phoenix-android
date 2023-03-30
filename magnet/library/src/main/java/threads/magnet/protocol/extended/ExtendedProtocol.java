package threads.magnet.protocol.extended;

import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import threads.magnet.net.buffer.ByteBufferView;
import threads.magnet.protocol.DecodingContext;
import threads.magnet.protocol.EncodingContext;
import threads.magnet.protocol.InvalidMessageException;
import threads.magnet.protocol.Message;
import threads.magnet.protocol.handler.BaseMessageHandler;
import threads.magnet.protocol.handler.MessageHandler;

public class ExtendedProtocol extends BaseMessageHandler<ExtendedMessage> {

    /**
     * Unique message type ID for all extended message types.
     * It must be present in the encoded representation of the message
     * in order for it to be passed to the {@link ExtendedProtocol}
     * for further processing.
     *
     * @since 1.0
     */
    public static final int EXTENDED_MESSAGE_ID = 20;

    private static final int HANDSHAKE_TYPE_ID = 0;

    private final ExtendedHandshakeMessageHandler extendedHandshakeHandler;

    private final Map<Class<? extends ExtendedMessage>, MessageHandler<? extends ExtendedMessage>> handlers;
    private final Map<String, Class<? extends ExtendedMessage>> uniqueTypes;
    private final Map<String, MessageHandler<? extends ExtendedMessage>> handlersByTypeName;

    private final ExtendedMessageTypeMapping messageTypeMapping;

    public ExtendedProtocol(ExtendedMessageTypeMapping messageTypeMapping,
                            Map<String, MessageHandler<? extends ExtendedMessage>> handlersByTypeName) {

        this.messageTypeMapping = messageTypeMapping;

        Map<Class<? extends ExtendedMessage>, MessageHandler<? extends ExtendedMessage>> handlers = new HashMap<>();
        extendedHandshakeHandler = new ExtendedHandshakeMessageHandler();
        handlers.put(ExtendedHandshake.class, extendedHandshakeHandler);

        Map<String, Class<? extends ExtendedMessage>> uniqueTypes = new HashMap<>();
        handlersByTypeName.forEach((typeName, handler) -> {

            if (handler.getSupportedTypes().isEmpty()) {
                throw new RuntimeException("No supported types declared in handler: " + handler.getClass().getName());
            } else {
                uniqueTypes.put(typeName, handler.getSupportedTypes().iterator().next());
            }

            handler.getSupportedTypes().forEach(messageType -> {
                if (handlers.containsKey(messageType)) {
                    throw new RuntimeException("Encountered duplicate handler for message type: " + messageType.getSimpleName());
                }
                handlers.put(messageType, handler);
            });
        });

        this.handlers = Collections.unmodifiableMap(handlers);
        this.handlersByTypeName = handlersByTypeName;
        this.uniqueTypes = uniqueTypes;
    }

    @Override
    public Collection<Class<? extends ExtendedMessage>> getSupportedTypes() {
        return handlers.keySet();
    }

    @Override
    public Class<? extends ExtendedMessage> readMessageType(ByteBufferView buffer) {
        if (!buffer.hasRemaining()) {
            return null;
        }
        int messageTypeId = buffer.get();
        if (messageTypeId == HANDSHAKE_TYPE_ID) {
            return ExtendedHandshake.class;
        }

        Class<? extends ExtendedMessage> messageType;
        String typeName = messageTypeMapping.getTypeNameForId(messageTypeId);
        if (typeName == null) {
            throw new InvalidMessageException("Unknown message type ID: " + messageTypeId);
        }
        messageType = uniqueTypes.get(typeName);
        if (messageType == null) {
            messageType = handlersByTypeName.get(typeName).readMessageType(buffer);
        }
        return messageType;
    }

    @Override
    public int doDecode(DecodingContext context, ByteBufferView buffer) {

        int typeId = buffer.get();
        MessageHandler<?> handler;
        if (typeId == HANDSHAKE_TYPE_ID) {
            handler = extendedHandshakeHandler;
        } else {
            String extendedType = messageTypeMapping.getTypeNameForId(typeId);
            if (extendedType == null) {
                throw new RuntimeException("Received unsupported extended message id: " + typeId);
            }
            handler = handlersByTypeName.get(extendedType);
        }

        int consumed = handler.decode(context, buffer);
        if (consumed > 0) {
            consumed += 1; // type ID was trimmed when passing data to handler
        }
        return consumed;
    }

    @Override
    public boolean doEncode(EncodingContext context, ExtendedMessage message, ByteBuffer buffer) {
        Class<? extends Message> messageType = message.getClass();
        return doEncode(context, message, messageType, buffer);
    }

    @SuppressWarnings("unchecked")
    private <T extends Message> boolean doEncode(EncodingContext context, Message message, Class<T> messageType, ByteBuffer buffer) {

        if (!buffer.hasRemaining()) {
            return false;
        }

        int begin = buffer.position();
        if (ExtendedHandshake.class.equals(messageType)) {
            buffer.put((byte) HANDSHAKE_TYPE_ID);
        } else {
            String typeName = messageTypeMapping.getTypeNameForJavaType(messageType);
            if (typeName == null) {
                throw new IllegalStateException("Unknown message type: " + messageType.getName());
            }
            Integer typeId = null;
            for (Map.Entry<Integer, String> e : extendedHandshakeHandler.getPeerTypeMapping(context.getPeer()).entrySet()) {
                if (e.getValue().equals(typeName)) {
                    typeId = e.getKey();
                }
            }
            if (typeId == null) {
                throw new IllegalStateException("Peer does not support extension message: " + typeName);
            }
            buffer.put(typeId.byteValue());
        }

        boolean encoded = ((MessageHandler<T>) handlers.get(messageType)).encode(context, (T) message, buffer);
        if (!encoded) {
            buffer.position(begin);
        }
        return encoded;
    }
}
