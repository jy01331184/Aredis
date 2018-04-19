package aredis;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.charset.Charset;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import aredis.ext.APersistBytesWrapper;
import aredis.ext.ARedisException;
import aredis.ext.BytesWrapper;
import aredis.ext.CollectionBytesWrapper;
import aredis.ext.DirectBytesWrapper;
import aredis.ext.Util;
import aredis.persist.APersistImpl;
import aredis.persist.APojoManager;

/**
 * Created by tianyang on 17/10/16.
 */
public class AType {

    public static byte TYPE_STRING = 0;
    public static byte TYPE_INT = -1;
    public static byte TYPE_LONG = -2;
    public static byte TYPE_FLOAT = -3;
    public static byte TYPE_DOUBLE = -4;
    public static byte TYPE_BOOL = -5;
    public static byte TYPE_BYTE = -6;
    public static byte TYPE_BYTES = -16;

    public static byte TYPE_SET = -11;
    public static byte TYPE_LIST = -10;
    public static byte TYPE_SERIALIZABLE = -126;
    public static byte TYPE_APERSIST = -127;
    public static byte TYPE_UNKNOW = -128;

    private static final Charset charset = Charset.forName("UTF-8");


    public static byte getType(Object object) {

        if (object instanceof String) {
            return TYPE_STRING;
        } else if (object instanceof Integer) {
            return TYPE_INT;
        } else if (object instanceof Long) {
            return TYPE_LONG;
        } else if (object instanceof Float) {
            return TYPE_FLOAT;
        } else if (object instanceof Double) {
            return TYPE_DOUBLE;
        } else if (object instanceof Boolean) {
            return TYPE_BOOL;
        } else if (object instanceof List) {
            return TYPE_LIST;
        } else if (object instanceof Set) {
            return TYPE_SET;
        } else if (object instanceof Byte) {
            return TYPE_BYTE;
        } else if (object instanceof byte[]) {
            return TYPE_BYTES;
        } else if (object instanceof aredis.persist.APersist) {
            return TYPE_APERSIST;
        } else if (object instanceof Serializable) {
            return TYPE_SERIALIZABLE;
        } else {
            throw new IllegalArgumentException(String.format("unknown type %s", object.getClass().getName()));
        }

    }

    public static BytesWrapper getTypeBytesWrapper(Object object) {
        if (object instanceof String) {
            return DirectBytesWrapper.acquire(((String) object).getBytes(charset));
        } else if (object instanceof Integer) {
            Integer integer = (Integer) object;
            return new DirectBytesWrapper(BinaryUtil.intToBytes(integer));
        } else if (object instanceof Long) {
            Long l = (Long) object;
            return new DirectBytesWrapper(BinaryUtil.longToBytes(l));
        } else if (object instanceof Float) {
            Float f = (Float) object;
            return new DirectBytesWrapper(BinaryUtil.floatToBytes(f));
        } else if (object instanceof Double) {
            Double d = (Double) object;
            return new DirectBytesWrapper(BinaryUtil.doubleToBytes(d));
        } else if (object instanceof Boolean) {
            Boolean b = (Boolean) object;
            return new DirectBytesWrapper(BinaryUtil.booleanToBytes(b));
        } else if (object instanceof Byte) {
            Byte b = (Byte) object;
            return new DirectBytesWrapper(new byte[]{b});
        } else if (object instanceof aredis.persist.APersist) {
            aredis.persist.APersist aPersist = (aredis.persist.APersist) object;
            return new APersistBytesWrapper(aPersist);
        } else if (object instanceof byte[]) {
            byte[] b = (byte[]) object;
            return new DirectBytesWrapper(b);
        } else if (object instanceof List) {
            List list = (List) object;
            return new CollectionBytesWrapper(list);
        } else if (object instanceof Set) {
            Set set = (Set) object;
            return new CollectionBytesWrapper(set);
        } else if (object instanceof Serializable) {
            try {
                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
                objectOutputStream.writeObject(object);
                objectOutputStream.flush();
                DirectBytesWrapper directBytesWrapper = new DirectBytesWrapper(byteArrayOutputStream.toByteArray());

                objectOutputStream.close();
                byteArrayOutputStream.close();
                return directBytesWrapper;

            } catch (IOException e) {
                e.printStackTrace();
                throw new IllegalArgumentException(String.format("error in type seri %s", object.getClass().getName()));
            }
        } else {
            throw new IllegalArgumentException(String.format("unknown type %s", object.getClass().getName()));
        }
    }

