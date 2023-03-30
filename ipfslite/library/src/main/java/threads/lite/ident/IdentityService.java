package threads.lite.ident;

import androidx.annotation.NonNull;

import com.google.protobuf.ByteString;

import net.luminis.quic.QuicConnection;
import net.luminis.quic.QuicStream;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import identify.pb.IdentifyOuterClass;
import threads.lite.IPFS;
import threads.lite.cid.Multiaddr;
import threads.lite.host.PeerInfo;
import threads.lite.utils.DataHandler;
import threads.lite.utils.ReaderHandler;

public class IdentityService {
    public static final String TAG = IdentityService.class.getSimpleName();

    @NonNull
    public static PeerInfo getPeerInfo(@NonNull QuicConnection conn)
            throws Exception {

        IdentifyOuterClass.Identify identify = IdentityService.getIdentity(conn);
        Objects.requireNonNull(identify);
        return getPeerInfo(identify);
    }

    @NonNull
    private static PeerInfo getPeerInfo(@NonNull IdentifyOuterClass.Identify identify) {

        String agent = identify.getAgentVersion();
        String version = identify.getProtocolVersion();
        Multiaddr observedAddr = null;
        if (identify.hasObservedAddr()) {
            observedAddr = new Multiaddr(identify.getObservedAddr().toByteArray());
        }

        List<String> protocols = new ArrayList<>();
        List<Multiaddr> addresses = new ArrayList<>();
        List<ByteString> entries = identify.getProtocolsList().asByteStringList();
        for (ByteString entry : entries) {
            protocols.add(entry.toStringUtf8());
        }
        entries = identify.getListenAddrsList();
        for (ByteString entry : entries) {
            addresses.add(new Multiaddr(entry.toByteArray()));
        }

        return new PeerInfo(agent, version, addresses, protocols, observedAddr);
    }

    @NonNull
    public static IdentifyOuterClass.Identify getIdentity(@NonNull QuicConnection conn)
            throws Exception {
        return requestIdentity(conn);
    }


    private static IdentifyOuterClass.Identify requestIdentity(
            @NonNull QuicConnection conn) throws Exception {


        QuicStream quicStream = conn.createStream(true,
                IPFS.CREATE_STREAM_TIMEOUT, TimeUnit.SECONDS);
        OutputStream outputStream = quicStream.getOutputStream();

        outputStream.write(DataHandler.writeToken(IPFS.STREAM_PROTOCOL, IPFS.IDENTITY_PROTOCOL));
        outputStream.close();

        CompletableFuture<IdentifyOuterClass.Identify> store = new CompletableFuture<>();

        ReaderHandler.reading(quicStream,
                (token) -> {
                    if (!Arrays.asList(IPFS.STREAM_PROTOCOL, IPFS.IDENTITY_PROTOCOL).contains(token)) {
                        store.completeExceptionally(
                                new RuntimeException("Token " + token + " not supported"));
                    }
                },
                (data) -> {
                    try {
                        store.complete(IdentifyOuterClass.Identify.parseFrom(data));
                    } catch (Throwable throwable) {
                        store.completeExceptionally(throwable);
                    }

                }, store::completeExceptionally);

        return store.get(IPFS.CONNECT_TIMEOUT, TimeUnit.SECONDS);

    }
}
