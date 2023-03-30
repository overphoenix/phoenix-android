package threads.magnet.net;

import java.io.Closeable;
import java.io.IOException;

import threads.magnet.metainfo.TorrentId;
import threads.magnet.protocol.Message;

public interface PeerConnection extends Closeable {

    /**
     * @return Remote peer
     * @since 1.0
     */
    Peer getRemotePeer();

    /**
     * @since 1.9
     */
    int getRemotePort();

    /**
     * Associate this connection with the given threads.torrent ID.
     *
     * @param torrentId Torrent ID to associate this connection with
     * @return Torrent ID, that this connection was previously associated with, or null
     * @since 1.5
     */
    TorrentId setTorrentId(TorrentId torrentId);

    /**
     * @return Torrent ID, that this connection is associated with, or null
     * @since 1.0
     */
    TorrentId getTorrentId();

    /**
     * Attempt to read an incoming message.
     * <p>
     * Note that implementation may buffer all received data
     * and try to read next message from the buffer,
     * so calling this method does not necessarily imply I/O access.
     *
     * @return Message, or null if there isn't any
     * @since 1.0
     */
    Message readMessageNow();

    /**
     * Attempt to read an incoming message within a specified time interval.
     * Invocation blocks the calling thread until either a message is read
     * or the limit of waiting time is reached.
     * <p>
     * Note that implementation may buffer all received data
     * and try to read next message from the buffer,
     * so calling this method does not necessarily imply I/O access.
     *
     * @return Message, or null if there isn't any
     * @since 1.0
     */
    Message readMessage(long timeout);

    /**
     * Send a message to remote peer.
     *
     * @since 1.0
     */
    void postMessage(Message message) throws IOException;

    /**
     * @return Last time a message was received or sent via this connection
     * @since 1.0
     */
    long getLastActive();

    /**
     * Close the connection without throwing an {@link java.io.IOException}.
     *
     * @since 1.0
     */
    void closeQuietly();

    /**
     * @return true if connection is closed
     * @since 1.0
     */
    boolean isClosed();
}
