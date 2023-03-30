package tech.nagual.phoenix.tools.browser.magic.types;

import java.util.Date;

import tech.nagual.phoenix.tools.browser.magic.endian.EndianType;

/**
 * A 8-byte value interpreted as a UNIX date in UTC timezone.
 *
 * @author graywatson
 */
public class UtcLongDateType extends UtcDateType {

    private static final int BYTES_PER_UTC_LONG_DATE = 8;

    public UtcLongDateType(EndianType endianType) {
        super(endianType);
    }

    @Override
    protected Date dateFromExtractedValue(long val) {
        // XXX: is this in millis or seconds?
        // val *= 1000;
        return new Date(val);
    }

    @Override
    public int getBytesPerType() {
        return BYTES_PER_UTC_LONG_DATE;
    }
}
