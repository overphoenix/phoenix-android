package threads.magnet.torrent;

import java.nio.ByteBuffer;

public interface BlockReader {

    boolean readTo(ByteBuffer buffer);
}
