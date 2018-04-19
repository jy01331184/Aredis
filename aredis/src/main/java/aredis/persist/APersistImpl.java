package aredis.persist;

import java.util.Arrays;

import aredis.BinaryUtil;

/**
 * Created by tianyang on 17/12/27.
 */
public class APersistImpl implements APersist {

    private int type;
    private byte[] value;

    public APersistImpl(byte[] data,int offset,int length) {
        this.type = BinaryUtil.bytesToInt(data,offset);

        this.value = new byte[length - 4];
        System.arraycopy(data,offset+4,this.value,0,length-4);
    }


    public int getType() {
        return type;
    }

    public byte[] getValue() {
        return value;
    }

    @Override
    public String toString() {
        return "APersistImpl{" +
                "type=" + type +
                ", value=" + Arrays.toString(value) +
                '}';
    }
}
