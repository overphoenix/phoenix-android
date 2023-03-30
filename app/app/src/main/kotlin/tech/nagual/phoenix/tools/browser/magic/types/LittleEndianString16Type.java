package tech.nagual.phoenix.tools.browser.magic.types;

/**
 * A two-byte unicode (UCS16) string in little-endian byte order.
 *
 * @author graywatson
 */
public class LittleEndianString16Type extends BigEndianString16Type {

    @Override
    protected char bytesToChar(int firstByte, int secondByte) {
        return (char) ((secondByte << 8) + firstByte);
    }
}
