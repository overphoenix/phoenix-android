package threads.magnet.kad.tasks;

import static java.lang.Math.max;

import androidx.annotation.NonNull;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import threads.magnet.kad.IDMismatchDetector;
import threads.magnet.kad.KBucketEntry;
import threads.magnet.kad.Key;
import threads.magnet.kad.NonReachableCache;
import threads.magnet.kad.RPCCall;
import threads.magnet.kad.RPCState;
import threads.magnet.kad.SpamThrottle;
import threads.magnet.utils.CowSet;

/*
 * Issues:
 *
 * - spurious packet loss
 * - remotes might fake IDs. possibly with collusion.
 * - invalid results
 *   - duplicate IPs
 *   - duplicate IDs
 *   - wrong IDs -> might trip up fake ID detection!
 *   - IPs not belonging to DHT nodes -> DoS abuse
 *
 * Solution:
 *
 * - generally avoid querying an IP more than once
 * - dedup result lists from each node
 * - ignore responses with unexpected IDs. normally this could be abused to silence others, but...
 * - allow duplicate requests if many *separate* sources suggest precisely the same <id, ip, port> tuple
 *
 * -> we can recover from all the above-listed issues because the terminal set of nodes should have some partial agreement about their neighbors
 *
 * TODO:
 *  - global mutual exclusion for in-flight targets. bad peers polluting everyone's routing table are the #1 slowdown for bulk lookups now
 *  - min-heaps (active + penalty box)? assign score once. recalculate as needed
 *
 */
class IterativeLookupCandidates {

    final Predicate<LookupGraphNode> lookupFilter;
    private final Key target;
    // maybe split out call tracking
    private final Map<RPCCall, KBucketEntry> calls;
    private final Map<InetAddress, Set<RPCCall>> callsByIp;
    private final Collection<Object> accepted;
    private final IDMismatchDetector detector;
    private final Map<KBucketEntry, LookupGraphNode> candidates;
    private final boolean allowRetransmits = true;
    private NonReachableCache nonReachableCache;
    private SpamThrottle throttle;

    IterativeLookupCandidates(Key target, IDMismatchDetector detector) {
        this.target = target;
        calls = new ConcurrentHashMap<>();
        callsByIp = new ConcurrentHashMap<>();
        candidates = new ConcurrentHashMap<>();
        accepted = new HashSet<>();
        this.detector = detector;
        lookupFilter = node -> {
            KBucketEntry kbe = node.e;

            if (node.tainted || node.unreachable || node.throttled)
                return false;

            // check if we can do retransmits
            if (!allowRetransmits && !node.calls.isEmpty())
                return false;

            // skip retransmits if we previously got a response but from the wrong socket address
            if (!node.calls.isEmpty() && node.calls.stream().anyMatch(RPCCall::hasSocketMismatch))
                return false;


            InetAddress addr = kbe.getAddress().getAddress();

            if (accepted.contains(addr) || accepted.contains(kbe.getID()))
                return false;

            // only do requests to nodes which have at least one source where the source has not given us lots of bogus candidates
            if (node.sources.size() > 0 && node.sources.stream().noneMatch(source -> source.nonSuccessfulDescendantCalls() < 3))
                return false;

            int dups = 0;

            // also check other calls based on matching IP instead of strictly matching ip+port+id
            Set<RPCCall> byIp = callsByIp.get(addr);
            if (byIp != null) {
                synchronized (byIp) {
                    for (RPCCall c : byIp) {
                        // in flight, not stalled
                        if (c.state() == RPCState.SENT || c.state() == RPCState.UNSENT)
                            return false;

                        // already got a response from that addr that does not match what we would expect from this candidate anyway
                        if (c.state() == RPCState.RESPONDED && !c.getResponse().getID().equals(kbe.getID()))
                            return false;
                        // we don't strictly check the presence of IDs in error messages, so we can't compare those here
                        if (c.state() == RPCState.ERROR)
                            return false;
                        dups++;
                    }
                }
            }
            // log2 scale
            int sources = max(1, node.sources.size() + (node.root ? 1 : 0));
            int scaledSources = 31 - Integer.numberOfLeadingZeros(sources);
            //System.out.println("sd:" + sources + " " + dups);

            return scaledSources >= dups;
        };
    }

    private static Predicate<LookupGraphNode> retransmitFilter(boolean retransmits) {

        return (node) -> node.calls.size() <= 0 || retransmits;

    }

    void setNonReachableCache(NonReachableCache nonReachableCache) {
        this.nonReachableCache = nonReachableCache;
    }

    void setSpamThrottle(SpamThrottle throttle) {
        this.throttle = throttle;
    }

    void addCall(RPCCall c, KBucketEntry kbe) {
        calls.put(c, kbe);
        Set<RPCCall> byIp = callsByIp.computeIfAbsent(c.getRequest().getDestination().getAddress(), k -> new HashSet<>());

        synchronized (byIp) {
            byIp.add(c);
        }


        Objects.requireNonNull(candidates.get(kbe)).addCall(c);
    }

