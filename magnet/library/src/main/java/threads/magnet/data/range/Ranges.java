package threads.magnet.data.range;

import threads.magnet.data.BlockSet;

public class Ranges {


    public static <T extends Range<T>> BlockRange<T> blockRange(T range, long blockSize) {
        return new BlockRange<>(range, blockSize);
    }

    public static <T extends Range<T>> Range<T> synchronizedRange(T range) {
        return new SynchronizedRange<>(range);
    }

    public static BlockSet synchronizedBlockSet(BlockSet blockSet) {
        return new SynchronizedBlockSet(blockSet);
    }
}
