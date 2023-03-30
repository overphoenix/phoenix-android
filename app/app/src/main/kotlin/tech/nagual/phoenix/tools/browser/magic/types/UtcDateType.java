package tech.nagual.phoenix.tools.browser.magic.types;

import java.util.Date;

import tech.nagual.phoenix.tools.browser.magic.endian.EndianType;

/**
 * A 4-byte value interpreted as a UNIX date in UTC timezone.
 *
 * @author graywatson
 */
public class UtcDateType extends LocalDateType {


    public UtcDateType(EndianType endianType) {
        super(endianType);
    }

    @Override
    protected Date dateFromExtractedValue(long val) {
        val *= 1000;
        return new Date(val);
    }


}
