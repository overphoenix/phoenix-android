/*
 * Copyright (c) 2016—2017 Andrei Tomashpolskiy and individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package threads.magnet.dht;

import java.io.IOException;

import threads.magnet.net.HandshakeHandler;
import threads.magnet.net.PeerConnection;
import threads.magnet.protocol.Handshake;
import threads.magnet.protocol.Port;

/**
 * @since 1.1
 */
public class DHTHandshakeHandler implements HandshakeHandler {

    private static final int DHT_FLAG_BIT_INDEX = 63;

    private final int port;

    public DHTHandshakeHandler(int port) {
        this.port = port;
    }

    @Override
    public void processIncomingHandshake(PeerConnection connection, Handshake peerHandshake) {
        // according to the spec, the client should immediately communicate its' DHT service port
        // upon receiving a handshake indicating DHT support
        if (peerHandshake.isReservedBitSet(DHT_FLAG_BIT_INDEX)) {
            try {
                connection.postMessage(new Port(port));
            } catch (IOException e) {
                throw new RuntimeException("Failed to send port to peer: " + connection.getRemotePeer(), e);
            }
        }
    }

    @Override
    public void processOutgoingHandshake(Handshake handshake) {
        handshake.setReservedBit(DHT_FLAG_BIT_INDEX);
    }
}
