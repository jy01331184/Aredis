package aredis.ext;

import aredis.persist.APersist;
import aredis.persist.APojoManager;

/**
 * Created by tianyang on 17/12/27.
 */
public class APersistBytesWrapper implements BytesWrapper {

    private byte[] values;

    public APersistBytesWrapper(APersist aPersist) {
        byte[] value = APojoManager.getValue(aPersist);
        byte[] types = APojoManager.getType(aPersist);
        values = new byte[value.length+types.length];
        System.arraycopy(types,0,values,0,types.length);
        System.arraycopy(value,0,values,types.length,value.length);
    }

    @Override
    public int size() {
        return 1;
    }

    @Override
    public byte[] get(int index) {
        return values;
    }

    @Override
    public byte[] getHeader() throws ARedisException {
        return Util.makeSingleLength(values);
    }
}
