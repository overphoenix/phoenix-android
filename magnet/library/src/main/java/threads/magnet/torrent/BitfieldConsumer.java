package threads.magnet.torrent;


import java.util.ArrayList;
import java.util.List;

import threads.magnet.IConsumers;
import threads.magnet.event.EventSink;
import threads.magnet.net.ConnectionKey;
import threads.magnet.protocol.BitOrder;
import threads.magnet.protocol.Bitfield;
import threads.magnet.protocol.Have;
import threads.magnet.protocol.Message;


public class BitfieldConsumer implements IConsumers {

    private final threads.magnet.data.Bitfield bitfield;
    private final PieceStatistics pieceStatistics;
    private final EventSink eventSink;

    public BitfieldConsumer(threads.magnet.data.Bitfield bitfield, PieceStatistics pieceStatistics, EventSink eventSink) {
        this.bitfield = bitfield;
        this.pieceStatistics = pieceStatistics;
        this.eventSink = eventSink;
    }


    public void doConsume(Message message, MessageContext messageContext) {
        if (message instanceof Bitfield) {
            consume((Bitfield) message, messageContext);
        }
        if (message instanceof Have) {
            consume((Have) message, messageContext);
        }
    }

    @Override
    public List<MessageConsumer<? extends Message>> getConsumers() {
        List<MessageConsumer<? extends Message>> list = new ArrayList<>();
        list.add(new MessageConsumer<Bitfield>() {
            @Override
            public Class<Bitfield> getConsumedType() {
                return Bitfield.class;
            }

            @Override
            public void consume(Bitfield message, MessageContext context) {
                doConsume(message, context);
            }
        });
        list.add(new MessageConsumer<Have>() {
            @Override
            public Class<Have> getConsumedType() {
                return Have.class;
            }

            @Override
            public void consume(Have message, MessageContext context) {
                doConsume(message, context);
            }
        });
        return list;
    }


    private void consume(Bitfield bitfieldMessage, MessageContext context) {
        ConnectionKey peer = context.getConnectionKey();
        threads.magnet.data.Bitfield peerBitfield = new threads.magnet.data.Bitfield(
                bitfieldMessage.getBitfield(), BitOrder.LITTLE_ENDIAN, bitfield.getPiecesTotal());
        pieceStatistics.addBitfield(peer, peerBitfield);
        eventSink.firePeerBitfieldUpdated(peer);
    }

    private void consume(Have have, MessageContext context) {
        ConnectionKey peer = context.getConnectionKey();
        pieceStatistics.addPiece(peer, have.getPieceIndex());
        pieceStatistics.getPeerBitfield(peer).ifPresent(
                bitfield -> eventSink.firePeerBitfieldUpdated(peer));
    }
}
