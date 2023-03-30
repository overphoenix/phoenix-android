package threads.magnet.event;

import androidx.annotation.NonNull;

import threads.magnet.net.ConnectionKey;


public class PeerBitfieldUpdatedEvent extends BaseEvent {

    private final ConnectionKey connectionKey;


    PeerBitfieldUpdatedEvent(long id, long timestamp, ConnectionKey connectionKey) {
        super(id, timestamp);
        this.connectionKey = connectionKey;
    }


    @NonNull
    @Override
    public String toString() {
        return "[" + this.getClass().getSimpleName() + "]  connection key {" + connectionKey + "}";
    }
}
