package threads.magnet.kad;

import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import threads.magnet.Settings;

/**
 * @author Damokles
 */
public class Database {
    private static final int MAX_SAMPLE_COUNT = 20;
    private static final byte[] sessionSecret = new byte[20];

    static {
        ThreadLocalUtils.getThreadLocalRandom().nextBytes(sessionSecret);
    }

    private final AtomicLong timestampCurrent = new AtomicLong();
    private final ConcurrentMap<Key, PeersSeeds> items;
    private volatile long timestampPrevious;
    private volatile byte[] samples = new byte[0];

    Database() {
        items = new ConcurrentHashMap<>(3000);
    }

    private static void fill(List<DBItem> target, PeerAddressDBItem[] source, int max) {
        if (source.length == 0)
            return;

        if (source.length < max - target.size()) {
            // copy whole
            target.addAll(Arrays.asList(source));
        } else {
            // sample random sublist
            int offset = ThreadLocalRandom.current().nextInt(source.length);

            for (int i = 0; i < source.length && target.size() < max; i++) {
                PeerAddressDBItem toInsert = source[(i + offset) % source.length];
                target.add(toInsert);
            }

        }


    }

    /**
     * Store an entry in the database
     *
     * @param key The key
     * @param dbi The DBItem to store
     */
    public void store(Key key, PeerAddressDBItem dbi) {

        items.compute(key, (k, v) -> {
            if (v != null) {
                v.add(dbi);
                return v;
            }

            return new PeersSeeds(dbi.seed ? new PeerAddressDBItem[]{dbi} : ItemSet.NO_ITEMS, dbi.seed ? ItemSet.NO_ITEMS : new PeerAddressDBItem[]{dbi});
        });
    }

    /**
     * Get max_entries items from the database, which have the same key, items
     * are taken randomly from the list. If the key is not present no items will
     * be returned, if there are fewer then max_entries items for the key, all
     * entries will be returned
     *
     * @param key         The key to search for
     * @param max_entries The maximum number entries
     */
    List<DBItem> sample(Key key, int max_entries, boolean preferPeers) {
        PeersSeeds keyEntry;
        PeerAddressDBItem[] seedSnapshot;
        PeerAddressDBItem[] peerSnapshot;

        keyEntry = items.get(key);
        if (keyEntry == null)
            return null;

        seedSnapshot = keyEntry.seeds.snapshot();
        peerSnapshot = keyEntry.peers.snapshot();

        int lengthSum = peerSnapshot.length + seedSnapshot.length;

        if (lengthSum == 0)
            return null;

        List<DBItem> peerlist = new ArrayList<>(Math.min(max_entries, lengthSum));

        preferPeers &= lengthSum > max_entries;

        PeerAddressDBItem[] source;

        if (preferPeers)
            source = peerSnapshot;
        else {
            // proportional sampling
            source = ThreadLocalRandom.current().nextInt(lengthSum) < peerSnapshot.length ? peerSnapshot : seedSnapshot;
        }

        fill(peerlist, source, max_entries);

        source = source == peerSnapshot ? seedSnapshot : peerSnapshot;

        fill(peerlist, source, max_entries);

        return peerlist;
    }

    BloomFilterBEP33 createScrapeFilter(Key key, boolean seedFilter) {
        PeersSeeds dbl = items.get(key);

        if (dbl == null)
            return null;

        return seedFilter ? dbl.seeds.getFilter() : dbl.peers.getFilter();
    }


    void expire() {

        for (PeersSeeds dbl : items.values()) {
            dbl.expire();
        }

        items.entrySet().removeIf(e -> e.getValue().size() == 0);

        samples = null;

    }

