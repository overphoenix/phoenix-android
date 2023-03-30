package threads.magnet.kad.messages;

import threads.magnet.kad.DHT;
import threads.magnet.kad.Key;

/**
 * @author Damokles
 */
public class FindNodeRequest extends AbstractLookupRequest {


    public FindNodeRequest(Key target) {
        super(target, Method.FIND_NODE);
    }

    /* (non-Javadoc)
     * @see threads.thor.bt.kad.messages.MessageBase#apply(threads.thor.bt.kad.DHT)
     */
    @Override
    public void apply(DHT dh_table) {
        dh_table.findNode(this);
    }

    @Override
    protected String targetBencodingName() {
        return "target";
    }
}
