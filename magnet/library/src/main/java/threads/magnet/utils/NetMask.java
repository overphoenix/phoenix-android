package threads.magnet.utils;

import static threads.magnet.utils.Functional.unchecked;

import java.net.InetAddress;
import java.util.Objects;

public class NetMask {
    private final byte[] addr;
    private final int mask;

    public NetMask(InetAddress addr, int mask) {
        this.mask = mask;
        this.addr = addr.getAddress();
        if (this.addr.length * 8 < mask)
            throw new IllegalArgumentException("mask cannot cover more bits than the length of the network address");
    }

    public static NetMask fromString(String toParse) {
        String[] parts = toParse.split("/");
        return new NetMask(Objects.requireNonNull(unchecked(()
                -> InetAddress.getByName(parts[0]))), Integer.parseInt(parts[1]));
    }

    public boolean contains(InetAddress toTest) {
        byte[] other = toTest.getAddress();

        if (addr.length != other.length)
            return false;

        for (int i = 0; i < mask / 8; i++) {
            if (addr[i] != other[i])
                return false;
        }

        if (mask % 8 == 0)
            return true;

        int offset = mask / 8;

        int probeMask = (0xff00 >> mask % 8) & 0xff;

        return (addr[offset] & probeMask) == (other[offset] & probeMask);
    }
}
