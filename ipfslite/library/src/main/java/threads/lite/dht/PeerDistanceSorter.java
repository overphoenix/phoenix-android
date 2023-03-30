package threads.lite.dht;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import threads.lite.LogUtils;
import threads.lite.cid.Peer;

public class PeerDistanceSorter extends ArrayList<PeerDistanceSorter.PeerDistance> {
    private static final String TAG = PeerDistanceSorter.class.getSimpleName();
    private final ID target;

    public PeerDistanceSorter(@NonNull ID target) {
        this.target = target;
    }

    @NonNull
    @Override
    public String toString() {
        return "PeerDistanceSorter{" +
                "target=" + target +
                '}';
    }

    public void appendPeer(@NonNull Peer peerId, @NonNull ID id) {
        this.add(new PeerDistance(peerId, ID.xor(target, id)));
    }

    public void appendPeersFromList(@NonNull Bucket bucket) {
        for (Bucket.PeerInfo peerInfo : bucket.values()) {
            appendPeer(peerInfo.getPeerId(), peerInfo.getID());
        }
    }

    public List<Peer> sortedList() {
        LogUtils.verbose(TAG, this.toString());
        Collections.sort(this);
        List<Peer> list = new ArrayList<>();
        for (PeerDistanceSorter.PeerDistance dist : this) {
            list.add(dist.peer);
        }
        return list;
    }

    public static class PeerDistance implements Comparable<PeerDistance> {
        private final Peer peer;
        private final ID distance;

        protected PeerDistance(@NonNull Peer peer, @NonNull ID distance) {
            this.peer = peer;
            this.distance = distance;
        }

        @NonNull
        @Override
        public String toString() {
            return "PeerDistance{" +
                    "peerId=" + peer +
                    ", distance=" + distance +
                    '}';
        }

        @Override
        public int compareTo(@NonNull PeerDistance o) {
            return this.distance.compareTo(o.distance);
        }

        @NonNull
        public Peer getPeer() {
            return peer;
        }
    }
}