    ByteBuffer samples() {
        byte[] currentSamples = samples;

        if (currentSamples != null)
            return ByteBuffer.wrap(currentSamples);

        List<Key> fullSet = new ArrayList<>(items.keySet());

        Collections.shuffle(fullSet);

        int size = Math.min(MAX_SAMPLE_COUNT, fullSet.size());

        byte[] newSamples = new byte[size * 20];

        ByteBuffer buf = ByteBuffer.wrap(newSamples);

        fullSet.stream().limit(size).forEach(k -> k.toBuffer(buf));
        buf.flip();

        if (size >= MAX_SAMPLE_COUNT)
            samples = newSamples;

        return buf;
    }

    boolean insertForKeyAllowed(Key target) {
        PeersSeeds entries = items.get(target);
        if (entries == null)
            return true;

        int size = Math.max(entries.peers.size(), entries.seeds.size());

        if (size < Settings.MAX_DB_ENTRIES_PER_KEY / 5)
            return true;

        // TODO: send a token if the node requesting it is already in the DB

        if (size >= Settings.MAX_DB_ENTRIES_PER_KEY)
            return false;


        // implement RED to throttle write attempts
        return size < ThreadLocalRandom.current().nextInt(Settings.MAX_DB_ENTRIES_PER_KEY);
    }

    /**
     * Generate a write token, which will give peers write access to the DB.
     *
     * @param ip   The IP of the peer
     * @param port The port of the peer
     * @return A Key
     */
    ByteWrapper genToken(Key nodeId, InetAddress ip, int port, Key lookupKey) {
        updateTokenTimestamps();

        byte[] tdata = new byte[Key.SHA1_HASH_LENGTH + ip.getAddress().length + 2 + 8 + Key.SHA1_HASH_LENGTH + sessionSecret.length];
        // generate a hash of the ip port and the current time
        // should prevent anybody from crapping things up
        ByteBuffer bb = ByteBuffer.wrap(tdata);
        nodeId.toBuffer(bb);
        bb.put(ip.getAddress());
        bb.putShort((short) port);
        bb.putLong(timestampCurrent.get());
        lookupKey.toBuffer(bb);
        bb.put(sessionSecret);

        // shorten 4bytes to not waste packet size
        // the chance of guessing correctly would be 1 : 4 million and only be valid for a single infohash
        byte[] token = Arrays.copyOf(ThreadLocalUtils.getThreadLocalSHA1().digest(tdata), 4);


        return new ByteWrapper(token);
    }

    private void updateTokenTimestamps() {
        long current = timestampCurrent.get();
        long now = System.nanoTime();
        while (TimeUnit.NANOSECONDS.toMillis(now - current) > Settings.TOKEN_TIMEOUT) {
            if (timestampCurrent.compareAndSet(current, now)) {
                timestampPrevious = current;
                break;
            }
            current = timestampCurrent.get();
        }
    }

    /**
     * Check if a received token is OK.
     *
     * @param token The token received
     * @param ip    The ip of the sender
     * @param port  The port of the sender
     * @return true if the token was given to this peer, false other wise
     */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    boolean checkToken(ByteWrapper token, Key nodeId, InetAddress ip, int port, Key lookupKey) {
        updateTokenTimestamps();
        return checkToken(token, nodeId, ip, port, lookupKey, timestampCurrent.get()) || checkToken(token, nodeId, ip, port, lookupKey, timestampPrevious);
    }

    private boolean checkToken(ByteWrapper toCheck, Key nodeId, InetAddress ip, int port, Key lookupKey, long timeStamp) {

        byte[] tdata = new byte[Key.SHA1_HASH_LENGTH + ip.getAddress().length + 2 + 8 + Key.SHA1_HASH_LENGTH + sessionSecret.length];
        ByteBuffer bb = ByteBuffer.wrap(tdata);
        nodeId.toBuffer(bb);
        bb.put(ip.getAddress());
        bb.putShort((short) port);
        bb.putLong(timeStamp);
        bb.put(lookupKey.getHash());
        bb.put(sessionSecret);

        byte[] rawToken = Arrays.copyOf(ThreadLocalUtils.getThreadLocalSHA1().digest(tdata), 4);

        return toCheck.equals(new ByteWrapper(rawToken));
    }

