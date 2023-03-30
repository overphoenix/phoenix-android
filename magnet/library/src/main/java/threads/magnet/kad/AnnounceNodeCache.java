package threads.magnet.kad;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentSkipListMap;

import threads.magnet.Settings;
import threads.magnet.kad.messages.MessageBase;

public class AnnounceNodeCache {

    private final ConcurrentSkipListMap<Key, CacheAnchorPoint> anchors = new ConcurrentSkipListMap<>();
    private final ConcurrentSkipListMap<Key, CacheBucket> cache = new ConcurrentSkipListMap<>();
    private final RPCCallListener cl = new RPCCallListener() {
        public void onTimeout(RPCCall c) {
            Key nodeId = c.getExpectedID();
            if (nodeId == null)
                return;

            Entry<Key, CacheBucket> targetEntry = cache.floorEntry(nodeId);

            if (targetEntry == null || !targetEntry.getValue().prefix.isPrefixOf(nodeId))
                return;

            // remove an entry if the id matches
            // ignore the removal if we have heard from the node after the request has been issued, it might be a spurious failure
            targetEntry.getValue().entries.removeIf(e -> e.getID().equals(nodeId) &&
                    (e.getLastSeen() < c.getSentTime() || c.getSentTime() == -1));


        }


        public void onResponse(RPCCall c, MessageBase rsp) {
            if (!c.matchesExpectedID())
                return;
            KBucketEntry kbe = new KBucketEntry(rsp.getOrigin(), rsp.getID());
            kbe.signalResponse(c.getRTT());
            add(kbe);
        }
    };

    AnnounceNodeCache() {
        CacheBucket rootBucket = new CacheBucket(new Prefix());
        cache.put(rootBucket.prefix, rootBucket);
    }

    public void register(Key target, boolean isFastLookup) {
        CacheAnchorPoint anchor = new CacheAnchorPoint(target);
        anchor.expirationTime = System.currentTimeMillis() + (isFastLookup ? Settings.ANNOUNCE_CACHE_FAST_LOOKUP_AGE : Settings.ANNOUNCE_CACHE_MAX_AGE);
        anchors.put(target, anchor);
    }

    public RPCCallListener getRPCListener() {
        return cl;
    }

    /*
     * this insert procedure might cause minor inconsistencies (duplicate entries, too-large lists)
     * but those are self-healing under merge/split operations
     */
    private void add(KBucketEntry entryToInsert) {
        Key target = entryToInsert.getID();

        outer:
        while (true) {
            Entry<Key, CacheBucket> targetEntry = cache.floorEntry(target);

            if (targetEntry == null || !targetEntry.getValue().prefix.isPrefixOf(target)) { // split/merge operation ongoing, retry
                Thread.yield();
                continue;
            }

            CacheBucket targetBucket = targetEntry.getValue();

            int size = 0;

            for (KBucketEntry e : targetBucket.entries) {
                size++;
                if (e.getID().equals(entryToInsert.getID())) { // refresh timestamp, this is checked for removals
                    e.mergeInTimestamps(entryToInsert);
                    break outer;
                }
            }

            if (size >= Settings.MAX_CONCURRENT_REQUESTS) {
                // cache entry full, see if we this bucket prefix covers any anchor
                Map.Entry<Key, CacheAnchorPoint> anchorEntry = anchors.ceilingEntry(targetBucket.prefix);

                if (anchorEntry == null || !targetBucket.prefix.isPrefixOf(anchorEntry.getValue())) {
                    // if this bucket is full and cannot be split
                    for (Iterator<KBucketEntry> it = targetBucket.entries.iterator(); it.hasNext(); ) {
                        KBucketEntry kbe = it.next();
                        if (entryToInsert.getRTT() < kbe.getRTT()) {
                            targetBucket.entries.add(entryToInsert);
                            it.remove();
                            break outer;
                        }
                    }
                    break;
                }


                synchronized (targetBucket) {
                    // check for concurrent split/merge
                    if (cache.get(targetBucket.prefix) != targetBucket)
                        continue;
                    // perform split operation
                    CacheBucket lowerBucket = new CacheBucket(targetBucket.prefix.splitPrefixBranch(false));
                    CacheBucket upperBucket = new CacheBucket(targetBucket.prefix.splitPrefixBranch(true));

                    // remove old entry. this leads to a temporary gap in the cache-keyspace!
                    if (!cache.remove(targetEntry.getKey(), targetBucket))
                        continue;

                    cache.put(upperBucket.prefix, upperBucket);
                    cache.put(lowerBucket.prefix, lowerBucket);

                    for (KBucketEntry e : targetBucket.entries)
                        add(e);
                }

                continue;
            }

            targetBucket.entries.add(entryToInsert);
            break;


        }
    }

