package threads.magnet.net.crypto;

import static threads.magnet.net.crypto.MSEKeyPairGenerator.PUBLIC_KEY_BYTES;

import android.annotation.SuppressLint;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.Arrays;
import java.util.Optional;
import java.util.Random;

import threads.magnet.LogUtils;
import threads.magnet.Settings;
import threads.magnet.metainfo.TorrentId;
import threads.magnet.net.BigIntegers;
import threads.magnet.net.ByteChannelReader;
import threads.magnet.net.Peer;
import threads.magnet.net.buffer.DelegatingByteBufferView;
import threads.magnet.protocol.DecodingContext;
import threads.magnet.protocol.Handshake;
import threads.magnet.protocol.Message;
import threads.magnet.protocol.Protocols;
import threads.magnet.protocol.crypto.EncryptionPolicy;
import threads.magnet.protocol.crypto.MSECipher;
import threads.magnet.protocol.handler.MessageHandler;
import threads.magnet.torrent.TorrentDescriptor;
import threads.magnet.torrent.TorrentRegistry;

public class MSEHandshakeProcessor {
    private static final String TAG = MSEHandshakeProcessor.class.getSimpleName();
    private static final Duration receiveTimeout = Duration.ofSeconds(10);
    private static final Duration waitBetweenReads = Duration.ofSeconds(1);
    private static final int paddingMaxLength = 512;
    private static final byte[] VC_RAW_BYTES = new byte[8];

    private final MSEKeyPairGenerator keyGenerator;
    private final TorrentRegistry torrentRegistry;
    private final MessageHandler<Message> protocol;
    private final EncryptionPolicy localEncryptionPolicy;

    // indicates, that MSE encryption negotiation procedure should not be used
    private final boolean mseDisabled;

    public MSEHandshakeProcessor(
            TorrentRegistry torrentRegistry,
            MessageHandler<Message> protocol) {

        this.localEncryptionPolicy = Settings.encryptionPolicy;

        int msePrivateKeySize = Settings.msePrivateKeySize;
        boolean mseDisabled = !MSECipher.isKeySizeSupported(msePrivateKeySize);
        if (mseDisabled) {
            @SuppressLint("DefaultLocale") String message = String.format(
                    "Current Bt runtime is configured to use private key size of %d bytes for Message Stream Encryption (MSE),"
                            + " and the preferred encryption policy is %s."
                            + " The aforementioned key size is not allowed in the current JDK configuration."
                            + " Hence, MSE encryption negotiation procedure will NOT be used",
                    msePrivateKeySize, localEncryptionPolicy.name());

            String postfix = " To fix this problem, please do one of the following:"
                    + " (a) update your JDK or Java runtime environment settings for unlimited cryptography support;"
                    + " (b) specify a different private key size (not recommended)";

            switch (localEncryptionPolicy) {
                case REQUIRE_PLAINTEXT:
                case PREFER_PLAINTEXT:
                case PREFER_ENCRYPTED: {
                    message += ", and all peer connections will be established in plaintext by using the standard BitTorrent handshake."
                            + " This may negatively affect the number of peers, which can be connected to."
                            + postfix;
                    LogUtils.info(TAG, message);
                    break;
                }
                case REQUIRE_ENCRYPTED: {
                    message += ", and considering the requirement for mandatory encryption, this effectively means,"
                            + " that no peer connections will ever be established."
                            + postfix
                            + "; (c) choose a more permissive encryption policy";
                    throw new IllegalStateException(message);
                }
                default: {
                    throw new IllegalStateException("Unknown encryption policy: " + localEncryptionPolicy.name());
                }
            }

        }
        this.mseDisabled = mseDisabled;

        this.keyGenerator = new MSEKeyPairGenerator(msePrivateKeySize);
        this.torrentRegistry = torrentRegistry;
        this.protocol = protocol;
    }

