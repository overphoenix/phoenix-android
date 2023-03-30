package tech.nagual.phoenix.tools.browser.magic.entries;

import java.util.HashMap;
import java.util.Map;

import tech.nagual.phoenix.tools.browser.magic.endian.EndianType;
import tech.nagual.phoenix.tools.browser.magic.types.BigEndianString16Type;
import tech.nagual.phoenix.tools.browser.magic.types.ByteType;
import tech.nagual.phoenix.tools.browser.magic.types.DefaultType;
import tech.nagual.phoenix.tools.browser.magic.types.DoubleType;
import tech.nagual.phoenix.tools.browser.magic.types.FloatType;
import tech.nagual.phoenix.tools.browser.magic.types.Id3LengthType;
import tech.nagual.phoenix.tools.browser.magic.types.IntegerType;
import tech.nagual.phoenix.tools.browser.magic.types.LittleEndianString16Type;
import tech.nagual.phoenix.tools.browser.magic.types.LocalDateType;
import tech.nagual.phoenix.tools.browser.magic.types.LocalLongDateType;
import tech.nagual.phoenix.tools.browser.magic.types.LongType;
import tech.nagual.phoenix.tools.browser.magic.types.PStringType;
import tech.nagual.phoenix.tools.browser.magic.types.RegexType;
import tech.nagual.phoenix.tools.browser.magic.types.SearchType;
import tech.nagual.phoenix.tools.browser.magic.types.ShortType;
import tech.nagual.phoenix.tools.browser.magic.types.StringType;
import tech.nagual.phoenix.tools.browser.magic.types.UtcDateType;
import tech.nagual.phoenix.tools.browser.magic.types.UtcLongDateType;

/**
 * The various types which correspond to the "type" part of the magic (5) format.
 *
 * @author graywatson
 */
public enum MagicType {

    /**
     * Single byte value.
     */
    BYTE("byte", new ByteType()),
    /**
     * 2 byte short integer in native-endian byte order.
     */
    SHORT("short", new ShortType(EndianType.NATIVE)),
    /**
     * 4 byte "long" integer in native-endian byte order. This is C language long (shudder).
     */
    INTEGER("long", new IntegerType(EndianType.NATIVE)),
    /**
     * 8 byte long integer in native-endian byte order.
     */
    QUAD("quad", new LongType(EndianType.NATIVE)),
    /**
     * 4 byte floating point number in native-endian byte order.
     */
    FLOAT("float", new FloatType(EndianType.NATIVE)),
    /**
     * 8 byte floating point number in native-endian byte order.
     */
    DOUBLE("double", new DoubleType(EndianType.NATIVE)),
    /**
     * Special string matching that supports white-space and case handling.
     */
    STRING("string", new StringType()),
    /**
     * Strings that are encoded with the first byte being the length of the string.
     */
    PSTRING("pstring", new PStringType()),
    /**
     * 4 byte value in native=endian byte order, interpreted as a Unix date using UTC time zone.
     */
    DATE("date", new UtcDateType(EndianType.NATIVE)),
    /**
     * 8 byte value in native-endian byte order, interpreted as a Unix date using UTC time zone.
     */
    LONG_DATE("qdate", new UtcLongDateType(EndianType.NATIVE)),
    /**
     * 4 byte value in native-endian byte order, interpreted as a Unix date using the local time zone.
     */
    LOCAL_DATE("ldate", new LocalDateType(EndianType.NATIVE)),
    /**
     * 8 byte value in native-endian byte order, interpreted as a Unix date using the local time zone.
     */
    LONG_LOCAL_DATE("qldate", new LocalLongDateType(EndianType.NATIVE)),

    /**
     * 4 byte integer with each byte using lower 7-bits in big-endian byte order.
     */
    BIG_ENDIAN_ID3("beid3", new Id3LengthType(EndianType.BIG)),
    /**
     * 2 byte short integer in big-endian byte order.
     */
    BIG_ENDIAN_SHORT("beshort", new ShortType(EndianType.BIG)),
    /**
     * 4 byte "long" integer in big-endian byte order. This is C language long (shudder).
     */
    BIG_ENDIAN_INTEGER("belong", new IntegerType(EndianType.BIG)),
    /**
     * 8 byte long integer in big-endian byte order.
     */
    BIG_ENDIAN_QUAD("bequad", new LongType(EndianType.BIG)),
    /**
     * 4 byte floating point number in big-endian byte order.
     */
    BIG_ENDIAN_FLOAT("befloat", new FloatType(EndianType.BIG)),
    /**
     * 8 byte floating point number in big-endian byte order.
     */
    BIG_ENDIAN_DOUBLE("bedouble", new DoubleType(EndianType.BIG)),
    /**
     * 4 byte value in big-endian byte order, interpreted as a Unix date using UTC time zone.
     */
    BIG_ENDIAN_DATE("bedate", new UtcDateType(EndianType.BIG)),
    /**
     * 8 byte value in big-endian byte order, interpreted as a Unix date using UTC time zone.
     */
    BIG_ENDIAN_LONG_DATE("beqdate", new UtcLongDateType(EndianType.BIG)),
    /**
     * 4 byte value big-endian byte order, interpreted as a Unix date using the local time zone.
     */
    BIG_ENDIAN_LOCAL_DATE("beldate", new LocalDateType(EndianType.BIG)),
    /**
     * 8 byte value in big-endian byte order, interpreted as a Unix date using the local time zone.
     */
    BIG_ENDIAN_LONG_LOCAL_DATE("beqldate", new LocalLongDateType(EndianType.BIG)),
    /**
     * String made up of 2-byte characters in big-endian byte order.
     */
    BIG_ENDIAN_TWO_BYTE_STRING("bestring16", new BigEndianString16Type()),

