package threads.magnet.torrent;

import java.util.function.Consumer;

import threads.magnet.IAgent;
import threads.magnet.protocol.Message;

public interface MessageRouter {

    /**
     * Route a message to consumers.
     *
     * @since 1.3
     */
    void consume(Message message, MessageContext context);

    /**
     * Request a message from producers.
     *
     * @since 1.3
     */
    void produce(Consumer<Message> messageConsumer, MessageContext context);

    /**
     * Add a messaging agent, that can act as a message consumer and/or producer.
     *
     * @since 1.3
     */
    void registerMessagingAgent(IAgent agent);


}
