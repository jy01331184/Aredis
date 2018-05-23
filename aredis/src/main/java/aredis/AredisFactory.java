package aredis;

import android.content.Context;

import java.util.HashMap;
import java.util.Map;

import aredis.cfg.AredisConfig;
import aredis.cfg.AredisDefaultConfig;
import aredis.process.AredisListener;
import aredis.process.ProcessSupport;

/**
 * Created by tianyang on 18/5/23.
 */
public class AredisFactory {

    private static Map<String, ARedisCache> caches = new HashMap<>();
    private static Map<String, ProcessARedisCache> processCaches = new HashMap<>();


    static synchronized ProcessSupport getProcessSupport(Context context, String name, AredisConfig config) {
        ARedisCache redis = caches.get(name);

        if (redis == null && config != null) {
            redis = new ARedisCache(context, name, config);
            caches.put(name, redis);
        }

        return redis;
    }

    public static synchronized Aredis create(Context context, String name) {

        ARedisCache redis = caches.get(name);
        if (redis == null) {
            redis = new ARedisCache(context, name, new AredisDefaultConfig());
            caches.put(name, redis);
        }

        return redis;
    }

    public static synchronized Aredis create(Context context, String name, AredisConfig config) {
        ARedisCache redis = caches.get(name);
        if (redis == null) {
            redis = new ARedisCache(context, name, config);
            caches.put(name, redis);
        }

        return redis;
    }

    public static synchronized Aredis createProcessAredis(Context context, String name, AredisListener listener) {
        ProcessARedisCache redis = processCaches.get(name);
        if (redis == null) {
            redis = new ProcessARedisCache(context, name, listener);
            processCaches.put(name, redis);
        }

        return redis;
    }

    public static synchronized Aredis createProcessAredis(Context context, String name, AredisListener listener, AredisConfig config) {
        ProcessARedisCache redis = processCaches.get(name);
        if (redis == null) {
            redis = new ProcessARedisCache(context, name, listener, config);
            processCaches.put(name, redis);
        }

        return redis;
    }
}
