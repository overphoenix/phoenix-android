package threads.lite;


import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertNull;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import net.luminis.quic.QuicConnection;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Set;

import threads.lite.cid.Multiaddr;
import threads.lite.cid.Peer;
import threads.lite.cid.PeerId;
import threads.lite.core.TimeoutCloseable;
import threads.lite.host.PeerInfo;


@SuppressWarnings("SpellCheckingInspection")
@RunWith(AndroidJUnit4.class)
public class IpfsConnectTest {
    private static final String TAG = IpfsConnectTest.class.getSimpleName();

    private static final String DUMMY_PID = "QmVLnkyetpt7JNpjLmZX21q9F8ZMnhBts3Q53RcAGxWH6V";

    private static Context context;

    @BeforeClass
    public static void setup() {
        context = ApplicationProvider.getApplicationContext();
    }


    @Test
    public void swarm_connect() {

        IPFS ipfs = TestEnv.getTestInstance(context);
        PeerId pc = PeerId.fromBase58("QmRxoQNy1gNGMM1746Tw8UBNBF8axuyGkzcqb2LYFzwuXd");

        // TIMEOUT not working
        QuicConnection result = ipfs.connect(pc, IPFS.CONNECT_TIMEOUT, IPFS.GRACE_PERIOD,
                IPFS.MAX_STREAMS, true);
        assertNull(result);

    }

    @Test
    public void test_swarm_connect() {
        IPFS ipfs = TestEnv.getTestInstance(context);

        PeerId relay = ipfs.getPeerId("QmchgNzyUFyf2wpfDMmpGxMKHA3PkC1f3H2wUgbs21vXoz");


        LogUtils.debug(TAG, "Stage 1");

        QuicConnection result = ipfs.connect(relay, IPFS.CONNECT_TIMEOUT, IPFS.GRACE_PERIOD,
                IPFS.MAX_STREAMS, true);
        assertNull(result);

        LogUtils.debug(TAG, "Stage 2");

        result = ipfs.find(relay, IPFS.CONNECT_TIMEOUT,
                IPFS.MAX_STREAMS, true, new TimeoutCloseable(10));
        assertNull(result);

        LogUtils.debug(TAG, "Stage 3");

        relay = ipfs.getPeerId(DUMMY_PID);
        result = ipfs.find(relay, IPFS.CONNECT_TIMEOUT,
                IPFS.MAX_STREAMS, true, new TimeoutCloseable(10));
        assertNull(result);

        LogUtils.debug(TAG, "Stage 4");

    }


    @Test
    public void test_print_swarm_peers() {
        IPFS ipfs = TestEnv.getTestInstance(context);


        Set<Peer> peers = ipfs.getPeers();

        assertNotNull(peers);
        LogUtils.debug(TAG, "Peers : " + peers.size());
        for (Peer peer : peers) {

            try {

                QuicConnection conn = ipfs.connect(peer.getPeerId(), IPFS.CONNECT_TIMEOUT, IPFS.GRACE_PERIOD,
                        IPFS.MAX_STREAMS, true);
                assertNotNull(conn);
                PeerInfo peerInfo = ipfs.getPeerInfo(conn);


                LogUtils.debug(TAG, peerInfo.toString());
                assertNotNull(peerInfo.getAddresses());
                assertNotNull(peerInfo.getAgent());

                Multiaddr observed = peerInfo.getObserved();
                if (observed != null) {
                    LogUtils.debug(TAG, observed.toString());
                }

            } catch (Throwable throwable) {
                LogUtils.debug(TAG, "" + throwable.getClass().getName());
            }

        }

    }


}
