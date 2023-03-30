package threads.magnet;

import java.util.function.Consumer;

import threads.magnet.protocol.Message;
import threads.magnet.torrent.MessageContext;

public interface IProduces extends IAgent {
    void produce(Consumer<Message> messageConsumer, MessageContext context);
}
