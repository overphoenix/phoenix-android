package tech.nagual.phoenix.tools.browser.magic.endian;

import java.nio.ByteOrder;

public enum EndianType {
    /**
     * big endian, also called network byte order (motorola 68k)
     */
    BIG(new BigEndianConverter()),
    /**
     * little endian (x86)
     */
    LITTLE(new LittleEndianConverter()),
    /**
     * old PDP11 byte order
     */
    MIDDLE(new MiddleEndianConverter()),
    /**
     * uses the byte order of the current system
     */
    NATIVE(ByteOrder.nativeOrder() == ByteOrder.BIG_ENDIAN ? BIG.getConverter() : LITTLE.getConverter()),
    // end
    ;

    private final EndianConverter converter;

    EndianType(EndianConverter converter) {
        this.converter = converter;
    }

    /**
     * Returns the converter associated with this endian-type.
     */
    public EndianConverter getConverter() {
        return converter;
    }
}
