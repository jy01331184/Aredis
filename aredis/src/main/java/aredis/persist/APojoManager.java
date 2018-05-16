package aredis.persist;

import java.util.HashMap;
import java.util.Map;

import aredis.BinaryUtil;
import aredis.ext.ARedisException;

/**
 * Created by tianyang on 17/12/28.
 */
public class APojoManager {

    private static APojoStrategy<Object> defalutStrategy = new APojoJsonStrategy();

    private static Map<Class, APojoStrategy> strategyMap = new HashMap<>();
    private static Map<Class, Integer> mappings = new HashMap<>();
    private static Map<Integer, Class> revertMappings = new HashMap<>();

    public static void regist(Class<? extends APersist> cls, int type) throws ARedisException {
        regist(cls, type, null);
    }

    public static void regist(Class<? extends APersist> cls, int type, APojoStrategy<? extends APersist> strategy) throws ARedisException {
        Class oldCls = revertMappings.put(type, cls);
        if (oldCls != null && oldCls != cls) {  //一个type对应多个 cls
            revertMappings.remove(type);
            throw new ARedisException("type:" + type + " with two classes:" + oldCls + "," + cls);
        }
        mappings.put(cls, type);
        if (strategy != null) {
            strategyMap.put(cls, strategy);
        }
    }

    public static void setDefalutStrategy(APojoStrategy<Object> defalutStrategy) {
        APojoManager.defalutStrategy = defalutStrategy;
    }

    public static byte[] getType(Object object) {
        Integer type = mappings.get(object.getClass());

        if (type == null) {
            throw new IllegalArgumentException("no type with class:" + object.getClass());
        }

        return BinaryUtil.intToBytes(type);
    }

    public static byte[] getValue(Object object) {
        APojoStrategy strategy = strategyMap.get(object.getClass());
        if (strategy == null) {
            strategy = defalutStrategy;
        }

        return strategy.toBytes(object);
    }

    public static Object convert(APersistImpl aPersist) {
        //System.out.println("before:"+aPersist);
        Class cls = revertMappings.get(aPersist.getType());

        APojoStrategy strategy = strategyMap.get(cls);
        if (strategy == null) {
            strategy = defalutStrategy;
        }

        Object obj = strategy.toObject(aPersist.getValue(), cls);
        //System.out.println(strategy+":"+obj);
        return obj;
    }
}