    public MSECipher negotiateOutgoing(ByteChannel channel, TorrentId torrentId, ByteBuffer in, ByteBuffer out) throws IOException {
        if (mseDisabled) {
            return null;
        }


        /*
         * Blocking steps:
         *
         * 1. A->B: Diffie Hellman Ya, PadA
         * 2. B->A: Diffie Hellman Yb, PadB
         * 3. A->B:
         *  - HASH('req1', S),
         *  - HASH('req2', SKEY) xor HASH('req3', S),
         *  - ENCRYPT(VC, crypto_provide, len(PadC), PadC, len(IA)),
         *  - ENCRYPT(IA)
         * 4. B->A:
         * - ENCRYPT(VC, crypto_select, len(padD), padD),
         * - ENCRYPT2(Payload Stream)
         * 5. A->B: ENCRYPT2(Payload Stream)
         */
        ByteChannelReader reader = reader(channel);

        // 1. A->B: Diffie Hellman Ya, PadA
        // send our public key
        KeyPair keys = keyGenerator.generateKeyPair();
        out.put(keys.getPublic().getEncoded());
        out.put(getPadding());
        out.flip();
        channel.write(out);
        out.clear();

        // 2. B->A: Diffie Hellman Yb, PadB
        // receive peer's public key
        int phase1Min = PUBLIC_KEY_BYTES;
        int phase1Limit = phase1Min + paddingMaxLength;
        int phase1Read = reader.readBetween(phase1Min, phase1Limit).read(in);
        in.flip();
        BigInteger peerPublicKey = BigIntegers.decodeUnsigned(in, phase1Min);
        in.clear(); // discard the padding, if present

        // calculate shared secret S
        BigInteger S = keyGenerator.calculateSharedSecret(peerPublicKey, keys.getPrivate());

        // 3. A->B:
        MessageDigest digest = getDigest();
        // - HASH('req1', S)
        digest.update("req1".getBytes(StandardCharsets.US_ASCII));
        digest.update(BigIntegers.encodeUnsigned(S, PUBLIC_KEY_BYTES));
        out.put(digest.digest());
        // - HASH('req2', SKEY) xor HASH('req3', S)
        digest.update("req2".getBytes(StandardCharsets.US_ASCII));
        digest.update(torrentId.getBytes());
        byte[] b1 = digest.digest();
        digest.update("req3".getBytes(StandardCharsets.US_ASCII));
        digest.update(BigIntegers.encodeUnsigned(S, PUBLIC_KEY_BYTES));
        byte[] b2 = digest.digest();
        out.put(xor(b1, b2));
        // write
        out.flip();
        channel.write(out);
        out.clear();

        byte[] Sbytes = BigIntegers.encodeUnsigned(S, PUBLIC_KEY_BYTES);
        MSECipher cipher = MSECipher.forInitiator(Sbytes, torrentId);
        ByteChannel encryptedChannel = new EncryptedChannel(channel, cipher.getDecryptionCipher(), cipher.getEncryptionCipher());
        // - ENCRYPT(VC, crypto_provide, len(PadC), PadC, len(IA))
        out.put(VC_RAW_BYTES);
        out.put(getCryptoProvideBitfield(localEncryptionPolicy));
        byte[] padding = getZeroPadding();
        out.put(Protocols.getShortBytes(padding.length));
        out.put(padding);
        // - ENCRYPT(IA)
        // do not write IA (initial payload data) for now, wait for encryption negotiation
        out.putShort((short) 0); // IA length = 0
        out.flip();
        encryptedChannel.write(out);
        out.clear();

        // 4. B->A:
        // - ENCRYPT(VC, crypto_select, len(padD), padD)
        byte[] encryptedVC;
        {
            MSECipher throwawayCipher = MSECipher.forInitiator(Sbytes, torrentId);
            try {
                encryptedVC = throwawayCipher.getDecryptionCipher().doFinal(VC_RAW_BYTES);
            } catch (Exception e) {
                throw new RuntimeException("Failed to encrypt VC", e);
            }
        }
        // synchronize on the incoming stream of data
        int phase2Min = encryptedVC.length + 4/*crypto_select*/ + 2/*padding_len*/;
        // account for (phase1Limit - phase1Read) in case the padding from phase1 arrives later than expected
        int phase2Limit = (phase1Limit - phase1Read) + phase2Min + paddingMaxLength;

        int initpos = in.position();
        // use plaintext reader because encryption stream is not synced yet and will potentially produce garbage
        int phase2Read = reader.readBetween(phase2Min, phase2Limit).sync(in, encryptedVC);
        int matchpos = in.position();

        // the rest of the data is known to be encrypted
        ByteChannelReader encryptedReader = reader(encryptedChannel);
        // but we need to align the incoming (decrypting) cipher
        // for the number of encrypted bytes that have already arrived
        // and decrypt these bytes in the incoming data buffer for later processing
        in.limit(initpos + phase2Read);
        {
            cipher.getDecryptionCipher().update(new byte[VC_RAW_BYTES.length]);
            byte[] encryptedData = new byte[in.remaining()];
            in.get(encryptedData);
            in.position(matchpos);
            byte[] decryptedData = cipher.getDecryptionCipher().update(encryptedData);
            in.put(decryptedData);
            in.position(matchpos);
        }

        // we may still need to receive some handshake data (e.g. padding), that is arriving later than expected
        if (in.remaining() < (phase2Min - encryptedVC.length)) {
            int lim = in.limit();
            in.limit(in.capacity());
            int read = encryptedReader.readAtLeast(phase2Min - encryptedVC.length)
                    .readNoMoreThan((phase2Min - encryptedVC.length) + paddingMaxLength)
                    .read(in);
            in.position(matchpos);
            in.limit(lim + read);
        }

        byte[] crypto_select = new byte[4];
        in.get(crypto_select);
        EncryptionPolicy negotiatedEncryptionPolicy = selectPolicy(crypto_select, localEncryptionPolicy);


        int theirPadding = in.getShort() & 0xFFFF;
        int missing = (theirPadding - in.remaining());
        if (missing > 0) {
            int pos = in.position();
            in.limit(in.capacity());
            encryptedReader.readAtLeast(missing).read(in);
            in.flip();
            in.position(pos);
        }

        // account for the upper layer protocol data that has already arrived
        in.position(in.position() + theirPadding);
        in.compact();
        out.clear();

        // - ENCRYPT2(Payload Stream)
        switch (negotiatedEncryptionPolicy) {
            case REQUIRE_PLAINTEXT:
            case PREFER_PLAINTEXT: {
                return null;
            }
            case PREFER_ENCRYPTED:
            case REQUIRE_ENCRYPTED: {
                return cipher;
            }
            default: {
                throw new IllegalStateException("Unknown encryption policy: " + negotiatedEncryptionPolicy.name());
            }
        }
    }


