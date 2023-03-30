package threads.lite.dag;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;

import threads.lite.cid.Cid;
import threads.lite.core.Closeable;
import threads.lite.core.ClosedException;
import threads.lite.format.Block;
import threads.lite.format.Decoder;
import threads.lite.format.Node;
import threads.lite.format.NodeAdder;
import threads.lite.format.NodeGetter;

public interface DagService extends NodeGetter, NodeAdder {


    static DagService createDagService(@NonNull BlockService blockService) {
        return new DagService() {

            @Override
            @Nullable
            public Node getNode(@NonNull Closeable closeable, @NonNull Cid cid, boolean root) throws ClosedException {

                Block b = blockService.getBlock(closeable, cid, root);
                if (b == null) {
                    return null;
                }
                return Decoder.Decode(b);
            }

            @Override
            public void preload(@NonNull Closeable closeable, @NonNull List<Cid> cids) {
                blockService.preload(closeable, cids);
            }

            public void add(@NonNull Node nd) {
                blockService.addBlock(nd);
            }
        };
    }


}
