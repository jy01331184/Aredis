package aredis.ext;

/**
 * Created by tianyang on 17/12/13.
 */
public interface BytesWrapper {

    int size();

    byte[] get(int index) throws ARedisException;

    byte[] getHeader() throws ARedisException;
}
