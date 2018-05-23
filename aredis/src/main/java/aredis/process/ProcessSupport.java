package aredis.process;

import aredis.NativeRecord;
import aredis.ext.ARedisException;

/**
 * Created by tianyang on 18/5/23.
 */
public interface ProcessSupport {

    NativeRecord getRaw(String key) throws ARedisException;

    void setRaw(String key, byte type, byte[] data, long expire) throws ARedisException;



}
