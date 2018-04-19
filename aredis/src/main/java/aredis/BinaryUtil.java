package aredis;

/**
 * Created by tianyang on 17/10/16.
 */
public class BinaryUtil {

    public static String binary2decimal(int decNum, int digit) {
        String binStr = "";
        for (int i = digit - 1; i >= 0; i--) {
            binStr += (decNum >> i) & 1;
        }
        return binStr;
    }

    public static byte[] intToBytes(int value) {
        byte[] src = new byte[4];
        src[0] = (byte) ((value >> 24) & 0xFF);
        src[1] = (byte) ((value >> 16) & 0xFF);
        src[2] = (byte) ((value >> 8) & 0xFF);
        src[3] = (byte) (value & 0xFF);
        return src;
    }

    public static int bytesToInt(byte[] src, int offset) {
        int value;
        value = (int) (((src[offset] & 0xFF) << 24)
                | ((src[offset + 1] & 0xFF) << 16)
                | ((src[offset + 2] & 0xFF) << 8)
                | (src[offset + 3] & 0xFF));
        return value;
    }

    public static long bytesToLong(byte[] src, int offset) {
        long values = 0;
        for (int i = 0; i < 8; i++) {
            values <<= 8;
            values |= (src[i + offset] & 0xff);
        }
        return values;
    }

    public static byte[] longToBytes(long values) {
        byte[] buffer = new byte[8];
        for (int i = 0; i < 8; i++) {
            int offset = 64 - (i + 1) * 8;
            buffer[i] = (byte) ((values >> offset) & 0xff);
        }
        return buffer;
    }

    public static float bytesToFloat(byte[] src, int offset) {
        return Float.intBitsToFloat(bytesToInt(src,offset));
    }

    public static byte[] floatToBytes(float values) {
        int r = Float.floatToIntBits(values);
        return intToBytes(r);
    }

    public static double bytesToDouble(byte[] src, int offset) {
        return Double.longBitsToDouble(bytesToLong(src,offset));
    }

    public static byte[] doubleToBytes(double values) {
        long r = Double.doubleToLongBits(values);
        return longToBytes(r);
    }

    public static boolean bytesToBoolean(byte[] src, int offset) {
        return src[offset] == 1;
    }

    public static byte[] booleanToBytes(boolean values) {
        byte[] bytes = new byte[1];
        bytes[0] = (byte) (values?1:0);
        return bytes;
    }


}
