package aredis.ext;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

import aredis.BinaryUtil;

/**
 * Created by tianyang on 17/12/13.
 */
public class Util {

    public static final int MAX_10 = 16383;
    public static final int MAX_11 = 4194303;
    public static final byte TOP = 63;

    public static final int MAX_KEY_VALUE_LENGTH = 4194304;


    public static void putOrFlush(FileChannel channel, ByteBuffer byteBuffer, byte[] bytes) throws IOException {
        if (byteBuffer.remaining() < bytes.length) {
            byteBuffer.flip();
            channel.write(byteBuffer);
            byteBuffer.clear();
        }
        byteBuffer.put(bytes);
    }

    public static void putOrFlush(FileChannel channel, ByteBuffer byteBuffer, byte b) throws IOException {
        if (byteBuffer.remaining() == 0) {
            byteBuffer.flip();
            channel.write(byteBuffer);
            byteBuffer.clear();
        }
        byteBuffer.put(b);
    }

    public static void putOrFlush(FileChannel channel, ByteBuffer byteBuffer, long l) throws IOException {
        if (byteBuffer.remaining() < 8) {
            byteBuffer.flip();
            channel.write(byteBuffer);
            byteBuffer.clear();
        }
        byteBuffer.putLong(l);
    }

    public static long getLongOrPull(FileChannel channel, ByteBuffer byteBuffer) throws IOException {

        if (byteBuffer.remaining() < 8) {

            int writeCount = 0;
            byte[] longBytes = new byte[8];
            while (writeCount < 8) {
                int remaining = byteBuffer.remaining();

                if (remaining > 0) {
                    int needCount = (8 - writeCount >= remaining ? remaining : (8 - writeCount));
                    byteBuffer.get(longBytes, writeCount, needCount);
                    writeCount += needCount;
                }

                if (writeCount < 8) {
                    byteBuffer.rewind();
                    channel.read(byteBuffer);
                    byteBuffer.flip();
                }
            }
            return BinaryUtil.bytesToLong(longBytes, 0);

        } else {
            return byteBuffer.getLong();
        }
    }

    public static int getIntOrPull(FileChannel channel, ByteBuffer byteBuffer) throws IOException {

        if (byteBuffer.remaining() < 4) {

            int writeCount = 0;
            byte[] intBytes = new byte[4];
            while (writeCount < 4) {
                int remaining = byteBuffer.remaining();

                if (remaining > 0) {
                    int needCount = (4 - writeCount >= remaining ? remaining : (4 - writeCount));
                    byteBuffer.get(intBytes, writeCount, needCount);
                    writeCount += needCount;
                }

                if (writeCount < 4) {
                    byteBuffer.rewind();
                    channel.read(byteBuffer);
                    byteBuffer.flip();
                }
            }
            return BinaryUtil.bytesToInt(intBytes, 0);

        } else {
            return byteBuffer.getInt();
        }
    }

    public static byte getByteOrPull(FileChannel channel, ByteBuffer byteBuffer) throws IOException {

        if (byteBuffer.remaining() <= 0) {
            byteBuffer.rewind();
            channel.read(byteBuffer);
            byteBuffer.flip();
        }

        return byteBuffer.get();
    }

    public static int getSingleLength(FileChannel channel, ByteBuffer byteBuffer) throws IOException {
        byte top = getByteOrPull(channel, byteBuffer);
        int valueLength = top;

        if (top < 0) {
            String topStr = BinaryUtil.binary2decimal(top & TOP, 8);
            if (top >= -64) {     //start with 11
                byte mid = getByteOrPull(channel, byteBuffer);
                byte bottom = getByteOrPull(channel, byteBuffer);

                String midStr = BinaryUtil.binary2decimal(mid, 8);
                String bottomStr = BinaryUtil.binary2decimal(bottom, 8);
                valueLength = Integer.parseInt(topStr + midStr + bottomStr, 2);

            } else {    //start with 10
                byte bottom = getByteOrPull(channel, byteBuffer);

                String bottomStr = BinaryUtil.binary2decimal(bottom, 8);
                valueLength = Integer.parseInt(topStr + bottomStr, 2);
            }
        }

        return valueLength;
    }

    public static byte[] makeSingleLength(int length) throws ARedisException {

        if (length <= 0) {
            throw new ARedisException(length + " nagetive length %d");
        }
        if (length <= Byte.MAX_VALUE) {
            byte b = (byte) length;
            return new byte[]{b};
        } else if (length <= MAX_10) {
            String result = BinaryUtil.binary2decimal(length, 14);
            byte top = (byte) Integer.parseInt("10" + result.substring(0, 6), 2);
            byte bottom = (byte) Integer.parseInt(result.substring(6), 2);
            return new byte[]{top, bottom};
        } else if (length <= MAX_11) {
            String result = BinaryUtil.binary2decimal(length, 22);
            byte top = (byte) Integer.parseInt("11" + result.substring(0, 6), 2);
            byte mid = (byte) Integer.parseInt(result.substring(6, 14), 2);
            byte bottom = (byte) Integer.parseInt(result.substring(14), 2);

            return new byte[]{top, mid, bottom};

        } else {
            throw new ARedisException(String.format(length + " bits over max value length %d", MAX_KEY_VALUE_LENGTH));
        }
    }

    public static byte[] makeSingleLength(byte[] bytes) throws ARedisException {
        int length = bytes.length;
        if (length <= 0) {
            throw new ARedisException(length + " nagetive length %d");
        }
        if (length <= Byte.MAX_VALUE) {
            byte b = (byte) length;
            return new byte[]{b};
        } else if (length <= MAX_10) {
            String result = BinaryUtil.binary2decimal(length, 14);
            byte top = (byte) Integer.parseInt("10" + result.substring(0, 6), 2);
            byte bottom = (byte) Integer.parseInt(result.substring(6), 2);
            return new byte[]{top, bottom};
        } else if (length <= MAX_11) {
            String result = BinaryUtil.binary2decimal(length, 22);
            byte top = (byte) Integer.parseInt("11" + result.substring(0, 6), 2);
            byte mid = (byte) Integer.parseInt(result.substring(6, 14), 2);
            byte bottom = (byte) Integer.parseInt(result.substring(14), 2);

            return new byte[]{top, mid, bottom};

        } else {
            throw new ARedisException(String.format(length + " bits over max value length %d", MAX_KEY_VALUE_LENGTH));
        }
    }
}
