package threads.magnet;


import java.util.List;

import threads.magnet.protocol.Message;
import threads.magnet.torrent.MessageConsumer;

public interface IConsumers extends IAgent {

    List<MessageConsumer<? extends Message>> getConsumers();
}
