package threads.magnet.kad.tasks;

import android.annotation.SuppressLint;

import java.util.stream.Collectors;

import threads.magnet.LogUtils;
import threads.magnet.kad.AddressUtils;
import threads.magnet.kad.Key;
import threads.magnet.kad.Node;
import threads.magnet.kad.RPCServer;
import threads.magnet.kad.RPCState;
import threads.magnet.kad.tasks.IterativeLookupCandidates.LookupGraphNode;

public abstract class IteratingTask extends TargetedTask {

    final ClosestSet closest;
    final IterativeLookupCandidates todo;

    IteratingTask(Key target, RPCServer srv, Node node) {
        super(target, srv, node);
        todo = new IterativeLookupCandidates(target, node.getDHT().getMismatchDetector());
        todo.setNonReachableCache(node.getDHT().getUnreachableCache());
        todo.setSpamThrottle(node.getDHT().getServerManager().getOutgoingRequestThrottle());
        closest = new ClosestSet(target);
    }

    @Override
    public int getTodoCount() {
        return (int) todo.allCand().filter(todo.lookupFilter).count();
    }

    private String closestDebug() {
        return this.closest.entries().map(kbe -> {
            Key k = kbe.getID();
            return k + "  " + targetKey.distance(k) + " src:" + todo.nodeForEntry(kbe).sources.size();
        }).collect(Collectors.joining("\n"));
    }

    @SuppressLint("DefaultLocale")
    void logClosest() {
        Key farthest = closest.tail();

        if (LogUtils.isDebug()) {
            LogUtils.verbose(TAG, this.toString() + "\n" +

                    "Task " + getTaskID() + "  done " + counts + " " + closest + "\n" + targetKey + "\n" + closestDebug() + "\n" +


                    todo.allCand().sorted(todo.comp()).filter(node -> targetKey.threeWayDistance(node.toKbe().getID(), farthest) <= 0).map(node -> String.format("%s %s %s %s%s%s%s%s fail:%d src:%d call:%d rsp:%d acc:%d %s",
                            node.toKbe().getID(),
                            targetKey.distance(node.toKbe().getID()),
                            AddressUtils.toString(node.toKbe().getAddress()),
                            node.toKbe().hasSecureID() ? "🔒" : " ",
                            node.root ? "🌲" : " ",
                            node.tainted ? "!" : " ",
                            node.throttled ? "⏳" : " ",
                            node.unreachable ? "⛔" : " ",
                            -node.previouslyFailedCount,
                            node.sources.size(),
                            node.calls.size(),
                            node.calls.stream().filter(c -> c.state() == RPCState.RESPONDED).count(),
                            node.acceptedResponse ? 1 : 0,
                            node.sources.stream().map(LookupGraphNode::toKbe).collect(Collectors.toList())
                    )).collect(Collectors.joining("\n"))

            );
        }
    }

}
