package threads.magnet.kad.tasks;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.stream.Collectors;

import threads.magnet.LogUtils;


final class TaskStats {

    private final int[] counters;

    public TaskStats() {
        counters = new int[CountedStat.values().length];
    }

    private TaskStats(int[] c) {
        counters = c;
    }

    private void invariants() {
        if (LogUtils.isDebug() && !(get(CountedStat.SENT) >= get(CountedStat.STALLED) + get(CountedStat.FAILED) + get(CountedStat.RECEIVED))) {
            throw new AssertionError("Assertion failed");
        }
        if (LogUtils.isDebug() && Arrays.stream(counters).anyMatch(i -> i < 0)) {
            throw new AssertionError("Assertion failed");
        }
    }


    public TaskStats update(EnumSet<CountedStat> inc, EnumSet<CountedStat> dec, EnumSet<CountedStat> zero) {
        TaskStats p = cloneTaskStats();
        for (CountedStat counter : inc) {
            p.counters[counter.ordinal()]++;
        }
        for (CountedStat counter : dec) {
            p.counters[counter.ordinal()]--;
        }
        for (CountedStat counter : zero) {
            p.counters[counter.ordinal()] = 0;
        }
        p.invariants();
        return p;
    }

    public int get(CountedStat c) {
        return counters[c.ordinal()];
    }

    private TaskStats cloneTaskStats() {
        return new TaskStats(counters.clone());
    }

    private int done() {
        return get(CountedStat.FAILED) + get(CountedStat.RECEIVED);
    }

    public int activeOnly() {
        return unanswered() - currentStalled();
    }

    private int currentStalled() {
        return get(CountedStat.STALLED);
    }

    public int unanswered() {
        return get(CountedStat.SENT) - done();
    }

    @Override
    public String toString() {
        String coreVals = Arrays.stream(CountedStat.values()).map(st -> st.toString() + ":" + get(st)).collect(Collectors.joining(" "));
        return coreVals + " activeOnly:" + activeOnly() + " unanswered:" + unanswered();
    }


}
