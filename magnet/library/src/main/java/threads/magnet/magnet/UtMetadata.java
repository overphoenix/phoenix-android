package threads.magnet.magnet;

import androidx.annotation.NonNull;

import java.util.Objects;
import java.util.Optional;

import threads.magnet.protocol.extended.ExtendedMessage;

public class UtMetadata extends ExtendedMessage {


    private static final String messageTypeField = "msg_type";
    private static final String pieceIndexField = "piece";
    private static final String totalSizeField = "total_size";
    private final Type type;
    private final int pieceIndex;
    private final Integer totalSize;
    private final byte[] data;

    private UtMetadata(Type type, int pieceIndex) {
        this(type, pieceIndex, null, null);
    }

    private UtMetadata(Type type, int pieceIndex, Integer totalSize, byte[] data) {
        if (pieceIndex < 0) {
            throw new IllegalArgumentException("Invalid piece index: " + pieceIndex);
        }
        if (totalSize != null && totalSize <= 0) {
            throw new IllegalArgumentException("Invalid total size: " + totalSize);
        }
        this.type = type;
        this.pieceIndex = pieceIndex;
        this.totalSize = totalSize;
        this.data = data;
    }

    static String messageTypeField() {
        return messageTypeField;
    }

    static String pieceIndexField() {
        return pieceIndexField;
    }

    static String totalSizeField() {
        return totalSizeField;
    }

    /**
     * Create metadata request for a given piece.
     *
     * @param pieceIndex Piece index, non-negative
     * @since 1.3
     */
    public static UtMetadata request(int pieceIndex) {
        return new UtMetadata(Type.REQUEST, pieceIndex);
    }

    /**
     * Create metadata response for a given piece.
     *
     * @param pieceIndex Piece index, non-negative
     * @param totalSize  Total size of the threads.torrent's metadata, in bytes
     * @param data       Requested piece's data
     * @since 1.3
     */
    public static UtMetadata data(int pieceIndex, int totalSize, byte[] data) {
        return new UtMetadata(Type.DATA, pieceIndex, totalSize, Objects.requireNonNull(data));
    }

    /**
     * Create metadata rejection response for a given piece.
     *
     * @param pieceIndex Piece index, non-negative
     * @since 1.3
     */
    public static UtMetadata reject(int pieceIndex) {
        return new UtMetadata(Type.REJECT, pieceIndex);
    }

    /**
     * @return Type of this metadata message
     * @since 1.3
     */
    public Type getType() {
        return type;
    }

    /**
     * @return Piece index, non-negative
     * @since 1.3
     */
    public int getPieceIndex() {
        return pieceIndex;
    }

    /**
     * @return Piece's data, when {@link #getType()} is {@link Type#DATA},
     * or {@link Optional#empty()} otherwise
     * @since 1.3
     */
    public Optional<byte[]> getData() {
        return Optional.ofNullable(data);
    }

    /**
     * @return Total size of the threads.torrent's metadata, when {@link #getType()} is {@link Type#DATA},
     * or {@link Optional#empty()} otherwise
     * @since 1.3
     */
    public Optional<Integer> getTotalSize() {
        return Optional.ofNullable(totalSize);
    }

    @NonNull
    @Override
    public String toString() {
        String s = "[" + this.getClass().getSimpleName() + "] type {" + type.name() + "}, piece index {" + pieceIndex + "}";
        if (type == Type.DATA) {
            s += ", data {" + data.length + " bytes}, total size {" + totalSize + "}";
        }
        return s;
    }

    /**
     * @since 1.3
     */
    public enum Type {

        /**
         * @since 1.3
         */
        REQUEST(0),

        /**
         * @since 1.3
         */
        DATA(1),

        /**
         * @since 1.3
         */
        REJECT(2);

        private final int id;

        Type(int id) {
            this.id = id;
        }

        static Type forId(int id) {
            for (Type type : values()) {
                if (type.id == id) {
                    return type;
                }
            }
            throw new IllegalArgumentException("Unknown message id: " + id);
        }

        int id() {
            return id;
        }
    }
}