    KBucketEntry acceptResponse(RPCCall c) {
        // we ignore on mismatch, node will get a 2nd chance if sourced from multiple nodes and hasn't sent a successful reply yet
        synchronized (this) {
            if (!c.matchesExpectedID())
                return null;

            KBucketEntry kbe = calls.get(c);
            Objects.requireNonNull(kbe);
            if (!kbe.getVersion().isPresent())
                c.getResponse().getVersion().ifPresent(kbe::setVersion);
            LookupGraphNode node = candidates.get(kbe);
            Objects.requireNonNull(node);
            boolean insertOk = !accepted.contains(kbe.getAddress().getAddress()) && !accepted.contains(kbe.getID());
            if (insertOk) {
                accepted.add(kbe.getAddress().getAddress());
                accepted.add(kbe.getID());
                node.accept();
                return kbe;
            }
            return null;
        }
    }

    void addCandidates(KBucketEntry source, Collection<KBucketEntry> entries) {
        Set<Object> dedup = new HashSet<>();

        LookupGraphNode sourceNode = source != null ? candidates.get(source) : null;

        List<LookupGraphNode> children = new ArrayList<>();

        for (KBucketEntry e : entries) {
            if (!dedup.add(e.getID()) || !dedup.add(e.getAddress().getAddress()))
                continue;


            LookupGraphNode newNode = candidates.compute(e, (kbe, node) -> {
                if (node == null) {
                    node = new LookupGraphNode(kbe);
                    node.root = source == null;
                    node.tainted = detector.isIdInconsistencyExpected(kbe.getAddress(), kbe.getID());
                    if (nonReachableCache != null) {
                        int failures = nonReachableCache.getFailures(kbe.getAddress());
                        node.previouslyFailedCount = failures;
                        // 0-20
                        int rnd = ThreadLocalRandom.current().nextInt(21);
                        // -2 - 19 -> 5% chance to let even the worst stuff still through to keep the counters going up
                        node.unreachable = Math.min(failures - 2, 19) > rnd;
                    }
                    if (throttle != null) {
                        node.throttled = throttle.test(kbe.getAddress().getAddress());
                    }
                }
                if (sourceNode != null)
                    node.addSource(sourceNode);
                return node;
            });

            children.add(newNode);

        }

        if (sourceNode != null)
            sourceNode.addChildren(children);


    }

    Set<KBucketEntry> getSources(KBucketEntry e) {
        return Objects.requireNonNull(candidates.get(e)).sources.
                stream().map(LookupGraphNode::toKbe).collect(Collectors.toSet());
    }

    Comparator<LookupGraphNode> comp() {
        Comparator<KBucketEntry> d = new KBucketEntry.DistanceOrder(target);
        Comparator<LookupGraphNode> s = (a, b) -> b.sources.size() - a.sources.size();
        return Comparator.<LookupGraphNode, KBucketEntry>comparing(n -> n.e, d).thenComparing(s);
    }

    Optional<KBucketEntry> next() {
        synchronized (this) {
            return allCand().sorted(comp()).filter(lookupFilter).findFirst().map(LookupGraphNode::toKbe);
        }
    }

    Optional<KBucketEntry> next2(Predicate<KBucketEntry> postFilter) {
        synchronized (this) {

            // sort + filter + findAny should be faster than filter + min in this case since findAny reduces the invocations of the filter, and that is more expensive than the sorting
            Optional<KBucketEntry> kbe = allCand().sorted(comp()).filter(retransmitFilter(false)).filter(lookupFilter).findFirst().map(node -> node.e).filter(postFilter);

            if (!kbe.isPresent() && allowRetransmits)
                kbe = allCand().sorted(comp()).filter(lookupFilter).filter(retransmitFilter(true)).findFirst().map(node -> node.e).filter(postFilter);

            return kbe;
        }
    }

    Stream<LookupGraphNode> allCand() {
        return candidates.values().stream();
    }

    LookupGraphNode nodeForEntry(KBucketEntry e) {
        return candidates.get(e);

    }

    static class LookupGraphNode {
        final KBucketEntry e;
        final Set<LookupGraphNode> sources = new CopyOnWriteArraySet<>();
        final Set<LookupGraphNode> returnedNodes = new CowSet<>();
        final List<RPCCall> calls = new CopyOnWriteArrayList<>();
        boolean tainted;
        boolean acceptedResponse;
        boolean root;
        int previouslyFailedCount;
        boolean unreachable;
        boolean throttled;

        LookupGraphNode(KBucketEntry kbe) {
            e = kbe;
        }

        void addCall(RPCCall c) {
            calls.add(c);
        }

        void addSource(LookupGraphNode toAdd) {
            sources.add(toAdd);
        }

        boolean callsNotSuccessful() {
            return !calls.isEmpty() && !wasAccepted();
        }

        int nonSuccessfulDescendantCalls() {
            return (int) Math.ceil(returnedNodes.isEmpty() ? 0 : returnedNodes.stream().filter(LookupGraphNode::callsNotSuccessful).mapToDouble(node -> 1.0 / Math.max(node.sources.size(), 1)).sum());
        }

        void addChildren(Collection<LookupGraphNode> toAdd) {
            returnedNodes.addAll(toAdd);
        }

        KBucketEntry toKbe() {
            return e;
        }

        void accept() {
            acceptedResponse = true;
        }

        boolean wasAccepted() {
            return acceptedResponse;
        }

        @Override
        public boolean equals(Object other) {
            if (other instanceof LookupGraphNode) {
                return e.equals(((LookupGraphNode) other).e);
            }
            return false;
        }

        @Override
        public int hashCode() {
            return e.hashCode();
        }

        @NonNull
        @Override
        public String toString() {
            return "LookupNode desc:" + nonSuccessfulDescendantCalls();
        }

    }

}
