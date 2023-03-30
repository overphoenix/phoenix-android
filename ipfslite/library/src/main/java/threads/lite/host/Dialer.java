package threads.lite.host;

import androidx.annotation.NonNull;

import net.luminis.quic.QuicClientConnectionImpl;
import net.luminis.quic.QuicConnection;
import net.luminis.quic.TransportParameters;
import net.luminis.quic.Version;

import java.net.ConnectException;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import threads.lite.IPFS;
import threads.lite.LogUtils;
import threads.lite.cid.Multiaddr;
import threads.lite.cid.PeerId;
import threads.lite.holepunch.HolePunchService;

public class Dialer {
    private static final String TAG = Dialer.class.getSimpleName();
    private static int failure = 0;
    private static int success = 0;


    @NonNull
    public static QuicConnection connect(
            @NonNull LiteHost host, @NonNull PeerId peerId, @NonNull List<Multiaddr> multiaddrs,
            int timeout, int maxIdleTimeoutInSeconds,
            int initialMaxStreams, int initialMaxStreamData,
            boolean keepConnection) throws ConnectException {

        CompletableFuture<QuicConnection> done = new CompletableFuture<>();
        if (!multiaddrs.isEmpty()) {
            ExecutorService executor = Executors.newFixedThreadPool(multiaddrs.size());
            for (Multiaddr address : multiaddrs) {

                executor.execute(() -> {
                    try {
                        boolean relayConnection = address.isCircuitAddress();
                        if (relayConnection) {
                            done.complete(Dialer.dialDirect(host, peerId, address,
                                    maxIdleTimeoutInSeconds, initialMaxStreams, initialMaxStreamData,
                                    keepConnection));
                        } else {
                            QuicConnection conn = Dialer.dial(host, peerId, address, timeout,
                                    maxIdleTimeoutInSeconds, initialMaxStreams, initialMaxStreamData,
                                    keepConnection);
                            done.complete(conn);
                        }
                    } catch (Throwable ignore) {
                        // ignore
                    }
                });

            }
            try {
                return done.get(timeout, TimeUnit.SECONDS);
            } catch (Throwable throwable) {
                throw new ConnectException(throwable.getMessage());
            } finally {
                try {
                    executor.shutdownNow();
                } catch (Throwable throwable) {
                    LogUtils.error(TAG, throwable);
                }
            }
        }

        throw new ConnectException("no addresses left");

    }

    @NonNull
    public static QuicConnection dialDirect(
            @NonNull LiteHost host, @NonNull PeerId peerId, @NonNull Multiaddr address,
            int maxIdleTimeoutInSeconds, int initialMaxStreams, int initialMaxStreamData,
            boolean keepConnection) throws ConnectException {

        long start = System.currentTimeMillis();
        boolean run = false;
        try {
            QuicConnection conn = HolePunchService.directConnect(host, peerId, address,
                    maxIdleTimeoutInSeconds, initialMaxStreams, initialMaxStreamData,
                    keepConnection);
            run = true;
            return conn;
        } catch (Throwable throwable) {
            throw new ConnectException("dialDirect failed : " + throwable.getMessage());
        } finally {
            LogUtils.debug(TAG, "Run dialDirect " + run +
                    " Peer " + peerId.toBase58() + " " +
                    address + " " + (System.currentTimeMillis() - start));
        }
    }

    @NonNull
    public static QuicClientConnectionImpl dial(
            @NonNull LiteHost host, @NonNull PeerId peerId, @NonNull Multiaddr address,
            int timeout, int maxIdleTimeoutInSeconds, int initialMaxStreams,
            int initialMaxStreamData, boolean keepConnection) throws ConnectException {


        LiteHostCertificate selfSignedCertificate = host.getSelfSignedCertificate();

        boolean relayConnection = address.isCircuitAddress();
        if (relayConnection) {
            throw new RuntimeException("Relays can not be dialed here");
        }

        long start = System.currentTimeMillis();
        boolean run = false;
        try {
            QuicClientConnectionImpl conn = QuicClientConnectionImpl.newBuilder()
                    .version(Version.IETF_draft_29) // in the future switch to version 1
                    .noServerCertificateCheck()
                    .clientCertificate(selfSignedCertificate.cert())
                    .clientCertificateKey(selfSignedCertificate.key())
                    .host(address.getHost())
                    .port(address.getPort())
                    .build();

            Objects.requireNonNull(conn);

            conn.connect(timeout, IPFS.APRN, new TransportParameters(
                    maxIdleTimeoutInSeconds, initialMaxStreamData,
                    initialMaxStreams, 0), null);

            if (initialMaxStreams > 0) {
                conn.setPeerInitiatedStreamCallback(
                        (quicStream) -> new StreamHandler(quicStream, host));
            }

            if (keepConnection) {
                host.addConnection(peerId, conn);
            }

            run = true;
            return conn;
        } catch (Throwable throwable) {
            throw new ConnectException("failure : " + throwable.getMessage());
        } finally {
            if (run) {
                success++;
            } else {
                failure++;
            }
            LogUtils.debug(TAG, "Run dialClient " + run + " Success " + success + " " +
                    "Failure " + failure +
                    " Peer " + peerId.toBase58() + " " +
                    address + " " + (System.currentTimeMillis() - start));
        }
    }

}
