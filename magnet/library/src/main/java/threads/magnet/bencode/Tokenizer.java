package threads.magnet.bencode;

import java.nio.ByteBuffer;
import java.util.stream.IntStream;

public class Tokenizer {

    /* handled edge-cases:
     *
     * - negative length strings
     * - deep dictionary/list nesting
     * - zero-prefixed numbers (illegal)
     * - dictionary-exit while expecting a value
     * - non-string while expecting dict key
     * - end of input reached
     *
     * TODO:
     * - verify correct sorting
     * - long overflow
     *
     * Handled downstream: duplicate dict keys
     *
     */
    private final Token[] stack = new Token[256];
    TokenConsumer consumer;
    long lastDecodedNum;
    private int stackIdx = 0;
    private ByteBuffer buf;

    public Tokenizer() {
        IntStream.range(0, stack.length).forEach(i -> stack[i] = new Token());
    }

    void reset() {
        for (Token token : stack) token.reset();
        stackIdx = 0;
        buf = null;
    }


    private Token currentToken() {
        return stack[stackIdx];
    }

    Token atStackOffset(int offset) {
        int depth = stackIdx + offset;
        if (depth < 0)
            return null;
        return stack[depth];
    }

    private void push(TokenType t, int pos) {
        Token current = currentToken();
        if (current.expect() == DictState.ExpectKeyOrEnd && t != TokenType.PREFIXED_STRING)
            throw new BDecodingException("encountered " + t.toString() + " at offset " + pos + " while expecting a dictionary key");

        stackIdx++;

        if (stackIdx >= stack.length)
            throw new BDecodingException("nesting too deep");

        Token newState = stack[stackIdx];
        newState.start = pos;
        newState.type(t);
        if (t == TokenType.DICT)
            newState.expect(DictState.ExpectKeyOrEnd);
        consumer.push(newState);
    }

    private void pop(int pos) {
        Token current = stack[stackIdx];

        if (current.type() == TokenType.DICT && current.expect() == DictState.ExpectValue)
            throw new BDecodingException("encountered 'e' (offset: " + buf.position() + ") after dictionary key, expected a value");

        current.end = pos;
        consumer.pop(current);

        lastDecodedNum = -1;

        current.reset();
        stackIdx--;

        current = currentToken();

        switch (current.expect()) {
            case ExpectKeyOrEnd:
                current.expect(DictState.ExpectValue);
                break;
            case ExpectValue:
                current.expect(DictState.ExpectKeyOrEnd);
                break;
            default:
                break;
        }
    }

    private void decodeString() {
        long length = this.parseNum(buf, (byte) ':');
        if (length < 0)
            length = 0;
        push(TokenType.STRING, buf.position());

        if (length > buf.remaining())
            throw new BDecodingException("string (offset: " + buf.position() + " + length: " + length + ") points beyond end of message (length: " + buf.limit() + ")");
        buf.position((int) (buf.position() + length));
        pop(buf.position());
    }

    ByteBuffer getSlice(Token t) {
        int oldPos = buf.position();
        buf.position(t.start);
        ByteBuffer slice = buf.slice();
        slice.limit(t.end - t.start);
        buf.position(oldPos);
        return slice;
    }

    int stackIdx() {
        return stackIdx;
    }

    void inputBuffer(ByteBuffer buf) {
        this.buf = buf;
    }

    public void consumer(TokenConsumer c) {
        this.consumer = c;
    }

    void tokenize() {

        while (buf.remaining() > 0) {

            int pos = buf.position();

            byte current = buf.get();

            switch (current) {
                case 'd':
                    push(TokenType.DICT, pos);
                    break;
                case 'i':
                    push(TokenType.LONG, pos);
                    lastDecodedNum = this.parseNum(buf, (byte) 'e');
                    pop(buf.position());
                    break;
                case 'l':
                    push(TokenType.LIST, pos);
                    break;
                case 'e':
                    pop(buf.position());
                    break;
                case '-':
                case '0':
                case '1':
                case '2':
                case '3':
                case '4':
                case '5':
                case '6':
                case '7':
                case '8':
                case '9':
                    push(TokenType.PREFIXED_STRING, pos);
                    buf.position(pos);
                    decodeString();
                    pop(buf.position());
                    break;
                default:
                    StringBuilder b = new StringBuilder();
                    Utils.toHex(new byte[]{current}, b, 1);
                    throw new BDecodingException("unexpected character 0x" + b + " at offset " + (buf.position() - 1));
            }

            if (stackIdx <= 0)
                break;
        }

        if (stackIdx != 0)
            throw new BDecodingException("reached end of data with unterminated lists/dictionaries on the stack");
    }

    private long parseNum(ByteBuffer buf, byte terminator) {
        long result = 0;
        boolean neg = false;

        if (buf.remaining() < 1)
            throw new BDecodingException("end of message reached while decoding a number/string length prefix. offset:" + buf.position());
        byte current = buf.get();

        if (current == '-') {
            neg = true;
            if (buf.remaining() < 1)
                throw new BDecodingException("end of message reached while decoding a number/string length prefix. offset:" + buf.position());
            current = buf.get();
        }

        int iter = 0;

        // do-while since we expect at least one digit
        do {
            // do zero-check on 2nd character, since 0 itself is a valid length
            if (iter > 0 && result == 0)
                throw new BDecodingException("encountered a leading zero at offset " + (buf.position() - 1) + " while decoding a number/string length prefix");

            if (current < '0' || current > '9') {
                StringBuilder b = new StringBuilder();
                Utils.toHex(new byte[]{current}, b, 1);
                throw new BDecodingException("encountered invalid character 0x" + b + " (offset:" + (buf.position() - 1) + ") while decoding a number/string length prefix, expected 0-9 or " + (char) terminator);
            }


            int digit = current - '0';

            result *= 10;
            result += digit;

            if (buf.remaining() < 1)
                throw new BDecodingException("end of message reached while decoding a number/string length prefix. offset:" + buf.position());
            current = buf.get();

            iter++;
        } while (current != terminator);

        if (neg)
            result *= -1;
        return result;
    }

    public enum TokenType {
        LIST, DICT, PREFIXED_STRING, STRING, LONG
    }

    enum DictState {
        NoExpectation,
        ExpectKeyOrEnd,
        ExpectValue
    }

    public interface TokenConsumer {

        void push(Token st);

        void pop(Token st);
    }

    public static class BDecodingException extends RuntimeException {

        BDecodingException(String msg) {
            super(msg);
        }

    }

    public static final class Token {
        private static final TokenType[] tokenEnums = TokenType.values();
        private static final DictState[] stateEnums = DictState.values();
        int start;
        int end;
        byte state;
        byte dictExpect;

        Token() {
            reset();
        }

        void reset() {
            start = -1;
            end = -1;
            state = -1;
            expect(DictState.NoExpectation);
        }

        public TokenType type() {
            return tokenEnums[state];
        }

        void type(TokenType t) {
            state = (byte) t.ordinal();
        }

        DictState expect() {
            return stateEnums[dictExpect];
        }

        void expect(DictState d) {
            dictExpect = (byte) d.ordinal();
        }

        @Override
        public String toString() {
            String st = "Token: " + type();
            if (type() == TokenType.DICT)
                st += " " + expect();
            st += " [" + start + "," + end + "]";

            return st;
        }


    }

}
