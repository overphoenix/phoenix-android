package threads.magnet.kad.tasks;

import androidx.annotation.NonNull;

import java.util.Objects;

import threads.magnet.kad.Key;
import threads.magnet.kad.Node;
import threads.magnet.kad.RPCServer;

abstract class TargetedTask extends Task {

    final Key targetKey;


    TargetedTask(Key k, @NonNull RPCServer rpc, Node node) {
        super(rpc, node);
        Objects.requireNonNull(k);
        targetKey = k;
    }

    public Key getTargetKey() {
        return targetKey;
    }

}
