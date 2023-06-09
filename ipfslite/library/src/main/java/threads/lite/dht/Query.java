package threads.lite.dht;

import android.util.Pair;

import androidx.annotation.NonNull;

import java.net.ConnectException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;

import threads.lite.LogUtils;
import threads.lite.cid.Peer;
import threads.lite.core.Closeable;
import threads.lite.core.ClosedException;

public class Query {

    private static final String TAG = Query.class.getSimpleName();

    @NonNull
    private final KadDht dht;
    @NonNull
    private final List<Peer> seedPeers;
    @NonNull
    private final QueryPeerSet queryPeers;
    @NonNull
    private final KadDht.QueryFunc queryFn;
    @NonNull
    private final KadDht.StopFunc stopFn;
    @NonNull
    private final BlockingQueue<QueryUpdate> queue;
    private final int alpha;

    public Query(@NonNull KadDht dht, @NonNull byte[] key, @NonNull List<Peer> seedPeers,
                 @NonNull KadDht.QueryFunc queryFn, @NonNull KadDht.StopFunc stopFn) {
        this.dht = dht;
        this.seedPeers = seedPeers;
        this.queryPeers = QueryPeerSet.create(key);
        this.queryFn = queryFn;
        this.stopFn = stopFn;
        this.alpha = dht.alpha;
        this.queue = new ArrayBlockingQueue<>(alpha);
    }


    public ConcurrentHashMap<Peer, PeerState> constructLookupResult(@NonNull ID target) {

        // extract the top K not unreachable peers
        List<QueryPeerState> qp = queryPeers.getClosestNInStates(dht.bucketSize,
                Arrays.asList(PeerState.PeerHeard, PeerState.PeerWaiting, PeerState.PeerQueried));

        ConcurrentHashMap<Peer, PeerState> res = new ConcurrentHashMap<>();
        List<Peer> peers = new ArrayList<>();
        Map<Peer, PeerState> map = new HashMap<>();
        for (QueryPeerState p : qp) {
            peers.add(p.id);
            map.put(p.id, p.getState());
        }


        PeerDistanceSorter pds = new PeerDistanceSorter(target);
        for (Peer p : peers) {
            pds.appendPeer(p, ID.convertPeerID(p.getPeerId()));
        }

        List<Peer> sorted = pds.sortedList();

        for (Peer peerId : sorted) {
            PeerState peerState = map.get(peerId);
            Objects.requireNonNull(peerState);
            res.put(peerId, peerState);
        }

        return res;
    }


    private void updateState(@NonNull QueryUpdate up) {

        for (Peer p : up.heard) {
            if (Objects.equals(p.getPeerId(), dht.self)) { // don't add self.
                continue;
            }
            queryPeers.tryAdd(p);
        }


        for (Peer p : up.queried) {
            if (Objects.equals(p.getPeerId(), dht.self)) { // don't add self.
                continue;
            }
            PeerState st = queryPeers.getState(p);
            if (st == PeerState.PeerWaiting) {
                queryPeers.setState(p, PeerState.PeerQueried);
            } else {
                throw new RuntimeException("internal state");
            }
        }
        for (Peer p : up.unreachable) {
            if (Objects.equals(p.getPeerId(), dht.self)) { // don't add self.
                continue;
            }
            PeerState st = queryPeers.getState(p);
            if (st == PeerState.PeerWaiting) {
                queryPeers.setState(p, PeerState.PeerUnreachable);
            } else {
                throw new RuntimeException("internal state");
            }
        }
    }

    public void run(@NonNull Closeable ctx) throws ClosedException, InterruptedException {

        QueryUpdate update = new QueryUpdate();
        update.heard.addAll(seedPeers);
        queue.offer(update);

        while (true) {

            QueryUpdate current = queue.take();

            if (ctx.isClosed()) {
                throw new ClosedException();
            }

            updateState(current);

            // calculate the maximum number of queries we could be spawning.
            // Note: NumWaiting will be updated in spawnQuery
            int maxNumQueriesToSpawn = alpha - queryPeers.NumWaiting();

            // termination is triggered on end-of-lookup conditions or starvation of unused peers
            // it also returns the peers we should query next for a maximum of `maxNumQueriesToSpawn` peers.
            Pair<Boolean, List<Peer>> result = isReadyToTerminate(maxNumQueriesToSpawn);

            if (!result.first) {

                // try spawning the queries, if there are no available peers to query then we won't spawn them
                for (Peer queryPeer : result.second) {
                    queryPeers.setState(queryPeer, PeerState.PeerWaiting);
                    new Thread(() -> {
                        try {
                            queryPeer(ctx, queryPeer);
                        } catch (ClosedException ignore) {
                            queue.clear();
                            queue.offer(new QueryUpdate());
                            // nothing to do here (works as expected)
                        } catch (Throwable throwable) {
                            // not expected exception
                            LogUtils.error(TAG, throwable);
                        }
                    }).start();
                }
            } else {
                LogUtils.warning(TAG, "Termination no succes");
                break;
            }
        }
    }


    private void queryPeer(@NonNull Closeable ctx, @NonNull Peer queryPeer) throws ClosedException {

        try {
            if (ctx.isClosed()) {
                throw new ClosedException();
            }
            Set<Peer> newPeers = queryFn.query(ctx, queryPeer);

            // query successful, try to add to routing table
            dht.peerFound(queryPeer, true);

            // process new peers
            List<Peer> saw = new ArrayList<>();
            for (Peer next : newPeers) {
                if (Objects.equals(next.getPeerId(), dht.self)) { // don't add self.
                    continue;
                }
                saw.add(next);
            }

            QueryUpdate update = new QueryUpdate();
            update.heard.addAll(saw);
            update.queried.add(queryPeer);
            queue.offer(update);

        } catch (ClosedException closedException) {
            throw closedException;
        } catch (ConnectException ignore) {
            dht.removeFromRouting(queryPeer);
            QueryUpdate update = new QueryUpdate();
            update.unreachable.add(queryPeer);
            queue.offer(update);
        } catch (Throwable throwable) {
            LogUtils.error(TAG, throwable);

            dht.removeFromRouting(queryPeer);
            QueryUpdate update = new QueryUpdate();
            update.unreachable.add(queryPeer);
            queue.offer(update);
        }


    }


    private boolean isStarvationTermination() {
        boolean result = queryPeers.NumHeard() == 0 && queryPeers.NumWaiting() == 0;
        if (result) {
            LogUtils.error(TAG, "Starvation Termination " + queryPeers.size());
        }
        return result;
    }


    private Pair<Boolean, List<Peer>> isReadyToTerminate(int nPeersToQuery) {

        if (stopFn.stop()) {
            return Pair.create(true, Collections.emptyList());
        }
        if (isStarvationTermination()) {
            return Pair.create(true, Collections.emptyList());
        }

        // The peers we query next should be ones that we have only Heard about.
        List<Peer> peersToQuery = new ArrayList<>();
        List<QueryPeerState> peers = queryPeers.getClosestInStates(
                nPeersToQuery, Collections.singletonList(PeerState.PeerHeard));
        int count = 0;
        for (QueryPeerState p : peers) {
            peersToQuery.add(p.id);
            count++;
            if (count == nPeersToQuery) {
                break;
            }
        }

        return Pair.create(false, peersToQuery);
    }

    public static class QueryUpdate {

        public final List<Peer> queried = new ArrayList<>();
        public final List<Peer> heard = new ArrayList<>();
        public final List<Peer> unreachable = new ArrayList<>();
    }

}