    int getStats() {
        return items.size();
    }

    static class PeersSeeds {
        final ItemSet seeds;
        final ItemSet peers;

        PeersSeeds(PeerAddressDBItem[] seeds, PeerAddressDBItem[] peers) {
            this.seeds = new ItemSet(seeds);
            this.peers = new ItemSet(peers);
        }


        void add(PeerAddressDBItem it) {
            ItemSet removeTarget = it.seed ? peers : seeds;
            ItemSet insertTarget = it.seed ? seeds : peers;

            removeTarget.remove(it);
            insertTarget.add(it);
        }

        void expire() {
            seeds.expire();
            peers.expire();
        }

        int size() {
            return peers.size() + seeds.size();
        }
    }

    public static class ItemSet {
        static final PeerAddressDBItem[] NO_ITEMS = new PeerAddressDBItem[0];


        private volatile PeerAddressDBItem[] items;
        private volatile BloomFilterBEP33 filter = null;

        ItemSet(PeerAddressDBItem[] initial) {
            this.items = initial;
        }

        private void remove(PeerAddressDBItem it) {
            synchronized (this) {
                PeerAddressDBItem[] current = items;

                if (current.length == 0)
                    return;


                int idx = Arrays.asList(current).indexOf(it);
                if (idx < 0) {
                    return;
                }

                PeerAddressDBItem[] newItems = Arrays.copyOf(current, current.length - 1);

                System.arraycopy(current, idx + 1, newItems, idx, newItems.length - idx);

                items = newItems;
                invalidateFilters();
            }
        }


        private void add(PeerAddressDBItem toAdd) {
            synchronized (this) {
                PeerAddressDBItem[] current = items;
                int idx = Arrays.asList(current).indexOf(toAdd);
                if (idx >= 0) {
                    current[idx] = toAdd;
                    return;
                }

                PeerAddressDBItem[] newItems = Arrays.copyOf(current, current.length + 1);
                newItems[newItems.length - 1] = toAdd;
                Collections.shuffle(Arrays.asList(newItems));

                items = newItems;

                // bloom filter supports adding, only deletions need a rebuild.
                BloomFilterBEP33 currentFilter = filter;
                if (currentFilter != null) {
                    synchronized (currentFilter) {
                        currentFilter.insert(toAdd.getInetAddress());
                    }
                }

            }
        }

        PeerAddressDBItem[] snapshot() {
            return items;
        }

        int size() {
            return items.length;
        }

        private void invalidateFilters() {
            filter = null;
        }

        BloomFilterBEP33 getFilter() {
            BloomFilterBEP33 f = filter;
            if (f == null) {
                f = filter = buildFilter();
            }

            return f;
        }

        private BloomFilterBEP33 buildFilter() {
            // also return empty filters. strict interpretation of the spec doesn't allow omission of empty sets
            // can happen if we have seeds but no peeds for example

            BloomFilterBEP33 filter = new BloomFilterBEP33();

            for (PeerAddressDBItem item : items) {
                filter.insert(item.getInetAddress());
            }

            return filter;
        }

        void expire() {
            synchronized (this) {
                long now = System.currentTimeMillis();

                PeerAddressDBItem[] items = this.items;
                PeerAddressDBItem[] newItems = new PeerAddressDBItem[items.length];

                // don't remove all at once -> smears out new registrations on popular keys over time
                int toRemove = Settings.MAX_DB_ENTRIES_PER_KEY / 5;

                int insertPoint = 0;

                for (PeerAddressDBItem e : items) {
                    if (toRemove == 0 || !e.expired(now))
                        newItems[insertPoint++] = e;
                    else
                        toRemove--;
                }

                if (insertPoint != newItems.length) {
                    this.items = Arrays.copyOf(newItems, insertPoint);
                    invalidateFilters();
                }

            }

        }
    }
}
