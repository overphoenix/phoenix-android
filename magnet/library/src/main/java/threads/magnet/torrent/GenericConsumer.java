package threads.magnet.torrent;

import java.util.ArrayList;
import java.util.List;

import threads.magnet.LogUtils;
import threads.magnet.IConsumers;
import threads.magnet.protocol.Cancel;
import threads.magnet.protocol.Choke;
import threads.magnet.protocol.Interested;
import threads.magnet.protocol.KeepAlive;
import threads.magnet.protocol.Message;
import threads.magnet.protocol.NotInterested;
import threads.magnet.protocol.Unchoke;

public class GenericConsumer implements IConsumers {

    private static final String TAG = GenericConsumer.class.getSimpleName();
    private static final GenericConsumer instance = new GenericConsumer();

    public static GenericConsumer consumer() {
        return instance;
    }

    public void doConsume(Message message, MessageContext messageContext) {

        if (message instanceof Choke) {
            consume((Choke) message, messageContext);
        }
        if (message instanceof Unchoke) {
            consume((Unchoke) message, messageContext);
        }
        if (message instanceof Interested) {
            consume((Interested) message, messageContext);
        }
        if (message instanceof NotInterested) {
            consume((NotInterested) message, messageContext);
        }
        if (message instanceof Cancel) {
            consume((Cancel) message, messageContext);
        }
    }

    @Override
    public List<MessageConsumer<? extends Message>> getConsumers() {
        List<MessageConsumer<? extends Message>> list = new ArrayList<>();
        list.add(new MessageConsumer<KeepAlive>() {
            @Override
            public Class<KeepAlive> getConsumedType() {
                return KeepAlive.class;
            }

            @Override
            public void consume(KeepAlive message, MessageContext context) {
                doConsume(message, context);
            }
        });
        list.add(new MessageConsumer<Choke>() {
            @Override
            public Class<Choke> getConsumedType() {
                return Choke.class;
            }

            @Override
            public void consume(Choke message, MessageContext context) {
                doConsume(message, context);
            }
        });
        list.add(new MessageConsumer<Unchoke>() {
            @Override
            public Class<Unchoke> getConsumedType() {
                return Unchoke.class;
            }

            @Override
            public void consume(Unchoke message, MessageContext context) {
                doConsume(message, context);
            }
        });
        list.add(new MessageConsumer<Interested>() {
            @Override
            public Class<Interested> getConsumedType() {
                return Interested.class;
            }

            @Override
            public void consume(Interested message, MessageContext context) {
                doConsume(message, context);
            }
        });

        list.add(new MessageConsumer<NotInterested>() {
            @Override
            public Class<NotInterested> getConsumedType() {
                return NotInterested.class;
            }

            @Override
            public void consume(NotInterested message, MessageContext context) {
                doConsume(message, context);
            }
        });
        list.add(new MessageConsumer<Cancel>() {
            @Override
            public Class<Cancel> getConsumedType() {
                return Cancel.class;
            }

            @Override
            public void consume(Cancel message, MessageContext context) {
                doConsume(message, context);
            }
        });
        return list;
    }


    private void consume(Choke choke, MessageContext context) {
        LogUtils.info(TAG, choke.toString());
        context.getConnectionState().setPeerChoking(true);
    }


    private void consume(Unchoke unchoke, MessageContext context) {
        LogUtils.info(TAG, unchoke.toString());
        context.getConnectionState().setPeerChoking(false);
    }


    private void consume(Interested interested, MessageContext context) {
        LogUtils.info(TAG, interested.toString());
        context.getConnectionState().setPeerInterested(true);
    }


    private void consume(NotInterested notInterested, MessageContext context) {
        LogUtils.info(TAG, notInterested.toString());
        context.getConnectionState().setPeerInterested(false);
    }


    private void consume(Cancel cancel, MessageContext context) {
        context.getConnectionState().onCancel(cancel);
    }
}
