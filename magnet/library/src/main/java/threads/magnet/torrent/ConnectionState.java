package threads.magnet.torrent;

import androidx.annotation.Nullable;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import threads.magnet.protocol.Cancel;
import threads.magnet.protocol.Request;


public class ConnectionState {

    private final Set<Object> cancelledPeerRequests;
    private final Set<Object> pendingRequests;
    private final Map<Object, CompletableFuture<BlockWrite>> pendingWrites;
    private final Set<Integer> enqueuedPieces;
    private final Queue<Request> requestQueue;
    private volatile boolean interested;
    private volatile boolean peerInterested;
    private volatile boolean choking;
    private volatile boolean peerChoking;
    private Boolean shouldChoke;
    private long lastChoked;
    private Assignment assignment;

    ConnectionState() {
        this.choking = true;
        this.peerChoking = true;
        this.shouldChoke = null;
        this.cancelledPeerRequests = new HashSet<>();
        this.pendingRequests = new HashSet<>();
        this.pendingWrites = new HashMap<>();

        this.enqueuedPieces = new HashSet<>();
        this.requestQueue = new ArrayDeque<>();

        this.assignment = null;
    }

    /**
     * @return true if the local client is interested in (some of the) pieces that remote peer has
     * @since 1.0
     */
    boolean isInterested() {
        return interested;
    }

    /**
     * @see #isInterested()
     * @since 1.0
     */
    void setInterested(boolean interested) {
        this.interested = interested;
    }

    /**
     * @return true if remote peer is interested in (some of the) pieces that the local client has
     * @since 1.0
     */
    boolean isPeerInterested() {
        return peerInterested;
    }

    /**
     * @see #isPeerInterested()
     * @since 1.0
     */
    void setPeerInterested(boolean peerInterested) {
        this.peerInterested = peerInterested;
    }

    /**
     * @return true if the local client is choking the connection
     * @since 1.0
     */
    boolean isChoking() {
        return choking;
    }

    /**
     * @see #isChoking()
     */
    void setChoking(boolean choking) {
        this.choking = choking;
        this.shouldChoke = null;
    }


    @Nullable
    Boolean getShouldChoke() {
        return shouldChoke;
    }

    /**
     * Propose choking/unchoking.
     *
     * @see Choker
     * @since 1.0
     */
    void setShouldChoke(boolean shouldChoke) {
        this.shouldChoke = shouldChoke;
    }

    /**
     * @return Last time connection was choked, 0 if it hasn't been choked yet.
     * Note that connections are initially choked when created.
     * @since 1.0
     */
    long getLastChoked() {
        return lastChoked;
    }

    /**
     * @see #getLastChoked()
     * @since 1.0
     */
    void setLastChoked(long lastChoked) {
        this.lastChoked = lastChoked;
    }


    boolean isPeerChoking() {
        return peerChoking;
    }


    void setPeerChoking(boolean peerChoking) {
        this.peerChoking = peerChoking;
    }


    /**
     * Get keys of block requests, that have been cancelled by remote peer.
     *
     * @return Set of block request keys
     * @see Mapper#buildKey(int, int, int)
     * @since 1.0
     */
    public Set<Object> getCancelledPeerRequests() {
        return cancelledPeerRequests;
    }

    /**
     * Signal that remote peer has cancelled a previously issued block request.
     *
     * @since 1.0
     */
    public void onCancel(Cancel cancel) {
        cancelledPeerRequests.add(Mapper.mapper().buildKey(
                cancel.getPieceIndex(), cancel.getOffset(), cancel.getLength()));
    }

    /**
     * Get keys of block requests, that have been sent to the remote peer.
     *
     * @return Set of block request keys
     * @see Mapper#buildKey(int, int, int)
     * @since 1.0
     */
    public Set<Object> getPendingRequests() {
        return pendingRequests;
    }

    /**
     * Get pending block writes, mapped by keys of corresponding requests.
     *
     * @return Pending block writes, mapped by keys of corresponding requests.
     * @see Mapper#buildKey(int, int, int)
     * @since 1.0
     */
    public Map<Object, CompletableFuture<BlockWrite>> getPendingWrites() {
        return pendingWrites;
    }


    Set<Integer> getEnqueuedPieces() {
        return enqueuedPieces;
    }

    Queue<Request> getRequestQueue() {
        return requestQueue;
    }

    Assignment getCurrentAssignment() {
        return assignment;
    }

    void setCurrentAssignment(Assignment assignment) {
        this.assignment = assignment;
    }

    void removeAssignment() {
        this.assignment = null;
    }
}
