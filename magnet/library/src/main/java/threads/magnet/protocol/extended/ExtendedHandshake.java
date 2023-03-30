package threads.magnet.protocol.extended;

import java.math.BigInteger;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import threads.magnet.bencoding.BEInteger;
import threads.magnet.bencoding.BEMap;
import threads.magnet.bencoding.BEObject;

/**
 * Extended handshake is sent during connection initialization procedure
 * by peers that support BEP-10: Extension Protocol.
 * It contains a dictionary of supported extended message types with
 * their corresponding numeric IDs, as well as any additional information,
 * that is specific to concrete BitTorrent clients and BEPs,
 * that utilize extended messaging.
 *
 * @since 1.0
 */
public final class ExtendedHandshake extends ExtendedMessage {

    /**
     * Message type mapping key in the extended handshake.
     *
     * @since 1.0
     */
    static final String MESSAGE_TYPE_MAPPING_KEY = "m";

    static final String ENCRYPTION_PROPERTY = "e";
    static final String TCPPORT_PROPERTY = "p";
    static final String VERSION_PROPERTY = "v";
    private final Map<String, BEObject<?>> data;
    private final Set<String> supportedMessageTypes;

    ExtendedHandshake(Map<String, BEObject<?>> data) {
        this.data = Collections.unmodifiableMap(data);

        BEMap supportedMessageTypes = (BEMap) data.get(MESSAGE_TYPE_MAPPING_KEY);
        if (supportedMessageTypes != null) {
            this.supportedMessageTypes = Collections.unmodifiableSet(supportedMessageTypes.getValue().keySet());
        } else {
            this.supportedMessageTypes = Collections.emptySet();
        }
    }

    /**
     * @since 1.0
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * @return Payload of this extended handshake.
     * @since 1.0
     */
    public Map<String, BEObject<?>> getData() {
        return data;
    }

    /**
     * @return TCP port or null, if absent in message data
     * @since 1.9
     */
    public BEInteger getPort() {
        return (BEInteger) data.get(TCPPORT_PROPERTY);
    }

    /**
     * @return Set of message type names, that are specified
     * in this handshake's message type mapping.
     * @since 1.0
     */
    public Set<String> getSupportedMessageTypes() {
        return supportedMessageTypes;
    }

    @Override
    public String toString() {
        return "[" + this.getClass().getSimpleName() + "] supported messages {" + supportedMessageTypes + "}, data {" + data + "}";
    }

    /**
     * Convenient builder that provides the means
     * to build an extended handshake by specifying
     * one message type mapping at a time.
     *
     * @since 1.0
     */
    public static class Builder {

        private final Map<String, BEObject<?>> data;
        private Map<String, BEObject<?>> messageTypeMap;

        private Builder() {
            data = new HashMap<>();
        }

        void property(String name, BEObject<?> value) {
            Objects.requireNonNull(name);
            if (MESSAGE_TYPE_MAPPING_KEY.equals(name)) {
                throw new IllegalArgumentException("Property name is reserved: " + MESSAGE_TYPE_MAPPING_KEY);
            }

            Objects.requireNonNull(value);
            data.put(name, value);
        }

        /**
         * Adds a mapping between message type name and its' numeric ID.
         *
         * @param typeName Message type name
         * @param typeId   Numeric message type ID
         * @since 1.0
         */
        public void addMessageType(String typeName, Integer typeId) {

            if (messageTypeMap == null) {
                messageTypeMap = new HashMap<>();
            }

            if (messageTypeMap.containsKey(Objects.requireNonNull(typeName))) {
                throw new RuntimeException("Message type already defined: " + typeName);
            }

            messageTypeMap.put(typeName, new BEInteger(null, BigInteger.valueOf((long) typeId)));
        }

        /**
         * @since 1.0
         */
        public ExtendedHandshake build() {

            if (messageTypeMap != null) {
                data.put(MESSAGE_TYPE_MAPPING_KEY, new BEMap(null, messageTypeMap));
            }
            return new ExtendedHandshake(data);
        }
    }
}