    public List<KBucketEntry> get(Key target, int targetSize) {
        ArrayList<KBucketEntry> closestSet = new ArrayList<>(2 * targetSize);

        Map.Entry<Key, CacheBucket> ceil = cache.ceilingEntry(target);
        Map.Entry<Key, CacheBucket> floor = cache.floorEntry(target);

        do {
            if (floor != null) {
                closestSet.addAll(floor.getValue().entries);
                floor = cache.lowerEntry(floor.getKey());
            }

            if (ceil != null) {
                closestSet.addAll(ceil.getValue().entries);
                ceil = cache.higherEntry(ceil.getKey());
            }
        } while (closestSet.size() / 2 < targetSize && (floor != null || ceil != null));

        return closestSet;
    }

    void cleanup(long now) {
        // first pass, eject old anchors
        anchors.values().removeIf(cacheAnchorPoint -> now - cacheAnchorPoint.expirationTime > 0);

        Set<Key> seenIDs = new HashSet<>();
        Set<InetAddress> seenIPs = new HashSet<>();

        // 2nd pass, eject old and/or duplicate entries
        for (CacheBucket b : cache.values()) {
            for (Iterator<KBucketEntry> it2 = b.entries.iterator(); it2.hasNext(); ) {
                KBucketEntry kbe = it2.next();
                if (now - kbe.getLastSeen() > Settings.ANNOUNCE_CACHE_MAX_AGE || seenIDs.contains(kbe.getID()) || seenIPs.contains(kbe.getAddress().getAddress()))
                    it2.remove();
                seenIDs.add(kbe.getID());
                seenIPs.add(kbe.getAddress().getAddress());
            }

            // IDs go into the appropriate buckets. no need to check across buckets
            seenIDs.clear();

        }


        // merge buckets that aren't full or don't have anchors

        Entry<Key, CacheBucket> entry = cache.firstEntry();
        if (entry == null)
            return;

        CacheBucket current;
        CacheBucket next = entry.getValue();

        while (true) {
            current = next;

            entry = cache.higherEntry(current.prefix);
            if (entry == null)
                return;
            next = entry.getValue();


            if (!current.prefix.isSiblingOf(next.prefix))
                continue;


            Prefix parent = current.prefix.getParentPrefix();
            Map.Entry<Key, CacheAnchorPoint> anchor = anchors.ceilingEntry(parent);
            if (anchor == null || !parent.isPrefixOf(anchor.getValue()) || current.entries.size() + next.entries.size() < Settings.MAX_CONCURRENT_REQUESTS) {
                synchronized (current) {
                    synchronized (next) {
                        // check for concurrent split/merge
                        if (cache.get(current.prefix) != current || cache.get(next.prefix) != next)
                            continue;

                        cache.remove(current.prefix, current);
                        cache.remove(next.prefix, next);

                        cache.put(parent, new CacheBucket(parent));

                        for (KBucketEntry e : current.entries)
                            add(e);
                        for (KBucketEntry e : next.entries)
                            add(e);
                    }
                }

                // move backwards if possible to cascade merges backwards if necessary
                entry = cache.lowerEntry(current.prefix);
                if (entry == null)
                    continue;
                next = entry.getValue();
            }
        }

    }

    private static class CacheAnchorPoint extends Key {
        long expirationTime;

        CacheAnchorPoint(Key k) {
            super(k);
        }
    }

    private static class CacheBucket {

        final Prefix prefix;
        final ConcurrentLinkedQueue<KBucketEntry> entries = new ConcurrentLinkedQueue<>();

        CacheBucket(Prefix p) {
            prefix = p;
        }
    }


}
