package tech.nagual.phoenix.tools.browser.services;

import android.content.Context;

import androidx.annotation.NonNull;

import java.net.Inet6Address;
import java.net.InetAddress;

import tech.nagual.phoenix.tools.browser.LogUtils;
import threads.lite.IPFS;
import threads.lite.cid.Multiaddr;
import threads.lite.cid.PeerId;

public class LocalConnectService {

    private static final String TAG = LocalConnectService.class.getSimpleName();

    public static void connect(@NonNull Context context, @NonNull PeerId peerId,
                               @NonNull InetAddress host, int port) {


        try {
            IPFS ipfs = IPFS.getInstance(context);

            ipfs.swarmEnhance(peerId);

            String pre = "/ip4";
            if (host instanceof Inet6Address) {
                pre = "/ip6";
            }

            String multiAddress = pre + host + "/udp/" + port + "/quic";

            ipfs.addMultiAddress(peerId, new Multiaddr(multiAddress));

            LogUtils.error(TAG, "Success " + peerId.toBase58() + " " + multiAddress);

            // Not sure to connect TODO
            // ipfs.connect(peerId, IPFS.CONNECT_TIMEOUT, IPFS.KEEP_ALIVE_TIMEOUT,
            //      IPFS.MAX_STREAMS, true, true);

        } catch (Throwable throwable) {
            LogUtils.error(TAG, throwable);
        }
    }

}

