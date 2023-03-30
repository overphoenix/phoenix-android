package threads.magnet.torrent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import threads.magnet.Settings;
import threads.magnet.IProduces;
import threads.magnet.data.Bitfield;
import threads.magnet.data.ChunkDescriptor;
import threads.magnet.data.DataDescriptor;
import threads.magnet.protocol.Cancel;
import threads.magnet.protocol.InvalidMessageException;
import threads.magnet.protocol.Message;
import threads.magnet.protocol.Request;

public class RequestProducer implements IProduces {

    private final int maxOutstandingRequests;
    private final Bitfield bitfield;
    private final List<ChunkDescriptor> chunks;

    public RequestProducer(DataDescriptor dataDescriptor) {
        this.bitfield = dataDescriptor.getBitfield();
        this.chunks = dataDescriptor.getChunkDescriptors();
        this.maxOutstandingRequests = Settings.maxOutstandingRequests;
    }

    @Override
    public void produce(Consumer<Message> messageConsumer, MessageContext context) {


        ConnectionState connectionState = context.getConnectionState();

        Assignment assignment = connectionState.getCurrentAssignment();
        if (assignment == null) {
            resetConnection(connectionState, messageConsumer);
            return;
        }

        Queue<Integer> assignedPieces = assignment.getPieces();
        if (assignedPieces.isEmpty()) {
            resetConnection(connectionState, messageConsumer);
            return;
        } else {
            List<Integer> finishedPieces = null;
            for (Integer assignedPiece : assignedPieces) {
                if (bitfield.isComplete(assignedPiece)) {
                    if (finishedPieces == null) {
                        finishedPieces = new ArrayList<>(assignedPieces.size() + 1);
                    }
                    // delay removing piece from assignments to avoid CME
                    finishedPieces.add(assignedPiece);
                } else if (connectionState.getEnqueuedPieces().add(assignedPiece)) {
                    addRequestsToQueue(connectionState, assignedPiece);
                }
            }
            if (finishedPieces != null) {
                finishedPieces.forEach(finishedPiece -> {
                    assignment.finish(finishedPiece);
                    connectionState.getEnqueuedPieces().remove(finishedPiece);

                });
            }
        }

        Queue<Request> requestQueue = connectionState.getRequestQueue();
        while (!requestQueue.isEmpty() && connectionState.getPendingRequests().size() <= maxOutstandingRequests) {
            Request request = requestQueue.poll();
            Object key = Mapper.mapper().buildKey(request.getPieceIndex(), request.getOffset(), request.getLength());
            messageConsumer.accept(request);
            connectionState.getPendingRequests().add(key);
        }
    }

    private void resetConnection(ConnectionState connectionState, Consumer<Message> messageConsumer) {
        connectionState.getRequestQueue().clear();
        connectionState.getEnqueuedPieces().clear();
        connectionState.getPendingRequests().forEach(r -> Mapper.decodeKey(r).
                ifPresent(key -> messageConsumer.accept(new Cancel(key.getPieceIndex(),
                        key.getOffset(), key.getLength()))));
        connectionState.getPendingRequests().clear();
        connectionState.getPendingWrites().clear();
    }

    private void addRequestsToQueue(ConnectionState connectionState, Integer pieceIndex) {
        List<Request> requests = buildRequests(pieceIndex).stream()
                .filter(request -> {
                    Object key = Mapper.mapper().buildKey(
                            request.getPieceIndex(), request.getOffset(), request.getLength());
                    if (connectionState.getPendingRequests().contains(key)) {
                        return false;
                    }

                    CompletableFuture<BlockWrite> future = connectionState.getPendingWrites().get(key);
                    if (future == null) {
                        return true;
                    } else if (!future.isDone()) {
                        return false;
                    }

                    BlockWrite block = future.getNow(null);
                    boolean failed = block.getError().isPresent();
                    if (failed) {
                        connectionState.getPendingWrites().remove(key);
                    }
                    return failed;
                }).collect(Collectors.toList());

        Collections.shuffle(requests);

        connectionState.getRequestQueue().addAll(requests);
    }

    private List<Request> buildRequests(int pieceIndex) {
        List<Request> requests = new ArrayList<>();
        ChunkDescriptor chunk = chunks.get(pieceIndex);
        long chunkSize = chunk.getData().length();
        long blockSize = chunk.blockSize();

        for (int blockIndex = 0; blockIndex < chunk.blockCount(); blockIndex++) {
            if (!chunk.isPresent(blockIndex)) {
                int offset = (int) (blockIndex * blockSize);
                int length = (int) Math.min(blockSize, chunkSize - offset);
                try {
                    requests.add(new Request(pieceIndex, offset, length));
                } catch (InvalidMessageException e) {
                    // shouldn't happen
                    throw new RuntimeException("Unexpected error", e);
                }
            }
        }

        return requests;
    }
}
