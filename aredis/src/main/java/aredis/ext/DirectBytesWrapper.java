package aredis.ext;

/**
 * Created by tianyang on 17/12/13.
 */
public class DirectBytesWrapper implements BytesWrapper {

    private static final Pools.Pool<DirectBytesWrapper> DIRECT_BYTES_WRAPPER_POOL = new Pools.SimplePool<>(10);

    private byte[] bytes;

    public static DirectBytesWrapper acquire(byte[] bytes) {
        DirectBytesWrapper directBytesWrapper = DIRECT_BYTES_WRAPPER_POOL.acquire();

        if (directBytesWrapper == null) {
            directBytesWrapper = new DirectBytesWrapper(bytes);
        }
        directBytesWrapper.bytes = bytes;

        return directBytesWrapper;
    }

    public static void release(DirectBytesWrapper directBytesWrapper) {
        directBytesWrapper.bytes = null;
        DIRECT_BYTES_WRAPPER_POOL.release(directBytesWrapper);
    }

    public DirectBytesWrapper(byte[] bytes) {
        this.bytes = bytes;
    }

    @Override
    public int size() {
        return 1;
    }

    @Override
    public byte[] get(int index) {
        return bytes;
    }

    @Override
    public byte[] getHeader() throws ARedisException {
        return Util.makeSingleLength(bytes);
    }

}
