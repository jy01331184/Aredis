package aredis.persist;

/**
 * Created by tianyang on 17/12/28.
 */
public interface APojoStrategy<T> {

    byte[] toBytes(T object);

    T toObject(byte[] bytes, Class<T> cls);

}