    public static byte[] getTypeBytes(Object object) throws ARedisException {
        BytesWrapper wrapper = getTypeBytesWrapper(object);
        if (wrapper.size() == 1) {
            byte[] result = wrapper.get(0);
            release(wrapper);
            return result;
        } else {
            int totalLength = 0;
            for (int i = 0; i < wrapper.size(); i++) {
                totalLength += wrapper.get(i).length;
            }
            byte[] rs = new byte[totalLength];
            int last = 0;
            for (int i = 0; i < wrapper.size(); i++) {
                byte[] temp = wrapper.get(i);
                System.arraycopy(temp, 0, rs, last, temp.length);
                last += temp.length;
            }
            release(wrapper);
            return rs;
        }
    }


    public static Object getValue(byte type, byte[] data, int offset, int length) {

        if (type == TYPE_STRING) {
            return new String(data, offset, length, charset);
        } else if (type == TYPE_INT) {
            return BinaryUtil.bytesToInt(data, offset);
        } else if (type == TYPE_LONG) {
            return BinaryUtil.bytesToLong(data, offset);
        } else if (type == TYPE_FLOAT) {
            return BinaryUtil.bytesToFloat(data, offset);
        } else if (type == TYPE_DOUBLE) {
            return BinaryUtil.bytesToDouble(data, offset);
        } else if (type == TYPE_BOOL) {
            return BinaryUtil.bytesToBoolean(data, offset);
        } else if (type == TYPE_BYTE) {
            return data[offset];
        } else if (type == TYPE_APERSIST) {
            APersistImpl aPersist = new APersistImpl(data, offset, length);
            Object convert = APojoManager.convert(aPersist);
            return convert;
        } else if (type == TYPE_BYTES) {
            byte[] result = new byte[length];
            System.arraycopy(data, offset, result, 0, length);
            return result;
        } else if (type == TYPE_SERIALIZABLE) {
            try {
                ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(data, offset, length);
                ObjectInputStream objectInputStream = new ObjectInputStream(byteArrayInputStream);
                Object obj = objectInputStream.readObject();

                return obj;
            } catch (Exception e) {
                e.printStackTrace();
                throw new IllegalArgumentException(String.format("error in type deseri " + type));
            }

        } else if (type == TYPE_LIST) {
            List<Object> list = new LinkedList<>();

            int index = offset;

            while (index < length + offset) {
                //
                byte childType = data[index++];
                {
                    byte top = data[index++];
                    int valueLength = top;
                    if (top < 0) {
                        String topStr = BinaryUtil.binary2decimal(top & Util.TOP, 8);
                        if (top >= -64) {     //start with 11
                            byte mid = data[index++];
                            byte bottom = data[index++];
                            String midStr = BinaryUtil.binary2decimal(mid, 8);
                            String bottomStr = BinaryUtil.binary2decimal(bottom, 8);
                            valueLength = Integer.parseInt(topStr + midStr + bottomStr, 2);
                        } else {    //start with 10
                            byte bottom = data[index++];
                            String bottomStr = BinaryUtil.binary2decimal(bottom, 8);
                            valueLength = Integer.parseInt(topStr + bottomStr, 2);
                        }
                    }
                    Object child = getValue(childType, data, index, valueLength);
                    index += valueLength;
                    list.add(child);
                }
            }

            return list;
        } else if (type == TYPE_SET) {
            Set<Object> set = new HashSet<>();

            int index = offset;

            while (index < length + offset) {
                //
                byte childType = data[index++];
                {
                    byte top = data[index++];
                    int valueLength = top;
                    if (top < 0) {
                        String topStr = BinaryUtil.binary2decimal(top & Util.TOP, 8);
                        if (top >= -64) {     //start with 11
                            byte mid = data[index++];
                            byte bottom = data[index++];
                            String midStr = BinaryUtil.binary2decimal(mid, 8);
                            String bottomStr = BinaryUtil.binary2decimal(bottom, 8);
                            valueLength = Integer.parseInt(topStr + midStr + bottomStr, 2);
                        } else {    //start with 10
                            byte bottom = data[index++];
                            String bottomStr = BinaryUtil.binary2decimal(bottom, 8);
                            valueLength = Integer.parseInt(topStr + bottomStr, 2);
                        }
                    }
                    Object child = getValue(childType, data, index, valueLength);
                    index += valueLength;
                    set.add(child);
                }
            }

            return set;
        } else {
            throw new IllegalArgumentException(String.format("unknown type %s", type));
        }
    }


    private static void release(BytesWrapper bytesWrapper) {
        if (bytesWrapper instanceof DirectBytesWrapper) {
            DirectBytesWrapper.release((DirectBytesWrapper) bytesWrapper);
        }
    }
}