    /**
     * 4 byte integer with each byte using lower 7-bits in little-endian byte order.
     */
    LITTLE_ENDIAN_ID3("leid3", new Id3LengthType(EndianType.LITTLE)),
    /**
     * 2 byte short integer in little-endian byte order.
     */
    LITTLE_ENDIAN_SHORT("leshort", new ShortType(EndianType.LITTLE)),
    /**
     * 4 byte "long" integer in little-endian byte order. This is C language long (shudder).
     */
    LITTLE_ENDIAN_INTEGER("lelong", new IntegerType(EndianType.LITTLE)),
    /**
     * 8 byte long integer in little-endian byte order.
     */
    LITTLE_ENDIAN_QUAD("lequad", new LongType(EndianType.LITTLE)),
    /**
     * 4 byte floating point number in little-endian byte order.
     */
    LITTLE_ENDIAN_FLOAT("lefloat", new FloatType(EndianType.LITTLE)),
    /**
     * 8 byte floating point number in little-endian byte order.
     */
    LITTLE_ENDIAN_DOUBLE("ledouble", new DoubleType(EndianType.LITTLE)),
    /**
     * 4 byte value in little-endian byte order, interpreted as a Unix date using UTC time zone.
     */
    LITTLE_ENDIAN_DATE("ledate", new UtcDateType(EndianType.LITTLE)),
    /**
     * 8 byte value in little-endian byte order, interpreted as a Unix date using UTC time zone.
     */
    LITTLE_ENDIAN_LONG_DATE("leqdate", new UtcLongDateType(EndianType.LITTLE)),
    /**
     * 4 byte value little-endian byte order, interpreted as a Unix date using the local time zone.
     */
    LITTLE_ENDIAN_LOCAL_DATE("leldate", new LocalDateType(EndianType.LITTLE)),
    /**
     * 8 byte value in little-endian byte order, interpreted as a Unix date using the local time zone.
     */
    LITTLE_ENDIAN_LONG_LOCAL_DATE("leqldate", new LocalLongDateType(EndianType.LITTLE)),
    /**
     * String made up of 2-byte characters in little-endian byte order.
     */
    LITTLE_ENDIAN_TWO_BYTE_STRING("lestring16", new LittleEndianString16Type()),

    // indirect -- special

    /**
     * Regex line search looking for compiled patterns.
     */
    REGEX("regex", new RegexType()),
    /**
     * String line search looking for sub-strings.
     */
    SEARCH("search", new SearchType()),

    /**
     * 4 byte "long" integer in middle-endian byte order. This is C language long (shudder).
     */
    MIDDLE_ENDIAN_INTEGER("melong", new IntegerType(EndianType.MIDDLE)),
    /**
     * 4 byte value in middle-endian byte order, interpreted as a Unix date using UTC time zone.
     */
    MIDDLE_ENDIAN_DATE("medate", new UtcDateType(EndianType.MIDDLE)),
    /**
     * 4 byte value middle-endian byte order, interpreted as a Unix date using the local time zone.
     */
    MIDDLE_ENDIAN_LOCAL_DATE("meldate", new LocalDateType(EndianType.MIDDLE)),

    /**
     * Default type that always matches. Used in rule chaining.
     */
    DEFAULT("default", new DefaultType()),
    // end
    ;

    private static final Map<String, MagicMatcher> typeMap = new HashMap<>();

    static {
        for (MagicType type : values()) {
            typeMap.put(type.name, type.matcher);
        }
    }

    private final String name;
    private final MagicMatcher matcher;

    MagicType(String name, MagicMatcher matcher) {
        this.name = name;
        this.matcher = matcher;
    }

    /**
     * Find the associated matcher to the string.
     */
    public static MagicMatcher matcherfromString(String typeString) {
        return typeMap.get(typeString);
    }
}
