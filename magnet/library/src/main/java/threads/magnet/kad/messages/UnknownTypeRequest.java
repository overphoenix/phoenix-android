package threads.magnet.kad.messages;

import threads.magnet.kad.DHT;
import threads.magnet.kad.Key;

public class UnknownTypeRequest extends AbstractLookupRequest {

    public UnknownTypeRequest(Key target) {
        super(target, Method.UNKNOWN);
    }

    @Override
    protected String targetBencodingName() {
        throw new UnsupportedOperationException("the name is only used for encoding. encoding of unknown requests is not supported");
    }

    @Override
    public void apply(DHT dh_table) {
        dh_table.findNode(this);
    }

}
