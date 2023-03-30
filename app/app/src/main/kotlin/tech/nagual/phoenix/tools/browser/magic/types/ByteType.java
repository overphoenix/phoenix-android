package tech.nagual.phoenix.tools.browser.magic.types;

import tech.nagual.phoenix.tools.browser.magic.endian.EndianType;

/**
 * A one-byte value.
 *
 * @author graywatson
 */
public class ByteType extends BaseLongType {

    public ByteType() {
        // we don't care about byte order since we only process 1 byte at a time
        super(EndianType.NATIVE);
    }

    @Override
    public int getBytesPerType() {
        return 1;
    }

    @Override
    public long maskValue(long value) {
        return value & 0xFFL;
    }

    @Override
    public int compare(boolean unsignedType, Number extractedValue, Number testValue) {
        if (unsignedType) {
            return LongType.staticCompare(extractedValue, testValue);
        }
        byte extractedByte = extractedValue.byteValue();
        byte testByte = testValue.byteValue();
        return Byte.compare(extractedByte, testByte);
    }
}
