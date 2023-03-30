package threads.lite.dht;

import androidx.annotation.NonNull;

import java.util.function.Consumer;

import threads.lite.cid.Cid;
import threads.lite.cid.Peer;
import threads.lite.cid.PeerId;
import threads.lite.core.Closeable;
import threads.lite.ipns.Ipns;

public interface Routing {
    void putValue(@NonNull Closeable closable, @NonNull byte[] key, @NonNull byte[] data);


    void findPeer(@NonNull Closeable closeable, @NonNull Consumer<Peer> consumer, @NonNull PeerId peerID);


    void searchValue(@NonNull Closeable closeable, @NonNull Consumer<Ipns.Entry> consumer,
                     @NonNull byte[] key);


    void findProviders(@NonNull Closeable closeable, @NonNull Consumer<Peer> providers,
                       @NonNull Cid cid, boolean acceptLocalAddress);

    void provide(@NonNull Closeable closeable, @NonNull Cid cid);

}
