package threads.lite.utils;

import androidx.annotation.NonNull;

import net.luminis.quic.QuicStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.function.Consumer;

import threads.lite.IPFS;
import threads.lite.LogUtils;
import threads.lite.cid.Charsets;
import threads.lite.cid.Multihash;

public class ReaderHandler {
    private static final String TAG = ReaderHandler.class.getSimpleName();

    private final ByteArrayOutputStream temp = new ByteArrayOutputStream();
    public int consumed; // TODO remove
    public int accepted; // TODO remove
    public int tokens; // TODO remove
    private int expectedLength;

    public ReaderHandler() {
        this.expectedLength = 0;
    }


    public static void reading(@NonNull QuicStream quicStream,
                               @NonNull Consumer<String> token,
                               @NonNull Consumer<byte[]> consumer,
                               @NonNull Consumer<Void> finished,
                               @NonNull Consumer<Throwable> throwable) {
        ReaderHandler reader = new ReaderHandler();

        quicStream.setConsumer(streamData -> {
            try {
                reader.load(streamData.data, token, consumer);
                if (streamData.fin) {
                    finished.accept(null);
                }
            } catch (Throwable exception) {
                if (LogUtils.isDebug()) {
                    LogUtils.error(TAG, "ExpectedLength " + reader.expectedLength + " Consumed " +
                            reader.consumed + " Accepted " + reader.accepted + " Tokens " + reader.tokens +
                            " Content Length " + reader.temp.size() + " " + exception.getMessage());
                }
                throwable.accept(exception);
            }
        });
    }

    public static void reading(@NonNull QuicStream quicStream,
                               @NonNull Consumer<String> token,
                               @NonNull Consumer<byte[]> consumer,
                               @NonNull Consumer<Throwable> throwable) {
        reading(quicStream, token, consumer, (fin) -> {
        }, throwable);
    }

    private static int copy(InputStream source, OutputStream sink, int length) throws IOException {
        int nread = 0;
        byte[] buf = new byte[length];
        int n;
        while ((n = source.read(buf)) > 0) {
            sink.write(buf, 0, n);
            nread += n;
            if (nread == length) {
                break;
            }
        }
        return nread;
    }

    private static int copy(InputStream source, OutputStream sink) throws IOException {
        int nread = 0;
        byte[] buf = new byte[4096];
        int n;
        while ((n = source.read(buf)) > 0) {
            sink.write(buf, 0, n);
            nread += n;
        }
        return nread;
    }

    @NonNull
    @Override
    public String toString() {
        return "DataHandler{" +
                ", temp=" + temp +
                ", expectedLength=" + expectedLength +
                '}';
    }

    private void iteration(@NonNull Consumer<String> tokenConsumer,
                           @NonNull Consumer<byte[]> dataConsumer)
            throws IOException {

        // LogUtils.error(TAG, "expected length " + expectedLength + " temp " + temp.size());


        // shortcut
        if (temp.size() < expectedLength) {
            // no reading required
            return;
        }


        try (InputStream inputStream = new ByteArrayInputStream(temp.toByteArray())) {
            expectedLength = (int) Multihash.readVarint(inputStream);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            int read = copy(inputStream, outputStream, expectedLength);

            if (read == expectedLength) {
                // token found
                byte[] tokenData = outputStream.toByteArray();
                // expected to be for a token
                if (tokenData[0] == '/' && tokenData[read - 1] == '\n') {
                    String token = new String(tokenData, Charsets.UTF_8);
                    token = token.substring(0, read - 1);
                    tokenConsumer.accept(token);
                    tokens++;
                } else if (tokenData[0] == 'n' && tokenData[1] == 'a' && tokenData[read - 1] == '\n') {
                    LogUtils.debug(TAG, "na token");
                    tokenConsumer.accept(IPFS.NA);
                    tokens++;
                } else if (tokenData[0] == 'l' && tokenData[1] == 's' && tokenData[read - 1] == '\n') {
                    LogUtils.debug(TAG, "ls token");
                    tokenConsumer.accept(IPFS.LS);
                    tokens++;
                } else {
                    dataConsumer.accept(tokenData);
                    accepted++;
                }
                consumed = consumed + tokenData.length;
                // next iteration
                expectedLength = 0;
                temp.reset();
                int copied = copy(inputStream, temp);
                if (copied == 0) {
                    temp.reset();
                } else {
                    iteration(tokenConsumer, dataConsumer);
                }
            }
        }
    }

    public void load(@NonNull byte[] data,
                     @NonNull Consumer<String> tokenConsumer,
                     @NonNull Consumer<byte[]> dataConsumer)
            throws IOException {

        temp.write(data);

        iteration(tokenConsumer, dataConsumer);
    }

    public void clear() {
        expectedLength = 0;
        try {
            temp.close();
        } catch (Throwable throwable) {
            LogUtils.error(TAG, throwable);
        }
    }

}
