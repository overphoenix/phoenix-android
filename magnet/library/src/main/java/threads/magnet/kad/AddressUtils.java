package threads.magnet.kad;

import android.annotation.SuppressLint;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.ProtocolFamily;
import java.net.SocketException;
import java.net.StandardProtocolFamily;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import threads.magnet.utils.Arrays;
import threads.magnet.utils.NetMask;


public class AddressUtils {

    private final static byte[] LOCAL_BROADCAST = new byte[]{(byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff};
    private final static NetMask V4_MAPPED;
    private final static NetMask V4_COMPAT = NetMask.fromString("0000::/96");

    static {
        try {
            // ::ffff:0:0/96
            V4_MAPPED = new NetMask(Inet6Address.getByAddress(null, new byte[]{0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, (byte) 0xff, (byte) 0xff, 0x00, 0x00, 0x00, 0x00,}, null), 96);
        } catch (Exception e) {
            throw new Error("should not happen");
        }

    }

    public static boolean isBogon(InetSocketAddress addr) {
        return isBogon(addr.getAddress(), addr.getPort());
    }

    private static boolean isBogon(InetAddress addr, int port) {
        return !(port > 0 && port <= 0xFFFF && isGlobalUnicast(addr));
    }

    private static boolean isTeredo(InetAddress addr) {
        if (!(addr instanceof Inet6Address))
            return false;
        byte[] raw = addr.getAddress();
        return raw[0] == 0x20 && raw[1] == 0x01 && raw[2] == 0x00 && raw[3] == 0x00;
    }

    public static boolean isGlobalUnicast(InetAddress addr) {
        // local identification block
        if (addr instanceof Inet4Address && addr.getAddress()[0] == 0)
            return false;
        // this would be rejected by a socket with broadcast disabled anyway, but filter it to reduce exceptions
        if (addr instanceof Inet4Address && java.util.Arrays.equals(addr.getAddress(), LOCAL_BROADCAST))
            return false;
        if (addr instanceof Inet6Address && (addr.getAddress()[0] & 0xfe) == 0xfc) // fc00::/7
            return false;
        if (addr instanceof Inet6Address && (V4_MAPPED.contains(addr) || V4_COMPAT.contains(addr)))
            return false;
        return !(addr.isAnyLocalAddress() || addr.isLinkLocalAddress() || addr.isLoopbackAddress() || addr.isMulticastAddress() || addr.isSiteLocalAddress());
    }

    public static byte[] packAddress(InetSocketAddress addr) {
        byte[] result = null;

        if (addr.getAddress() instanceof Inet4Address) {
            result = new byte[6];
        }

        if (addr.getAddress() instanceof Inet6Address) {
            result = new byte[18];
        }

        Objects.requireNonNull(result);
        ByteBuffer buf = ByteBuffer.wrap(result);
        buf.put(addr.getAddress().getAddress());
        buf.putChar((char) (addr.getPort() & 0xffff));

        return result;
    }


    public static InetAddress fromBytesVerbatim(byte[] raw) throws UnknownHostException {
        // bypass ipv4 mapped address conversion
        if (raw.length == 16) {
            return Inet6Address.getByAddress(null, raw, null);
        }

        return InetAddress.getByAddress(raw);
    }

    public static InetSocketAddress unpackAddress(byte[] raw) {
        if (raw.length != 6 && raw.length != 18)
            return null;
        ByteBuffer buf = ByteBuffer.wrap(raw);
        byte[] rawIP = new byte[raw.length - 2];
        buf.get(rawIP);
        int port = buf.getChar();
        InetAddress ip;
        try {
            ip = InetAddress.getByAddress(rawIP);
        } catch (UnknownHostException e) {
            return null;
        }
        return new InetSocketAddress(ip, port);
    }


    private static Stream<InetAddress> allAddresses() {
        try {
            return Collections.list(NetworkInterface.getNetworkInterfaces()).stream().filter(iface -> {
                try {
                    return iface.isUp();
                } catch (SocketException e) {
                    e.printStackTrace();
                    return false;
                }
            }).flatMap(iface -> Collections.list(iface.getInetAddresses()).stream());
        } catch (SocketException e) {
            e.printStackTrace();
            return Stream.empty();
        }
    }


    public static Stream<InetAddress> nonlocalAddresses() {
        return allAddresses().filter(addr ->
                !addr.isAnyLocalAddress() && !addr.isLoopbackAddress()
        );
    }


    public static List<InetAddress> getAvailableGloballyRoutableAddrs(Class<? extends InetAddress> type) {

        LinkedList<InetAddress> addrs = new LinkedList<>();

        try {
            for (NetworkInterface iface : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                if (!iface.isUp() || iface.isLoopback())
                    continue;
                for (InterfaceAddress ifaceAddr : iface.getInterfaceAddresses()) {
                    if (type == Inet6Address.class && ifaceAddr.getAddress() instanceof Inet6Address) {
                        Inet6Address addr = (Inet6Address) ifaceAddr.getAddress();
                        // only accept globally reachable IPv6 unicast addresses
                        if (addr.isIPv4CompatibleAddress() || !isGlobalUnicast(addr))
                            continue;

                        // prefer other addresses over teredo
                        if (isTeredo(addr))
                            addrs.addLast(addr);
                        else
                            addrs.addFirst(addr);
                    }

                    if (type == Inet4Address.class && ifaceAddr.getAddress() instanceof Inet4Address) {
                        Inet4Address addr = (Inet4Address) ifaceAddr.getAddress();

                        if (!isGlobalUnicast(addr))
                            continue;

                        addrs.add(addr);
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        addrs.sort((a, b) -> Arrays.compareUnsigned(a.getAddress(), b.getAddress()));


        return addrs;
    }

    public static boolean isValidBindAddress(InetAddress addr) {
        // we don't like them them but have to allow them
        if (addr.isAnyLocalAddress())
            return true;
        try {
            NetworkInterface iface = NetworkInterface.getByInetAddress(addr);
            if (iface == null)
                return false;
            return iface.isUp() && !iface.isLoopback();
        } catch (SocketException e) {
            return false;
        }
    }

    public static InetAddress getAnyLocalAddress(Class<? extends InetAddress> type) {
        try {
            if (type == Inet6Address.class)
                return InetAddress.getByAddress(new byte[16]);
            if (type == Inet4Address.class)
                return InetAddress.getByAddress(new byte[4]);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }

        throw new RuntimeException("this shouldn't happen");
    }

    public static InetAddress getDefaultRoute(Class<? extends InetAddress> type) {
        InetAddress target = null;

        ProtocolFamily family = type == Inet6Address.class ? StandardProtocolFamily.INET6 : StandardProtocolFamily.INET;

        try (DatagramChannel chan = DatagramChannel.open(family)) {
            if (type == Inet4Address.class)
                target = InetAddress.getByAddress(new byte[]{8, 8, 8, 8});
            if (type == Inet6Address.class)
                target = InetAddress.getByName("2001:4860:4860::8888");

            chan.connect(new InetSocketAddress(target, 63));

            InetSocketAddress soa = (InetSocketAddress) chan.getLocalAddress();
            InetAddress local = soa.getAddress();

            if (type.isInstance(local) && !local.isAnyLocalAddress())
                return local;
            return null;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    @SuppressLint("DefaultLocale")
    public static String toString(InetSocketAddress sockAddr) {
        InetAddress addr = sockAddr.getAddress();
        if (addr instanceof Inet6Address)
            return String.format("%41s:%-5d", "[" + addr.getHostAddress() + "]", sockAddr.getPort());
        return String.format("%15s:%-5d", addr.getHostAddress(), sockAddr.getPort());
    }

}
