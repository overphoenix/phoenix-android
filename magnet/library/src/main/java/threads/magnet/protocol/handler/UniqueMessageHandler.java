package threads.magnet.protocol.handler;

import java.util.Collection;
import java.util.Collections;

import threads.magnet.net.buffer.ByteBufferView;
import threads.magnet.protocol.Message;

public abstract class UniqueMessageHandler<T extends Message> extends BaseMessageHandler<T> {

    private final Class<T> type;
    private final Collection<Class<? extends T>> supportedTypes;

    UniqueMessageHandler(Class<T> type) {
        this.type = type;
        supportedTypes = Collections.singleton(type);
    }

    @Override
    public Collection<Class<? extends T>> getSupportedTypes() {
        return supportedTypes;
    }

    @Override
    public Class<? extends T> readMessageType(ByteBufferView buffer) {
        return type;
    }
}
