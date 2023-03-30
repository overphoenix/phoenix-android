package threads.magnet.net.buffer;

public class BufferedData {

    private final ByteBufferView buffer;
    private volatile boolean disposed;

    public BufferedData(ByteBufferView buffer) {
        this.buffer = buffer;
    }

    public ByteBufferView buffer() {
        return buffer;
    }


    public void dispose() {
        disposed = true;
    }

    public boolean isDisposed() {
        return disposed;
    }
}
