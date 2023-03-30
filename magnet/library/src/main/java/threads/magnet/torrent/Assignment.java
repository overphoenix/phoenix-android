package threads.magnet.torrent;

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Queue;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

import threads.magnet.Settings;
import threads.magnet.data.Bitfield;
import threads.magnet.net.ConnectionKey;

class Assignment {

    // TODO: change this to a configurable setting?
    private static final int MAX_SIMULTANEOUSLY_ASSIGNED_PIECES = 3;
    private final Duration limit = Settings.maxPieceReceivingTime;
    private final ConnectionKey connectionKey;
    private final PieceSelector selector;
    private final PieceStatistics pieceStatistics;
    private final Assignments assignments;

    private final Queue<Integer> pieces;
    private ConnectionState connectionState;
    private long started;
    private long checked;
    private boolean aborted;

    Assignment(ConnectionKey connectionKey, PieceSelector selector,
               PieceStatistics pieceStatistics, Assignments assignments) {
        this.connectionKey = connectionKey;
        this.selector = selector;
        this.pieceStatistics = pieceStatistics;
        this.assignments = assignments;

        this.pieces = new ArrayDeque<>();

        claimPiecesIfNeeded();
    }

    private static void shuffle(int[] arr) {
        Random rnd = ThreadLocalRandom.current();
        for (int k = arr.length - 1; k > 0; k--) {
            int i = rnd.nextInt(k + 1);
            int a = arr[i];
            arr[i] = arr[k];
            arr[k] = a;
        }
    }

    ConnectionKey getConnectionKey() {
        return connectionKey;
    }

    Queue<Integer> getPieces() {
        return pieces;
    }

    private void claimPiecesIfNeeded() {
        if (pieces.size() < MAX_SIMULTANEOUSLY_ASSIGNED_PIECES) {
            Bitfield peerBitfield = pieceStatistics.getPeerBitfield(connectionKey).get();

            // randomize order of pieces to keep the number of pieces
            // requested from different peers at the same time to a minimum
            int[] requiredPieces = selector.getNextPieces(pieceStatistics).toArray();
            if (assignments.isEndgame()) {
                shuffle(requiredPieces);
            }

            for (int i = 0; i < requiredPieces.length && pieces.size() < 3; i++) {
                int pieceIndex = requiredPieces[i];
                if (peerBitfield.isVerified(pieceIndex) && assignments.claim(pieceIndex)) {
                    pieces.add(pieceIndex);
                }
            }
        }
    }

    boolean isAssigned(int pieceIndex) {
        return pieces.contains(pieceIndex);
    }

    Status getStatus() {
        if (started > 0) {
            long duration = System.currentTimeMillis() - checked;
            if (duration > limit.toMillis()) {
                return Status.TIMEOUT;
            }
        }
        return Status.ACTIVE;
    }

    void start(ConnectionState connectionState) {
        if (this.connectionState != null) {
            throw new IllegalStateException("Assignment is already started");
        }
        if (aborted) {
            throw new IllegalStateException("Assignment is aborted");
        }
        this.connectionState = connectionState;
        connectionState.setCurrentAssignment(this);
        started = System.currentTimeMillis();
        checked = started;
    }

    void check() {
        checked = System.currentTimeMillis();
    }

    void finish(Integer pieceIndex) {
        if (pieces.remove(pieceIndex)) {
            assignments.finish(pieceIndex);
            claimPiecesIfNeeded();
        }
    }

    void abort() {
        aborted = true;
        if (connectionState != null) {
            connectionState.removeAssignment();
        }
    }

    enum Status {ACTIVE, TIMEOUT}

}
