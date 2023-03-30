package threads.lite.host;


import androidx.annotation.NonNull;

import com.google.protobuf.ByteString;

import net.luminis.quic.QuicClientConnectionImpl;
import net.luminis.quic.QuicStream;
import net.luminis.quic.server.Server;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import bitswap.pb.MessageOuterClass;
import circuit.pb.Circuit;
import holepunch.pb.Holepunch;
import identify.pb.IdentifyOuterClass;
import threads.lite.IPFS;
import threads.lite.LogUtils;
import threads.lite.cid.Multiaddr;
import threads.lite.cid.PeerId;
import threads.lite.utils.DataHandler;
import threads.lite.utils.ReaderHandler;


public class StreamHandler {
    private static final String TAG = StreamHandler.class.getSimpleName();
    @NonNull
    private final LiteHost host;
    @NonNull
    private final QuicStream quicStream;
    @NonNull
    private final OutputStream outputStream;
    private final AtomicReference<String> tokenProtocol = new AtomicReference<>(null);
    private final AtomicReference<List<Multiaddr>> punchHoleAddrs =
            new AtomicReference<>(Collections.emptyList());
    private final AtomicLong punchHoleStart = new AtomicLong(0L);

    public StreamHandler(@NonNull QuicStream quicStream, @NonNull LiteHost host) {
        this.outputStream = quicStream.getOutputStream();
        this.quicStream = quicStream;
        this.host = host;
        reading();
        LogUtils.debug(TAG, "Instance" + " StreamId " + quicStream.getStreamId() +
                " Connection " + quicStream.getConnection().getRemoteAddress().toString());
    }


    public void exceptionCaught(@NonNull Throwable cause) {
        LogUtils.error(TAG, "Error" + " StreamId " + quicStream.getStreamId() +
                " Connection " + quicStream.getConnection().getRemoteAddress().toString() + " " + cause);
    }

