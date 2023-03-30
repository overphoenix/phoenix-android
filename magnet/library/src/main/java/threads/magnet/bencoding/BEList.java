package threads.magnet.bencoding;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * BEncoded list. May contain objects of different types.
 *
 * @since 1.0
 */
public class BEList implements BEObject<List<? extends BEObject<?>>> {

    private final byte[] content;
    private final List<? extends BEObject<?>> value;
    private final BEEncoder encoder;

    /**
     * @param content Binary representation of this list, as read from source.
     * @param value   Parsed value
     * @since 1.0
     */
    public BEList(byte[] content, List<? extends BEObject<?>> value) {
        this.content = content;
        this.value = Collections.unmodifiableList(value);
        encoder = BEEncoder.encoder();
    }

    @Override
    public BEType getType() {
        return BEType.LIST;
    }

    public byte[] getContent() {
        return content;
    }

    @Override
    public List<? extends BEObject<?>> getValue() {
        return value;
    }

    @Override
    public void writeTo(OutputStream out) throws IOException {
        encoder.encode(this, out);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

    @Override
    public boolean equals(Object obj) {

        if (!(obj instanceof BEList)) {
            return false;
        }

        if (obj == this) {
            return true;
        }

        return value.equals(((BEList) obj).getValue());
    }

    @NonNull
    @Override
    public String toString() {
        return Arrays.toString(value.toArray());
    }
}
