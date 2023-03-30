package threads.magnet.kad;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import threads.magnet.LogUtils;
import threads.magnet.Settings;
import threads.magnet.kad.messages.MessageBase;
import threads.magnet.kad.messages.MessageBase.Method;

/**
 * @author Damokles
 */
public class RPCCall {
    private static final String TAG = RPCCall.class.getSimpleName();
    private final List<RPCCallListener> listeners = new ArrayList<>(3);
    private final MessageBase reqMsg;
    private long sentTime = -1;
    private long responseTime = -1;
    private long expectedRTT = -1;
    private RPCState state = RPCState.UNSENT;
    private ScheduledExecutorService scheduler;
    private MessageBase rspMsg;
    private boolean sourceWasKnownReachable;
    private boolean socketMismatch;
    private ScheduledFuture<?> timeoutTimer;
    private Key expectedID;

    public RPCCall(MessageBase msg) {
        assert (msg != null);
        this.reqMsg = msg;
    }

    public void builtFromEntry(KBucketEntry e) {
        sourceWasKnownReachable = e.verifiedReachable();
    }

    public boolean knownReachableAtCreationTime() {
        return sourceWasKnownReachable;
    }

    public long getExpectedRTT() {
        return expectedRTT;
    }

    public void setExpectedRTT(long rtt) {
        expectedRTT = rtt;
    }

    /**
     * @throws NullPointerException if no expected id has been specified in advance
     */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean matchesExpectedID() {
        return expectedID.equals(rspMsg.getID());
    }

    public Key getExpectedID() {
        return expectedID;
    }

    public RPCCall setExpectedID(Key id) {
        expectedID = id;
        return this;
    }

    public void setSocketMismatch() {
        socketMismatch = true;
    }

    public boolean hasSocketMismatch() {
        return socketMismatch;
    }

    /**
     * when external circumstances indicate that this request is probably stalled and will time out
     */
    void injectStall() {
        stateTransition(EnumSet.of(RPCState.SENT), RPCState.STALLED);
    }

    public void response(MessageBase rsp) {
        if (timeoutTimer != null) {
            timeoutTimer.cancel(false);
        }

        rspMsg = rsp;

        switch (rsp.getType()) {
            case RSP_MSG:
                stateTransition(EnumSet.of(RPCState.SENT, RPCState.STALLED), RPCState.RESPONDED);
                break;
            case ERR_MSG:
                LogUtils.error(TAG, "received non-response [" + rsp + "] in response to request: " + reqMsg.toString());
                stateTransition(EnumSet.of(RPCState.SENT, RPCState.STALLED), RPCState.ERROR);
                break;
            default:
                throw new IllegalStateException("should not happen");
        }

    }

    /* (non-Javadoc)
     * @see threads.thor.bt.kad.RPCCallBase#addListener(threads.thor.bt.kad.RPCCallListener)
     */
    public void addListener(RPCCallListener cl) {
        Objects.requireNonNull(cl);
        if (state != RPCState.UNSENT)
            throw new IllegalStateException("can only attach listeners while call is not started yet");
        listeners.add(cl);
    }

    /* (non-Javadoc)
     * @see threads.thor.bt.kad.RPCCallBase#getMessageMethod()
     */
    public Method getMessageMethod() {
        return reqMsg.getMethod();
    }

    /// Get the request sent
    /* (non-Javadoc)
     * @see threads.thor.bt.kad.RPCCallBase#getRequest()
     */
    public MessageBase getRequest() {
        return reqMsg;
    }

    public MessageBase getResponse() {
        return rspMsg;
    }

    void sent(RPCServer srv) {
        if (LogUtils.isDebug() && !(expectedRTT > 0)) {
            throw new AssertionError("Assertion failed");
        }
        if (LogUtils.isDebug() && !(expectedRTT <= Settings.RPC_CALL_TIMEOUT_MAX)) {
            throw new AssertionError("Assertion failed");
        }
        sentTime = System.currentTimeMillis();


        stateTransition(EnumSet.of(RPCState.UNSENT), RPCState.SENT);

        scheduler = srv.getDHT().getScheduler();

        // spread out the stalls by +- 1ms to reduce lock contention
        int smear = ThreadLocalRandom.current().nextInt(-1000, 1000);
        timeoutTimer = scheduler.schedule(this::checkStallOrTimeout, expectedRTT * 1000 + smear, TimeUnit.MICROSECONDS);
    }


    private void checkStallOrTimeout() {
        synchronized (this) {
            if (state != RPCState.SENT && state != RPCState.STALLED)
                return;

            long elapsed = System.currentTimeMillis() - sentTime;
            long remaining = Settings.RPC_CALL_TIMEOUT_MAX - elapsed;
            if (remaining > 0) {
                stateTransition(EnumSet.of(RPCState.SENT), RPCState.STALLED);
                // re-schedule for failed
                timeoutTimer = scheduler.schedule(this::checkStallOrTimeout, remaining, TimeUnit.MILLISECONDS);
            } else {
                stateTransition(EnumSet.of(RPCState.SENT, RPCState.STALLED), RPCState.TIMEOUT);
            }
        }
    }

    void sendFailed() {
        stateTransition(EnumSet.of(RPCState.UNSENT), RPCState.TIMEOUT);
    }

    void cancel() {
        ScheduledFuture<?> timer = timeoutTimer;
        if (timer != null)
            timer.cancel(false);
        // it would be better if we didn't have to treat this as a timeout and could just signal call termination with an internal reason
        stateTransition(EnumSet.complementOf(EnumSet.of(RPCState.ERROR, RPCState.RESPONDED, RPCState.TIMEOUT)), RPCState.TIMEOUT);
    }


    private void stateTransition(EnumSet<RPCState> expected, RPCState newState) {
        synchronized (this) {
            RPCState oldState = state;

            if (!expected.contains(oldState)) {
                return;
            }

            state = newState;


            switch (newState) {
                case ERROR:
                case RESPONDED:
                    responseTime = System.currentTimeMillis();
                    break;
                default:
                    break;
            }


            for (int i = 0; i < listeners.size(); i++) {
                RPCCallListener l = listeners.get(i);
                l.stateTransition(this, oldState, newState);

                switch (newState) {
                    case TIMEOUT:
                        l.onTimeout(this);
                        break;
                    case STALLED:
                        break;
                    case RESPONDED:
                        l.onResponse(this, rspMsg);

                }

            }


        }
    }

    /**
     * @return -1 if there is no response yet or it has timed out. The round trip time in milliseconds otherwise
     */
    public long getRTT() {
        if (sentTime == -1 || responseTime == -1)
            return -1;
        return responseTime - sentTime;
    }

    public long getSentTime() {
        return sentTime;
    }

    public RPCState state() {
        return state;
    }

}
