package tech.nagual.phoenix.tools.browser.magic.types;

import tech.nagual.phoenix.tools.browser.magic.endian.EndianType;

/**
 * A two-byte value.
 *
 * @author graywatson
 */
public class ShortType extends BaseLongType {

    private static final int BYTES_PER_SHORT = 2;

    public ShortType(EndianType endianType) {
        super(endianType);
    }

    @Override
    public int getBytesPerType() {
        return BYTES_PER_SHORT;
    }

    @Override
    public long maskValue(long value) {
        return value & 0xFFFFL;
    }

    @Override
    public int compare(boolean unsignedType, Number extractedValue, Number testValue) {
        if (unsignedType) {
            return LongType.staticCompare(extractedValue, testValue);
        }
        short extractedShort = extractedValue.shortValue();
        short testShort = testValue.shortValue();
        return Short.compare(extractedShort, testShort);
    }
}
