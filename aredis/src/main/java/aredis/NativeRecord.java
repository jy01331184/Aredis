package aredis;

import android.os.Parcel;
import android.os.Parcelable;

import aredis.ext.Pools;

/**
 * Created by tianyang on 17/11/23.
 */
public class NativeRecord implements Parcelable {

    public transient byte aofType = 1;

    public byte type;

    public long expire;

    public byte[] value;

    private NativeRecord() {
    }

    protected NativeRecord(Parcel in) {
        type = in.readByte();

        int length = in.readInt();

        if (length > 0) {
            value = new byte[length];
            in.readByteArray(value);
        }
    }

    public static final Creator<NativeRecord> CREATOR = new Creator<NativeRecord>() {
        @Override
        public NativeRecord createFromParcel(Parcel in) {
            return new NativeRecord(in);
        }

        @Override
        public NativeRecord[] newArray(int size) {
            return new NativeRecord[size];
        }
    };

    public static NativeRecord acquire() {
        NativeRecord record = recordPool.acquire();
        if (record == null) {
            record = new NativeRecord();
        }

        return record;
    }

    public static void release(NativeRecord nativeRecord) {
        recordPool.release(nativeRecord);
    }

    private static Pools.Pool<NativeRecord> recordPool = new Pools.SimplePool<NativeRecord>(5);

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeByte(type);
        if (value != null) {
            dest.writeInt(value.length);
            dest.writeByteArray(value);
        } else {
            dest.writeInt(0);
        }

    }
}
