package threads.magnet.protocol.extended;

import threads.magnet.protocol.Message;

public class ExtendedMessage implements Message {

    @Override
    public Integer getMessageId() {
        return ExtendedProtocol.EXTENDED_MESSAGE_ID;
    }
}