    protected void reading() {

        ReaderHandler.reading(quicStream, (token) -> {
            try {
                tokenProtocol.set(token);
                switch (token) {
                    /* NOT YET SUPPORTED
                    case IPFS.RELAY_PROTOCOL_HOP:
                        outputStream.write(DataHandler.writeToken(IPFS.RELAY_PROTOCOL_HOP));
                        break;
                    */
                    /* NOT YET SUPPORTED
                    case IPFS.PING_PROTOCOL:
                        outputStream.write(DataHandler.writeToken(IPFS.PING_PROTOCOL));
                        outputStream.close();
                        break;*/
                    case IPFS.RELAY_PROTOCOL_STOP:
                        outputStream.write(DataHandler.writeToken(IPFS.RELAY_PROTOCOL_STOP));
                        break;
                    case IPFS.RELAY_PROTOCOL_HOLE_PUNCH:
                        outputStream.write(DataHandler.writeToken(IPFS.RELAY_PROTOCOL_HOLE_PUNCH));
                        break;
                    case IPFS.STREAM_PROTOCOL:
                        outputStream.write(DataHandler.writeToken(IPFS.STREAM_PROTOCOL));
                        break;
                    case IPFS.PUSH_PROTOCOL:
                        outputStream.write(DataHandler.writeToken(IPFS.PUSH_PROTOCOL));
                        break;
                    case IPFS.BITSWAP_PROTOCOL:
                        outputStream.write(DataHandler.writeToken(IPFS.BITSWAP_PROTOCOL));
                        break;
                    case IPFS.IDENTITY_PROTOCOL:
                        outputStream.write(DataHandler.writeToken(IPFS.IDENTITY_PROTOCOL));

                        IdentifyOuterClass.Identify response =
                                host.createIdentity(
                                        quicStream.getConnection().getRemoteAddress());

                        outputStream.write(DataHandler.encode(response));
                        outputStream.close();
                        break;
                    case IPFS.NA:
                        outputStream.close();
                        break;
                    default:
                        LogUtils.debug(TAG, "Ignore " + token +
                                " StreamId " + quicStream.getStreamId());
                        outputStream.write(DataHandler.writeToken(IPFS.NA));
                        outputStream.close(); // todo
                }
            } catch (Throwable throwable) {
                throw new RuntimeException(throwable);
            }
        }, (message) -> {
            try {
                String protocol = tokenProtocol.get();
                if (protocol != null) {
                    switch (protocol) {
                        case IPFS.BITSWAP_PROTOCOL: {
                            host.message(quicStream.getConnection(),
                                    MessageOuterClass.Message.parseFrom(message));
                            outputStream.close();
                            break;
                        }
                        case IPFS.PUSH_PROTOCOL: {
                            host.push(quicStream.getConnection(), message);
                            outputStream.close();
                            break;
                        }
                        case IPFS.RELAY_PROTOCOL_HOLE_PUNCH: {
                            Holepunch.HolePunch holePunch = Holepunch.HolePunch.parseFrom(message);
                            Objects.requireNonNull(holePunch);

                            // Upon receiving the Sync, A immediately dials the address to B.
                            if (holePunch.getType() == Holepunch.HolePunch.Type.SYNC) {

                                long start = punchHoleStart.getAndSet(0L);
                                List<Multiaddr> obsDial =
                                        punchHoleAddrs.getAndSet(Collections.emptyList());
                                if (start > 0 && !obsDial.isEmpty()) {
                                    long rtt = System.currentTimeMillis() - start;

                                    if (quicStream.getConnection() instanceof QuicClientConnectionImpl) {
                                        LogUtils.debug(TAG, "Incoming Sync Punch Hole " + rtt);
                                        QuicClientConnectionImpl conn =
                                                (QuicClientConnectionImpl) quicStream.getConnection();


                                        Server server = host.getServer();
                                        if (server != null) {
                                            server.holePunchConnect(obsDial);
                                        }
                                    }

                                }
                                outputStream.close();
                            } else if (holePunch.getType() == Holepunch.HolePunch.Type.CONNECT) {
                                LogUtils.debug(TAG, "CONNECT " +
                                        quicStream.getConnection().getRemoteAddress());
                                List<Multiaddr> addresses = new ArrayList<>();
                                List<ByteString> entries = holePunch.getObsAddrsList();
                                if (entries.size() == 0) {
                                    outputStream.close();
                                    break;
                                }

                                for (ByteString entry : entries) {
                                    Multiaddr multiaddr = new Multiaddr(entry.toByteArray());
                                    boolean isRelayConn = multiaddr.isCircuitAddress();
                                    if (!isRelayConn) {
                                        addresses.add(multiaddr);
                                    }
                                }

                                Holepunch.HolePunch.Builder builder = Holepunch.HolePunch.newBuilder()
                                        .setType(Holepunch.HolePunch.Type.CONNECT);

                                for (Multiaddr ma : host.listenAddresses(false)) {
                                    builder.addObsAddrs(ByteString.copyFrom(ma.getBytes()));
                                }

                                punchHoleAddrs.set(addresses);
                                punchHoleStart.set(System.currentTimeMillis());

                                outputStream.write(DataHandler.encode(builder.build()));

                            } else {
                                outputStream.close();
                            }
                            break;
                        }
                        /* NOT YET SUPPORTED
                        case IPFS.RELAY_PROTOCOL_HOP: {
                                    Circuit.HopMessage hopMessage = Circuit.HopMessage.parseFrom(message);
                                    Objects.requireNonNull(hopMessage);

                                    outputStream.write(DataHandler.encode(Circuit.HopMessage.newBuilder()
                                            .setType(Circuit.HopMessage.Type.STATUS)
                                            .setStatus(Circuit.Status.PERMISSION_DENIED)
                                            .build()));

                                    break;
                        }*/
                        case IPFS.RELAY_PROTOCOL_STOP: {
                            LogUtils.debug(TAG, "STOP " + quicStream.getConnection().getRemoteAddress());
                            Circuit.StopMessage stopMessage = Circuit.StopMessage.parseFrom(message);
                            Objects.requireNonNull(stopMessage);

                            if (stopMessage.hasPeer()) {
                                Circuit.Peer client = stopMessage.getPeer();
                                PeerId clientId = new PeerId(client.getId().toByteArray());

                                boolean statusOk = true;
                                if (IPFS.RELAY_PROTOCOL_STOP_ONLY_SWARM_SUPPORT) {
                                    statusOk = host.swarmContains(clientId);
                                }
                                Circuit.StopMessage.Builder builder =
                                        Circuit.StopMessage.newBuilder()
                                                .setType(Circuit.StopMessage.Type.STATUS);
                                if (statusOk) {
                                    builder.setStatus(Circuit.Status.OK);
                                } else {
                                    builder.setStatus(Circuit.Status.PERMISSION_DENIED);
                                }
                                outputStream.write(DataHandler.encode(builder.build()));
                            }
                            break;
                        }
                        default:
                            throw new RuntimeException("StreamHandler invalid protocol");
                    }
                } else {
                    throw new RuntimeException("StreamHandler invalid protocol");
                }
            } catch (Throwable throwable) {
                throw new RuntimeException(throwable);
            }
        }, (fin) -> {
        }, this::exceptionCaught);

    }


}
