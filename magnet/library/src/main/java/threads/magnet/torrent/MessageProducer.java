package threads.magnet.torrent;

import java.util.function.Consumer;

import threads.magnet.protocol.Message;

interface MessageProducer {

    /**
     * Produce a message to the remote peer, if possible
     *
     * @since 1.0
     */
    void produce(Consumer<Message> messageConsumer, MessageContext context);
}
