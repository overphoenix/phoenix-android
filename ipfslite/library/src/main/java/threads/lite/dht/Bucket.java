package threads.lite.dht;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import threads.lite.cid.Peer;


public class Bucket {
    private static final String TAG = Bucket.class.getSimpleName();
    private final ConcurrentHashMap<Peer, PeerInfo> peers = new ConcurrentHashMap<>();


    public boolean containsPeer(@NonNull Peer peerId) {
        return peers.containsKey(peerId);
    }


    @Nullable
    public Peer weakest() {
        if (size() == 0) {
            return null;
        }
        long latency = 0;
        Peer found = null;
        for (Map.Entry<Peer, PeerInfo> entry : peers.entrySet()) {
            PeerInfo info = entry.getValue();
            Peer peerId = entry.getKey();
            if (info.isReplaceable()) {
                long tmp = peerId.getLatency();

                if (tmp >= latency) {
                    latency = tmp;
                    found = peerId;
                }

                if (tmp == Long.MAX_VALUE) {
                    break;
                }
            }
        }
        return found;
    }


    @NonNull
    @Override
    public String toString() {
        return "Bucket{" +
                "peers=" + peers.size() +
                '}';
    }

    public void addPeer(@NonNull Peer peerId, boolean isReplaceable) {
        synchronized (TAG.intern()) {
            if (!peers.containsKey(peerId)) {
                Bucket.PeerInfo peerInfo = new Bucket.PeerInfo(peerId, isReplaceable);
                peers.put(peerId, peerInfo);
            }
        }
    }

    public boolean removePeer(@NonNull Peer p) {
        PeerInfo peerInfo = peers.get(p);
        if (peerInfo != null) {
            if (peerInfo.isReplaceable()) {
                return peers.remove(p) != null;
            }
        }
        return false;
    }

    public int size() {
        return peers.size();
    }


    @NonNull
    public Collection<PeerInfo> values() {
        return peers.values();
    }


    public static class PeerInfo {
        @NonNull
        private final Peer peerId;
        @NonNull
        private final ID id;
        // if a bucket is full, this peer can be replaced to make space for a new peer.
        private final boolean replaceable;

        public PeerInfo(@NonNull Peer peerId, boolean replaceable) {
            this.peerId = peerId;
            this.id = ID.convertPeerID(peerId.getPeerId());
            this.replaceable = replaceable;
        }

        public boolean isReplaceable() {
            return replaceable;
        }

        @NonNull
        @Override
        public String toString() {
            return "PeerInfo{" +
                    "peerId=" + peerId +
                    ", replaceable=" + replaceable +
                    '}';
        }

        @NonNull
        public Peer getPeerId() {
            return peerId;
        }

        @NonNull
        public ID getID() {
            return id;
        }

    }
}
