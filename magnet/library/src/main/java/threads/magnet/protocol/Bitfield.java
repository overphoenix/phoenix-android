package threads.magnet.protocol;

public final class Bitfield implements Message {

    private final byte[] bitfield;

    /**
     * @since 1.0
     */
    public Bitfield(byte[] bitfield) {
        this.bitfield = bitfield;
    }

    /**
     * @since 1.0
     */
    public byte[] getBitfield() {
        return bitfield;
    }

    @Override
    public String toString() {
        return "[" + this.getClass().getSimpleName() + "] bitfield {" + bitfield.length + " bytes}";
    }

    @Override
    public Integer getMessageId() {
        return StandardBittorrentProtocol.BITFIELD_ID;
    }
}
