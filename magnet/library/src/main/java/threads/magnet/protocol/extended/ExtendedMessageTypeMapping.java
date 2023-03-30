package threads.magnet.protocol.extended;

import java.util.function.BiConsumer;

/**
 * Represents a set of mappings between message types
 * (both literal names and Java types) and their numeric IDs.
 *
 * <p>Used in «BEP-10: Extension Protocol» for communicating client-specific message type IDs to peers.</p>
 * <p>All mappings are listed as a set of key-value pairs in a dictionary of supported message types in the extended handshake.
 * In each pair, key is the literal name of a message type, and value is a unique numeric ID, assigned to this message type.
 * Numeric message type IDs are then included in the binary representation of a message
 * and can be used to determine the type of this message.</p>
 *
 * @see <a href="http://bittorrent.org/beps/bep_0010.html">BEP-10: Extension Protocol</a>
 * @since 1.0
 */
public interface ExtendedMessageTypeMapping {

    /**
     * Get literal name of a message type with a given numeric ID.
     *
     * @param typeId Numeric message type ID
     * @return Message type name
     * @since 1.0
     */
    String getTypeNameForId(Integer typeId);

    /**
     * Get literal name for a message type.
     *
     * @param type Message Java type
     * @return Message type name
     * @since 1.0
     */
    String getTypeNameForJavaType(Class<?> type);

    /**
     * Visitor interface for all mappings, contained in this set.
     *
     * @param visitor First parameter is message type name,
     *                second parameter is numeric message type ID.
     * @since 1.0
     */
    void visitMappings(BiConsumer<String, Integer> visitor);
}
