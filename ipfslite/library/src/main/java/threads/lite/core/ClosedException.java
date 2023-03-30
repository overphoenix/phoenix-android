package threads.lite.core;

import java.io.IOException;

public class ClosedException extends IOException {
    public ClosedException() {
        super("Context closed");
    }
}
