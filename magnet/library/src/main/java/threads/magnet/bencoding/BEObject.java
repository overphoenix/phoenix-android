package threads.magnet.bencoding;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Generic bencoded object.
 *
 * @param <T> Java type of the object's value.
 * @since 1.0
 */
public interface BEObject<T> {

    /**
     * @return Object's type
     * @since 1.0
     */
    BEType getType();

// --Commented out by Inspection START (08.12.21, 18:44):
//    /**
//     * @return Binary representation of this object, as read from source
//     * (excluding type prefix and terminator, if applicable).
//     * @since 1.0
//     */
//    byte[] getContent();
// --Commented out by Inspection STOP (08.12.21, 18:44)

    /**
     * @return Object's value
     * @since 1.0
     */
    T getValue();

    /**
     * Write this object's contents to the provided stream (excluding type prefix and terminator).
     *
     * @since 1.0
     */
    void writeTo(OutputStream out) throws IOException;
}
