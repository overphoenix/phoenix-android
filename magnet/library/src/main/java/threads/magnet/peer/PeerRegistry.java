package threads.magnet.peer;

import androidx.annotation.NonNull;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import threads.magnet.LogUtils;
import threads.magnet.Settings;
import threads.magnet.event.EventSink;
import threads.magnet.metainfo.Torrent;
import threads.magnet.metainfo.TorrentId;
import threads.magnet.net.InetPeer;
import threads.magnet.net.Peer;
import threads.magnet.net.PeerId;
import threads.magnet.service.RuntimeLifecycleBinder;
import threads.magnet.torrent.TorrentDescriptor;
import threads.magnet.torrent.TorrentRegistry;

public final class PeerRegistry {

    private static final String TAG = PeerRegistry.class.getSimpleName();
    private final Peer localPeer;

    private final TorrentRegistry torrentRegistry;
    private final EventSink eventSink;
    private final Set<PeerSourceFactory> extraPeerSourceFactories = new HashSet<>();


    public PeerRegistry(@NonNull RuntimeLifecycleBinder lifecycleBinder,
                        @NonNull TorrentRegistry torrentRegistry,
                        @NonNull EventSink eventSink,
                        @NonNull PeerId peerId,
                        int acceptorPort) {

        this.localPeer = InetPeer.builder(Settings.acceptorAddress, acceptorPort)
                .peerId(peerId)
                .build();

        this.torrentRegistry = torrentRegistry;
        this.eventSink = eventSink;


        createExecutor(lifecycleBinder);
    }

    public void addPeerSourceFactory(@NonNull PeerSourceFactory factory) {
        extraPeerSourceFactories.add(factory);
    }

    private void createExecutor(RuntimeLifecycleBinder lifecycleBinder) {
        ScheduledExecutorService executor =
                Executors.newSingleThreadScheduledExecutor(r -> new Thread(r, "bt.peer.peer-collector"));
        lifecycleBinder.onStartup("Schedule periodic peer lookup", () -> executor.scheduleAtFixedRate(
                this::collectAndVisitPeers, 1, Settings.peerDiscoveryInterval.toMillis(), TimeUnit.MILLISECONDS));
        lifecycleBinder.onShutdown("Shutdown peer lookup scheduler", executor::shutdownNow);
    }

    private void collectAndVisitPeers() {
        torrentRegistry.getTorrentIds().forEach(torrentId -> {
            Optional<TorrentDescriptor> descriptor = torrentRegistry.getDescriptor(torrentId);
            if (descriptor.isPresent() && descriptor.get().isActive()) {
                Optional<Torrent> torrentOptional = torrentRegistry.getTorrent(torrentId);


                // disallow querying peer sources other than the tracker for private torrents
                if ((!torrentOptional.isPresent() || !torrentOptional.get().isPrivate()) && !extraPeerSourceFactories.isEmpty()) {
                    extraPeerSourceFactories.forEach(factory ->
                            queryPeerSource(torrentId, factory.getPeerSource(torrentId)));
                }
            }
        });
    }

    private void queryPeerSource(TorrentId torrentId, PeerSource peerSource) {
        try {
            if (peerSource.update()) {
                Collection<Peer> discoveredPeers = peerSource.getPeers();
                Set<Peer> addedPeers = new HashSet<>();
                Iterator<Peer> iter = discoveredPeers.iterator();
                while (iter.hasNext()) {
                    Peer peer = iter.next();
                    if (!addedPeers.contains(peer)) {
                        addPeer(torrentId, peer);
                        addedPeers.add(peer);
                    }
                    iter.remove();
                }
            }
        } catch (Exception e) {
            LogUtils.error(TAG, "Error when querying peer source: " + peerSource, e);
        }
    }

    public void addPeer(TorrentId torrentId, Peer peer) {
        if (peer.isPortUnknown()) {
            throw new IllegalArgumentException("Peer's port is unknown: " + peer);
        } else if (peer.getPort() < 0 || peer.getPort() > 65535) {
            throw new IllegalArgumentException("Invalid port: " + peer.getPort());
        } else if (isLocal(peer)) {
            return;
        }
        eventSink.firePeerDiscovered(torrentId, peer);
    }


    private boolean isLocal(Peer peer) {
        return peer.getInetAddress().equals(localPeer.getInetAddress())
                && localPeer.getPort() == peer.getPort();
    }

    public Peer getLocalPeer() {
        return localPeer;
    }
}