    public MSECipher negotiateIncoming(Peer peer, ByteChannel channel, ByteBuffer in, ByteBuffer out) throws IOException {
        if (mseDisabled) {
            return null;
        }

        /*
         * Blocking steps:
         *
         * 1. A->B: Diffie Hellman Ya, PadA
         * 2. B->A: Diffie Hellman Yb, PadB
         * 3. A->B:
         *  - HASH('req1', S),
         *  - HASH('req2', SKEY) xor HASH('req3', S),
         *  - ENCRYPT(VC, crypto_provide, len(PadC), PadC, len(IA)),
         *  - ENCRYPT(IA)
         * 4. B->A:
         *  - ENCRYPT(VC, crypto_select, len(padD), padD),
         *  - ENCRYPT2(Payload Stream)
         * 5. A->B: ENCRYPT2(Payload Stream)
         */
        ByteChannelReader reader = reader(channel);

        // 1. A->B: Diffie Hellman Ya, PadA
        // receive initiator's public key
        // specify lower threshold on the amount of bytes to receive,
        // as we will try to decode plaintext message of an unknown length first
        int phase0Min = 20;
        int phase0Read = reader.readAtLeast(phase0Min).read(in);
        in.flip();
        // try to determine the protocol from the first received bytes
        DecodingContext context = new DecodingContext(peer);
        int consumed = 0;
        try {
            consumed = protocol.decode(context, new DelegatingByteBufferView(in));
        } catch (Exception ignored) {
            // ignore
        }
        // TODO: can this be done without knowing the protocol specifics? (KeepAlive can be especially misleading: 0x00 0x00 0x00 0x00)
        if (consumed > 0 && context.getMessage() instanceof Handshake) {
            // decoding was successful, can use plaintext (if supported)
            assertPolicyIsCompatible();
            return null;
        }

        int phase1Min = PUBLIC_KEY_BYTES;
        int phase1Limit = phase1Min + paddingMaxLength;
        in.limit(in.capacity());
        in.position(phase0Read);
        // verify that there is a sufficient amount of bytes to decode peer's public key
        int phase1Read;
        if (phase0Read < phase1Min) {
            phase1Read = reader.readAtLeast(phase1Min - phase0Read).readNoMoreThan(phase1Limit - phase0Read).read(in);
        } else {
            phase1Read = 0;
        }

        in.flip();
        BigInteger peerPublicKey = BigIntegers.decodeUnsigned(in, PUBLIC_KEY_BYTES);
        in.clear(); // discard padding

        // 2. B->A: Diffie Hellman Yb, PadB
        // send our public key
        KeyPair keys = keyGenerator.generateKeyPair();
        out.put(keys.getPublic().getEncoded());
        out.put(getPadding());
        out.flip();
        channel.write(out);
        out.clear();

        // calculate shared secret S
        BigInteger S = keyGenerator.calculateSharedSecret(peerPublicKey, keys.getPrivate());

        // 3. A->B:
        int phase2Min = 20 + 20 + 8 + 4 + 2 + 2;
        int phase2Limit = 20 + 20 + 8 + 4 + 2 + 512 + 2;
        MessageDigest digest = getDigest();
        // padding from phase 1 may be arriving later than expected, so we need to synchronize
        // on the incoming stream of data, looking for a correct S hash
        byte[] bytes = new byte[20];
        // - HASH('req1', S)
        digest.update("req1".getBytes(StandardCharsets.US_ASCII));
        digest.update(BigIntegers.encodeUnsigned(S, PUBLIC_KEY_BYTES));
        byte[] req1hash = digest.digest();
        // syncing will also ensure that the peer knows S (otherwise synchronization will fail due to not finding the pattern)
        int phase2Read = reader.readAtLeast(phase2Min)
                .readNoMoreThan(phase2Limit + (phase1Limit - (phase0Read + phase1Read))) // account for padding arriving later
                .sync(in, req1hash);
        in.limit(phase1Read + phase2Read);

        // - HASH('req2', SKEY) xor HASH('req3', S)
        in.get(bytes); // read SKEY/S hash
        TorrentId requestedTorrent = null;
        digest.update("req3".getBytes(StandardCharsets.US_ASCII));
        digest.update(BigIntegers.encodeUnsigned(S, PUBLIC_KEY_BYTES));
        byte[] b2 = digest.digest();
        for (TorrentId torrentId : torrentRegistry.getTorrentIds()) {
            digest.update("req2".getBytes(StandardCharsets.US_ASCII));
            digest.update(torrentId.getBytes());
            byte[] b1 = digest.digest();
            if (Arrays.equals(xor(b1, b2), bytes)) {
                requestedTorrent = torrentId;
                break;
            }
        }
        // check that threads.torrent is supported and active
        if (requestedTorrent == null) {
            throw new IllegalStateException("Unsupported threads.torrent requested");
        } else {
            Optional<TorrentDescriptor> descriptor = torrentRegistry.getDescriptor(requestedTorrent);
            if (descriptor.isPresent() && !descriptor.get().isActive()) {
                // don't throw an exception if descriptor is not present -- threads.torrent might be being fetched at the time
                throw new IllegalStateException("Inactive threads.torrent requested: " + requestedTorrent);
            }
        }

        byte[] Sbytes = BigIntegers.encodeUnsigned(S, PUBLIC_KEY_BYTES);
        MSECipher cipher = MSECipher.forReceiver(Sbytes, requestedTorrent);
        ByteChannel encryptedChannel = new EncryptedChannel(channel, cipher.getDecryptionCipher(), cipher.getEncryptionCipher());
        ByteChannelReader encryptedReader = reader(encryptedChannel);

        // - ENCRYPT(VC, crypto_provide, len(PadC), PadC, len(IA))
        // derypt encrypted leftovers from step #3
        int pos = in.position();
        byte[] leftovers = new byte[in.remaining()];
        in.get(leftovers);
        in.position(pos);
        try {
            in.put(cipher.getDecryptionCipher().update(leftovers));
        } catch (Exception e) {
            throw new RuntimeException("Failed to decrypt leftover bytes: " + leftovers.length);
        }
        in.position(pos);

        byte[] theirVC = new byte[8];
        in.get(theirVC);
        if (!Arrays.equals(VC_RAW_BYTES, theirVC)) {
            throw new IllegalStateException("Invalid VC: " + Arrays.toString(theirVC));
        }

        byte[] crypto_provide = new byte[4];
        in.get(crypto_provide);
        EncryptionPolicy negotiatedEncryptionPolicy = selectPolicy(crypto_provide, localEncryptionPolicy);


        int theirPadding = in.getShort() & 0xFFFF;
        if (theirPadding > 512) {
            // sanity check
            throw new IllegalStateException("Padding is too long: " + theirPadding);
        }
        int position = in.position(); // mark
        // check if the whole padding block has already arrived
        if (in.remaining() < theirPadding) {
            in.limit(in.capacity());
            in.position(phase1Read + phase2Read);
            encryptedReader.readAtLeast(theirPadding - in.remaining() + 2/*IA length*/).read(in);
            in.flip();
            in.position(position); // reset
        }

        in.position(position + theirPadding); // discard padding
        // Initial Payload length (0..65535 bytes)
        in.getShort(); // currently we ignore IA, it will be processed by the upper layer
        in.compact();

        // 4. B->A:
        // - ENCRYPT(VC, crypto_select, len(padD), padD)
        // - ENCRYPT2(Payload Stream)
        out.put(VC_RAW_BYTES);
        out.put(getCryptoProvideBitfield(negotiatedEncryptionPolicy));
        byte[] padding = getZeroPadding();
        out.putShort((short) padding.length);
        out.put(padding);
        out.flip();
        encryptedChannel.write(out);
        out.clear();

        switch (negotiatedEncryptionPolicy) {
            case REQUIRE_PLAINTEXT:
            case PREFER_PLAINTEXT: {
                return null;
            }
            case PREFER_ENCRYPTED:
            case REQUIRE_ENCRYPTED: {
                return cipher;
            }
            default: {
                throw new IllegalStateException("Unknown encryption policy: " + negotiatedEncryptionPolicy.name());
            }
        }
    }

