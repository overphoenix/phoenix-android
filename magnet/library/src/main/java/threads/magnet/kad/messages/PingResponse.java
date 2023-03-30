package threads.magnet.kad.messages;

import java.util.Map;
import java.util.TreeMap;

import threads.magnet.kad.DHT;


/**
 * @author Damokles
 */
public class PingResponse extends MessageBase {


    public PingResponse(byte[] mtid) {
        super(mtid, Method.PING, Type.RSP_MSG);
    }

    /* (non-Javadoc)
     * @see threads.thor.bt.kad.messages.MessageBase#apply(threads.thor.bt.kad.DHT)
     */
    @Override
    public void apply(DHT dh_table) {
        dh_table.response(this);
    }

    @Override
    public Map<String, Object> getInnerMap() {
        Map<String, Object> inner = new TreeMap<>();
        inner.put("id", id.getHash());

        return inner;
    }

}
