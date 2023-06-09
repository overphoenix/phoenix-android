package threads.lite;


import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertTrue;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import net.luminis.quic.QuicConnection;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import threads.lite.cid.Cid;
import threads.lite.cid.Multiaddr;
import threads.lite.cid.PeerId;
import threads.lite.relay.Reservation;


@RunWith(AndroidJUnit4.class)
public class IpfsBootstrapTest {
    private static final String TAG = IpfsBootstrapTest.class.getSimpleName();
    private static Context context;

    @BeforeClass
    public static void setup() {
        context = ApplicationProvider.getApplicationContext();
    }


    //@Test
    public void test_idle() throws InterruptedException, IOException {

        IPFS ipfs = TestEnv.getTestInstance(context);

        for (Multiaddr ma : ipfs.listenAddresses()) {
            LogUtils.error(TAG, ma.toString());
        }

        Cid cid = ipfs.storeText("moin moin moin moin");
        LogUtils.error(TAG, "ipfs get /ipfs/" + cid.String());


        /*
        PeerId pc = PeerId.fromBase58("12D3KooWSzeLJzRsmCaA7gPaC9tyJ2qgyfyRyYyo7nqx9ey4DQiw");
        ipfs.addMultiAddress(pc, new Multiaddr("/ip4/192.168.43.171/udp/4001/quic"));
        QuicConnection result = ipfs.connect(pc, IPFS.CONNECT_TIMEOUT, IPFS.GRACE_PERIOD,
                IPFS.MAX_STREAMS, true, false);
        assertNotNull(result);*/


        Thread.sleep(TimeUnit.MINUTES.toMillis(15));
    }

    @Test
    public void test_bootstrap_hops() throws InterruptedException {

        IPFS ipfs = TestEnv.getTestInstance(context);

        int timeInMinutes = 1; // make higher for long run

        assertTrue(ipfs.hasReservations());

        int numReservation = ipfs.numReservations();

        for (Multiaddr ma : ipfs.listenAddresses()) {
            LogUtils.error(TAG, ma.toString());
        }

        // test 15 minutes
        for (int i = 0; i < timeInMinutes; i++) {
            Thread.sleep(TimeUnit.MINUTES.toMillis(1));

            ConcurrentHashMap<PeerId, Reservation> reservations = ipfs.reservations();
            for (Reservation reservation : reservations.values()) {
                LogUtils.error(TAG, "Expire in minutes " + reservation.expireInMinutes()
                        + " " + reservation);


                PeerId relayId = reservation.getPeerId();
                QuicConnection conn = ipfs.getHost().getConnection(relayId);
                assertNotNull(conn);
                assertTrue(conn.isConnected());
            }

            assertEquals(numReservation, ipfs.numReservations());
        }

    }

}