    private ByteChannelReader reader(ReadableByteChannel channel) {
        return ByteChannelReader.forChannel(channel).withTimeout(receiveTimeout).waitBetweenReads(waitBetweenReads);
    }

    private void assertPolicyIsCompatible() {
        if (!localEncryptionPolicy.isCompatible(EncryptionPolicy.REQUIRE_PLAINTEXT)) {
            throw new RuntimeException("Encryption policies are incompatible: peer's (" + EncryptionPolicy.REQUIRE_PLAINTEXT.name()
                    + "), local (" + localEncryptionPolicy.name() + ")");
        }
    }

    private byte[] getPadding() {
        // todo: use this constructor everywhere in the project
        Random r = new Random();
        byte[] padding = new byte[r.nextInt(MSEHandshakeProcessor.paddingMaxLength + 1)];
        for (int i = 0; i < padding.length; i++) {
            padding[i] = (byte) r.nextInt(256);
        }
        return padding;
    }

    private byte[] getZeroPadding() {
        // todo: use this constructor everywhere in the project
        Random r = new Random();
        return new byte[r.nextInt(512 + 1)];
    }

    private MessageDigest getDigest() {
        try {
            return MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    private byte[] xor(byte[] b1, byte[] b2) {
        if (b1.length != b2.length) {
            throw new IllegalStateException("Lengths do not match: " + b1.length + ", " + b2.length);
        }
        byte[] result = new byte[b1.length];
        for (int i = 0; i < b1.length; i++) {
            result[i] = (byte) (b1[i] ^ b2[i]);
        }
        return result;
    }

    private byte[] getCryptoProvideBitfield(EncryptionPolicy encryptionPolicy) {
        byte[] crypto_provide = new byte[4];
        switch (encryptionPolicy) {
            case REQUIRE_PLAINTEXT: {
                crypto_provide[3] = 1; // only 0x01
                break;
            }
            case PREFER_PLAINTEXT:
            case PREFER_ENCRYPTED: {
                crypto_provide[3] = 3; // both 0x01 and 0x02
                break;
            }
            case REQUIRE_ENCRYPTED: {
                crypto_provide[3] = 2; // only 0x02
                break;
            }
            default: {
                // do nothing, bitfield is all zeros
            }
        }
        return crypto_provide;
    }

    private EncryptionPolicy selectPolicy(byte[] crypto_provide, EncryptionPolicy localEncryptionPolicy) {
        boolean plaintextProvided = (crypto_provide[3] & 0x01) == 0x01;
        boolean encryptionProvided = (crypto_provide[3] & 0x02) == 0x02;

        EncryptionPolicy selected = null;
        if (plaintextProvided || encryptionProvided) {
            switch (localEncryptionPolicy) {
                case REQUIRE_PLAINTEXT: {
                    if (plaintextProvided) {
                        selected = EncryptionPolicy.REQUIRE_PLAINTEXT;
                    }
                    break;
                }
                case PREFER_PLAINTEXT: {
                    selected = plaintextProvided ? EncryptionPolicy.REQUIRE_PLAINTEXT : EncryptionPolicy.REQUIRE_ENCRYPTED;
                    break;
                }
                case PREFER_ENCRYPTED: {
                    selected = encryptionProvided ? EncryptionPolicy.REQUIRE_ENCRYPTED : EncryptionPolicy.REQUIRE_PLAINTEXT;
                    break;
                }
                case REQUIRE_ENCRYPTED: {
                    if (encryptionProvided) {
                        selected = EncryptionPolicy.REQUIRE_ENCRYPTED;
                    }
                    break;
                }
                default: {
                    throw new IllegalStateException("Unknown encryption policy: " + localEncryptionPolicy.name());
                }
            }
        }

        if (selected == null) {
            throw new IllegalStateException("Failed to negotiate the encryption policy: local policy (" +
                    localEncryptionPolicy.name() + "), peer's policy (" + Arrays.toString(crypto_provide) + ")");
        }
        return selected;
    }
}
