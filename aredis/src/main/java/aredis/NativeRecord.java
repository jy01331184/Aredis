package aredis;

import aredis.ext.Pools;

/**
 * Created by tianyang on 17/11/23.
 */
public class NativeRecord {

    public byte aofType = 1;

    public byte type;

    public long expire;

    public byte[] value;

    private NativeRecord() {
    }

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

}
