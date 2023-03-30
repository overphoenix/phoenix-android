package threads.lite.bitswap;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.luminis.quic.QuicConnection;
import net.luminis.quic.QuicStream;

import java.io.OutputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import bitswap.pb.MessageOuterClass;
import threads.lite.IPFS;
import threads.lite.LogUtils;
import threads.lite.cid.Cid;
import threads.lite.core.Closeable;
import threads.lite.core.ClosedException;
import threads.lite.format.Block;
import threads.lite.format.BlockStore;
import threads.lite.host.LiteHost;
import threads.lite.utils.DataHandler;
import threads.lite.utils.ReaderHandler;


public class BitSwap implements Exchange {

    private static final String TAG = BitSwap.class.getSimpleName();

    @NonNull
    private final BitSwapManager bitSwapManager;
    @NonNull
    private final BitSwapEngine engine;

    public BitSwap(@NonNull BlockStore blockstore, @NonNull LiteHost host) {
        bitSwapManager = new BitSwapManager(this, blockstore, host);
        engine = new BitSwapEngine(blockstore, host.self());
    }

    @Nullable
    @Override
    public Block getBlock(@NonNull Closeable closeable, @NonNull Cid cid, boolean root) throws ClosedException {
        return bitSwapManager.getBlock(closeable, cid, root);
    }

    @Override
    public void preload(@NonNull Closeable closeable, @NonNull List<Cid> cids) {
        bitSwapManager.loadBlocks(closeable, cids);
    }

    @Override
    public void reset() {
        bitSwapManager.reset();
    }


    public void receiveMessage(@NonNull QuicConnection conn, @NonNull BitSwapMessage bsm) {

        receiveConnMessage(conn, bsm);

        if (IPFS.BITSWAP_ENGINE_ACTIVE) {
            try {
                BitSwapMessage msg = engine.messageReceived(bsm);
                if (msg != null) {
                    QuicStream stream = conn.createStream(true, IPFS.CREATE_STREAM_TIMEOUT,
                            TimeUnit.SECONDS);
                    OutputStream outputStream = stream.getOutputStream();

                    outputStream.write(DataHandler.writeToken(
                            IPFS.STREAM_PROTOCOL, IPFS.BITSWAP_PROTOCOL));
                    outputStream.write(DataHandler.encode(msg.ToProtoV1()));
                    outputStream.close();
                }
            } catch (Throwable throwable) {
                LogUtils.error(TAG, throwable);
            }
        }


    }


    private void receiveConnMessage(@NonNull QuicConnection conn, @NonNull BitSwapMessage bsm) {

        LogUtils.debug(TAG, "ReceiveMessage " +
                conn.getRemoteAddress().toString());

        List<Block> wanted = bsm.Blocks();
        List<Cid> haves = bsm.Haves();
        if (wanted.size() > 0 || haves.size() > 0) {
            for (Block block : wanted) {
                LogUtils.info(TAG, "Block Received " + block.getCid().String() + " " +
                        conn.getRemoteAddress().toString());
                bitSwapManager.blockReceived(block);
            }

            bitSwapManager.haveReceived(conn, haves);
        }

    }

    void sendHaveMessage(@NonNull QuicConnection conn, @NonNull List<Cid> haves) {
        sendHaves(conn, haves, (bsm) -> receiveMessage(conn, bsm));
    }


    private void sendHaves(@NonNull QuicConnection conn, @NonNull List<Cid> haves,
                           @NonNull Consumer<BitSwapMessage> consumer) {
        if (haves.size() == 0) {
            return;
        }

        int priority = Integer.MAX_VALUE;

        BitSwapMessage message = BitSwapMessage.New(false);

        for (Cid c : haves) {

            // Broadcast wants are sent as want-have
            MessageOuterClass.Message.Wantlist.WantType wantType =
                    MessageOuterClass.Message.Wantlist.WantType.Have;

            message.AddEntry(c, priority, wantType, false);

            priority--;
        }

        if (message.Empty()) {
            return;
        }

        writeMessage(conn, message, consumer);


    }

    void sendWantsMessage(@NonNull QuicConnection conn, @NonNull List<Cid> wants) {
        sendWants(conn, wants, (bsm) -> receiveMessage(conn, bsm));
    }


    public void sendWants(@NonNull QuicConnection conn, @NonNull List<Cid> wants,
                          @NonNull Consumer<BitSwapMessage> consumer) {

        if (wants.size() == 0) {
            return;
        }
        BitSwapMessage message = BitSwapMessage.New(false);

        int priority = Integer.MAX_VALUE;

        for (Cid c : wants) {

            message.AddEntry(c, priority,
                    MessageOuterClass.Message.Wantlist.WantType.Block, true);

            priority--;
        }

        if (message.Empty()) {
            return;
        }

        writeMessage(conn, message, consumer);

    }

    public void writeMessage(@NonNull QuicConnection conn,
                             @NonNull BitSwapMessage message,
                             @NonNull Consumer<BitSwapMessage> consumer) {

        if (IPFS.BITSWAP_REQUEST_ACTIVE) {
            try {
                QuicStream quicStream = conn.createStream(true,
                        IPFS.CREATE_STREAM_TIMEOUT, TimeUnit.SECONDS);

                OutputStream outputStream = quicStream.getOutputStream();
                outputStream.write(DataHandler.writeToken(
                        IPFS.STREAM_PROTOCOL, IPFS.BITSWAP_PROTOCOL));


                // TODO rethink done (just simple write the stream,
                // and then we go)
                CompletableFuture<Boolean> done = new CompletableFuture<>();
                ReaderHandler.reading(quicStream,
                        (token) -> {
                            if (!Arrays.asList(IPFS.STREAM_PROTOCOL, IPFS.BITSWAP_PROTOCOL)
                                    .contains(token)) {
                                done.completeExceptionally(
                                        new Exception("Token " + token + " not supported"));
                                return;
                            }
                            try {
                                if (Objects.equals(token, IPFS.BITSWAP_PROTOCOL)) {
                                    outputStream.write(DataHandler.encode(message.ToProtoV1()));
                                    outputStream.close();
                                }
                            } catch (Throwable throwable) {
                                done.completeExceptionally(throwable);
                            }
                        }, (data) -> {
                            try {
                                consumer.accept(BitSwapMessage.newMessageFromProto(
                                        MessageOuterClass.Message.parseFrom(data)));
                                done.complete(true);
                            } catch (Throwable throwable) {
                                done.completeExceptionally(throwable);
                            }

                        }, (fin) -> done.complete(true),
                        done::completeExceptionally);

            } catch (Throwable throwable) {
                LogUtils.error(TAG, throwable.getClass().getSimpleName() +
                        " : " + throwable.getMessage());
            }
        }
    }
}

