package threads.magnet.net.pipeline;

import androidx.annotation.Nullable;

import java.nio.ByteBuffer;
import java.util.List;

import threads.magnet.net.Peer;
import threads.magnet.net.buffer.BorrowedBuffer;
import threads.magnet.net.buffer.BufferMutator;
import threads.magnet.net.buffer.IBufferManager;
import threads.magnet.protocol.Message;
import threads.magnet.protocol.handler.MessageHandler;

public class ChannelPipelineFactory {

    private final IBufferManager bufferManager;
    private final BufferedPieceRegistry bufferedPieceRegistry;


    public ChannelPipelineFactory(IBufferManager bufferManager, BufferedPieceRegistry bufferedPieceRegistry) {
        this.bufferManager = bufferManager;
        this.bufferedPieceRegistry = bufferedPieceRegistry;
    }


    public ChannelPipelineBuilder buildPipeline(Peer peer) {
        return new ChannelPipelineBuilder(peer) {
            @Override
            protected ChannelPipeline doBuild(
                    Peer peer,
                    MessageHandler<Message> protocol,
                    @Nullable BorrowedBuffer<ByteBuffer> inboundBuffer,
                    @Nullable BorrowedBuffer<ByteBuffer> outboundBuffer,
                    List<BufferMutator> decoders,
                    List<BufferMutator> encoders) {

                BorrowedBuffer<ByteBuffer> _inboundBuffer = bufferManager.borrowByteBuffer();
                if (inboundBuffer != null) {
                    _inboundBuffer = inboundBuffer;
                }
                BorrowedBuffer<ByteBuffer> _outboundBuffer = bufferManager.borrowByteBuffer();
                if (_outboundBuffer != null) {
                    _outboundBuffer = outboundBuffer;
                }
                return new ChannelPipeline(peer, protocol, _inboundBuffer, _outboundBuffer,
                        decoders, encoders, bufferedPieceRegistry);
            }
        };
    }
}
