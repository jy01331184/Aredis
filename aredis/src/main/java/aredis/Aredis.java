package aredis;

import aredis.ext.ARedisException;

/**
 * Created by tianyang on 18/5/23.
 */
public interface Aredis {

    void set(final String key, final Object data, long expire) throws ARedisException;

    Object get(final String key) throws ARedisException;

    void delete(String key) throws ARedisException;

    void ladd(final String key, final Object data) throws ARedisException;

    void sync() throws ARedisException;

}
