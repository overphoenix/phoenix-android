package threads.lite.cid;

import java.util.Set;

public class Peer {
    private final PeerId peerId;
    private final Set<Multiaddr> multiaddrs;
    private long latency = Long.MAX_VALUE;

    public Peer(PeerId peerId, Set<Multiaddr> multiaddrs) {
        this.peerId = peerId;
        this.multiaddrs = multiaddrs;
    }

    public long getLatency() {
        return latency;
    }

    public void setLatency(long latency) {
        this.latency = latency;
    }

    public PeerId getPeerId() {
        return peerId;
    }

    public Set<Multiaddr> getMultiaddrs() {
        return multiaddrs;
    }

    public boolean hasAddresses() {
        return !multiaddrs.isEmpty();
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Peer peer = (Peer) o;
        return peerId.equals(peer.getPeerId());
    }

    @Override
    public int hashCode() {
        return peerId.hashCode();
    }
}
