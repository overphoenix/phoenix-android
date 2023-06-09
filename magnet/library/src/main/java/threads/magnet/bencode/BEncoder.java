package threads.magnet.bencode;

import static threads.magnet.bencode.Utils.str2buf;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.stream.Stream;

public class BEncoder {

    private final static byte[] MIN_INT = str2buf(Integer.toString(Integer.MIN_VALUE)).array();
    private ByteBuffer buf;

    public void encodeInto(Map<String, Object> toEnc, ByteBuffer target) {
        buf = target;
        encodeMap(toEnc);
        buf.flip();
    }

    private void encodeInternal(Object o) {
        if (o instanceof Map) {
            encodeMap((Map<String, Object>) o);
            return;
        }

        if (o instanceof List) {
            encodeList((List<Object>) o);
            return;
        }


        if (o instanceof String) {
            encodeString((String) o);
            return;
        }

        if (o instanceof byte[]) {
            byte[] b = (byte[]) o;
            encodeInt(b.length, (byte) ':');
            buf.put(b);
            return;
        }

        if (o instanceof ByteBuffer) {
            ByteBuffer clone = ((ByteBuffer) o).slice();
            encodeInt(clone.remaining(), (byte) ':');
            buf.put(clone);
            return;
        }

        if (o instanceof Integer) {
            buf.put((byte) 'i');
            encodeInt((Integer) o, (byte) 'e');
            return;
        }

        if (o instanceof Long) {
            buf.put((byte) 'i');
            encodeLong((Long) o);
            return;
        }

        if (o instanceof RawData) {
            ByteBuffer raw = ((RawData) o).rawBuf;
            buf.put(raw.duplicate());
            return;
        }

        if (o instanceof StringWriter) {
            StringWriter w = (StringWriter) o;
            encodeInt(w.length(), (byte) ':');
            w.writeTo(buf);
            return;
        }

        if (o instanceof Stream) {
            encodeStream((Stream<Object>) o);
            return;
        }

        throw new RuntimeException("unknown object to encode " + o);
    }

    private void encodeStream(Stream<Object> l) {
        buf.put((byte) 'l');
        l.forEach(this::encodeInternal);
        buf.put((byte) 'e');
    }

    private void encodeList(List<Object> l) {
        buf.put((byte) 'l');
        l.forEach(this::encodeInternal);
        buf.put((byte) 'e');
    }

    private void encodeString(String str) {
        encodeInt(str.length(), (byte) ':');
        str2buf(str, buf);
    }

    private void encodeMap(Map<String, Object> map) {
        buf.put((byte) 'd');
        Stream<Entry<String, Object>> str;
        if (map instanceof SortedMap<?, ?> && ((SortedMap<?, ?>) map).comparator() == null)
            str = map.entrySet().stream();
        else
            str = map.entrySet().stream().sorted(Map.Entry.comparingByKey());

        str.forEachOrdered(e -> {
            encodeString(e.getKey());
            encodeInternal(e.getValue());
        });
        buf.put((byte) 'e');
    }

    private void encodeInt(int val, byte terminator) {
        if (val == Integer.MIN_VALUE)
            buf.put(MIN_INT);
        else {
            if (val < 0) {
                buf.put((byte) '-');
                val = -val;
            }

            int numChars = 10;
            int probe = 10;
            for (int i = 1; i < 10; i++) {
                if (val < probe) {
                    numChars = i;
                    break;
                }
                probe = 10 * probe;
            }

            int pos = buf.position() + numChars;

            buf.position(pos);

            for (int i = 1; i <= numChars; i++) {
                int reduced = val / 10;
                int remainder = val - (reduced * 10);
                buf.put(pos - i, (byte) ('0' + remainder));
                val = reduced;
            }

        }
        buf.put(terminator);
    }

    private void encodeLong(long val) {
        str2buf(Long.toString(val), buf);
        buf.put((byte) 'e');
    }

    public interface StringWriter {
        int length();

        void writeTo(ByteBuffer buf);
    }

    public static class RawData {
        final ByteBuffer rawBuf;

        public RawData(ByteBuffer b) {
            rawBuf = b;
        }
    }
}
