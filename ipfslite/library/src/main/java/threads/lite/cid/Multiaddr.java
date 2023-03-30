package threads.lite.cid;


import androidx.annotation.NonNull;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Stream;


public class Multiaddr {
    private final byte[] raw;
    private final String address;

    public Multiaddr(String address) {
        this.address = address;
        this.raw = decodeFromString(address);
    }

    public Multiaddr(byte[] raw) {
        this.address = encodeToString(raw); // check validity
        this.raw = raw;
    }

    private static byte[] decodeFromString(String addr) {
        while (addr.endsWith("/"))
            addr = addr.substring(0, addr.length() - 1);
        String[] parts = addr.split("/");
        if (parts[0].length() != 0)
            throw new IllegalStateException("MultiAddress must start with a /");

        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        try {
            for (int i = 1; i < parts.length; ) {
                String part = parts[i++];
                Protocol p = Protocol.get(part);
                p.appendCode(bout);
                if (p.size() == 0)
                    continue;

                String component = p.isTerminal() ?
                        Stream.of(Arrays.copyOfRange(parts, i, parts.length)).reduce("", (a, b) -> a + "/" + b) :
                        parts[i++];
                if (component.length() == 0)
                    throw new IllegalStateException("Protocol requires address, but non provided!");

                bout.write(p.addressToBytes(component));
                if (p.isTerminal())
                    break;
            }
            return bout.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Error decoding multiaddress: " + addr);
        }
    }

    private static String encodeToString(byte[] raw) {
        StringBuilder b = new StringBuilder();
        InputStream in = new ByteArrayInputStream(raw);
        try {
            while (true) {
                int code = (int) Protocol.readVarint(in);
                Protocol p = Protocol.get(code);
                b.append("/" + p.name());
                if (p.size() == 0)
                    continue;

                String addr = p.readAddress(in);
                if (addr.length() > 0)
                    b.append("/" + addr);
            }
        } catch (EOFException ignore) {
            // ignore
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return b.toString();
    }

    @NonNull
    public static Multiaddr transform(@NonNull InetSocketAddress inetSocketAddress) {

        InetAddress inetAddress = inetSocketAddress.getAddress();
        boolean ipv6 = inetAddress instanceof Inet6Address;
        int port = inetSocketAddress.getPort();
        String multiaddress = "";
        if (ipv6) {
            multiaddress = multiaddress.concat("/ip6/");
        } else {
            multiaddress = multiaddress.concat("/ip4/");
        }
        multiaddress = multiaddress + inetAddress.getHostAddress() + "/udp/" + port + "/quic";
        return new Multiaddr(multiaddress);

    }

    public boolean isLocalAddress() {
        try {
            InetAddress inetAddress = InetAddress.getByName(getHost());
            if (inetAddress.isLinkLocalAddress() || (inetAddress.isLoopbackAddress())) {
                return true;
            }
        } catch (Throwable ignore) {
            return false;
        }
        return false;
    }

    public boolean isAnyLocalAddress() {
        try {
            InetAddress inetAddress = InetAddress.getByName(getHost());
            if (inetAddress.isAnyLocalAddress() || inetAddress.isLinkLocalAddress()
                    || (inetAddress.isLoopbackAddress())
                    || (inetAddress.isSiteLocalAddress())) {
                return true;
            }
        } catch (Throwable ignore) {
            return false;
        }
        return false;
    }

    public boolean isSupported() {

        if (has(threads.lite.cid.Protocol.Type.DNSADDR)) {
            return true;
        }
        if (has(threads.lite.cid.Protocol.Type.DNS)) {
            return true;
        }
        if (has(threads.lite.cid.Protocol.Type.DNS4)) {
            return true;
        }
        if (has(threads.lite.cid.Protocol.Type.DNS6)) {
            return true;
        }

        return has(threads.lite.cid.Protocol.Type.QUIC);
    }

    public byte[] getBytes() {
        return Arrays.copyOfRange(raw, 0, raw.length);
    }

    public String getHost() {
        String[] parts = toString().substring(1).split("/");
        if (parts[0].startsWith("ip") || parts[0].startsWith("dns"))
            return parts[1];
        throw new IllegalStateException("This multiaddress doesn't have a host: " + this);
    }

    public int getPort() {
        String[] parts = toString().substring(1).split("/");
        if (parts[2].startsWith("tcp") || parts[2].startsWith("udp"))
            return Integer.parseInt(parts[3]);
        throw new IllegalStateException("This multiaddress doesn't have a port: " + this);
    }

    @NonNull
    @Override
    public String toString() {
        return address;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof Multiaddr))
            return false;
        return Arrays.equals(raw, ((Multiaddr) other).raw);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(raw);
    }

    public String getStringComponent(Protocol.Type type) {
        String[] tokens = address.split("/");
        for (int i = 0; i < tokens.length; i++) {
            String token = tokens[i];
            if (Objects.equals(token, type.name)) {
                return tokens[i + 1];
            }
        }
        return null;
    }

    public boolean has(@NonNull Protocol.Type type) {
        String[] tokens = address.split("/");
        for (String token : tokens) {
            if (Objects.equals(token, type.name)) {
                return true;
            }
        }
        return false;
    }

    public boolean isIP4() {
        return address.startsWith("/ip4/");
    }

    public boolean isCircuitAddress() {
        return this.has(Protocol.Type.P2PCIRCUIT);
    }
}
