package threads.magnet.kad.messages;

import threads.magnet.kad.DHT;

/**
 * @author Damokles
 */
public class FindNodeResponse extends AbstractLookupResponse {


    public FindNodeResponse(byte[] mtid) {
        super(mtid, Method.FIND_NODE);
    }

    /* (non-Javadoc)
     * @see threads.thor.bt.kad.messages.MessageBase#apply(threads.thor.bt.kad.DHT)
     */
    @Override
    public void apply(DHT dh_table) {
        dh_table.response(this);
    }

}
