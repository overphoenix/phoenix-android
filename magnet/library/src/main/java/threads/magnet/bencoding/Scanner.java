package threads.magnet.bencoding;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;

class Scanner {

    private final MemoizingPushbackInputStream source;

    Scanner(byte[] bs) {
        this(new ByteArrayInputStream(bs));
    }

    Scanner(InputStream in) {
        source = new MemoizingPushbackInputStream(in);
    }

    <T extends BEObject> T readObject(BEObjectBuilder<T> builder) throws Exception {

        source.resetContents();

        int c;
        while ((c = source.read()) != -1 && builder.accept(c))
            ;
        return builder.build();
    }

    int peek() {
        return source.peek();
    }

    void close() {
        try {
            source.close();
        } catch (IOException e) {
            // do nothing
        }
    }

    private static class MemoizingPushbackInputStream extends InputStream {

        private final PushbackInputStream delegate;
        private ByteArrayOutputStream buf;

        MemoizingPushbackInputStream(InputStream delegate) {
            this.delegate = new PushbackInputStream(delegate);
            buf = new ByteArrayOutputStream();
        }

        @Override
        public int read() throws IOException {
            int c = delegate.read();
            buf.write(c);
            return c;
        }

        int peek() {
            try {
                int c = delegate.read();
                delegate.unread(c);
                return c;
            } catch (IOException e) {
                throw new RuntimeException("Unexpected I/O exception", e);
            }
        }

        void resetContents() {
            buf = new ByteArrayOutputStream();
        }


        @Override
        public void close() throws IOException {
            delegate.close();
        }
    }
}
