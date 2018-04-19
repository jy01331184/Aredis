package aredis;

import android.content.Context;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import aredis.cfg.AredisConfig;
import aredis.cfg.AredisDefaultConfig;
import aredis.ext.ARedisException;

/**
 * Created by magic.yang
 * AREDIS IMPL
 */

public class ARedisCache {
    static final int STATE_LOADING = 1, STATE_LOADED = 2, STATE_FAIL = -1;
    private static Map<String, ARedisCache> caches = new HashMap<>();

    private long last_rdb_save_time;
    private int max_io_wait_time = 1000;
    private long rdb_save_time = 10 * 60 * 1000;//10 min
    private int rdb_change_count = 0;
    private int rdb_save_count = 50;
    private boolean persist = true;
    private boolean rdb = false;
    private boolean aof = false;
    private boolean strictMode = false;
    private AtomicInteger initState = new AtomicInteger(0);
    private String name;
    private Native mANative;
    private ReentrantLock lock = new ReentrantLock();
    private Condition initCondition = lock.newCondition();


    private ExecutorService executorService = new ThreadPoolExecutor(1, 1,
            60L, TimeUnit.SECONDS,
            new LinkedBlockingQueue<Runnable>(),
            new ThreadFactory() {
                @Override
                public Thread newThread(Runnable r) {
                    return new Thread(r, "Native Pool");
                }
            });

//    private ExecutorService executorService = Executors.newSingleThreadExecutor(new ThreadFactory() {
//        @Override
//        public Thread newThread(Runnable r) {
//            return new Thread(r, "Native Pool");
//        }
//    });

    public static synchronized ARedisCache create(Context context, String name) {
        ARedisCache redis = caches.get(name);
        if (redis == null) {
            redis = new ARedisCache(context, name, new AredisDefaultConfig());
            caches.put(name, redis);
        }

        return redis;
    }

    public static synchronized ARedisCache create(Context context, String name, AredisConfig config) {
        ARedisCache redis = caches.get(name);
        if (redis == null) {
            redis = new ARedisCache(context, name, config);
            caches.put(name, redis);
        }

        return redis;
    }

    private ARedisCache(Context context, String kvname, AredisConfig config) {

        rdb = config.getType() == AredisConfig.AredisType.RDB;
        aof = config.getType() == AredisConfig.AredisType.AOF;
        rdb_save_time = config.getRdbSaveInterval();
        rdb_save_count = config.getRdbSaveCount();
        strictMode = config.useAofStrictMode();
        persist = config.persist();

        name = new File(context.getFilesDir(), kvname).getAbsolutePath();
        mANative = new Native(lock, executorService);

        if (aof) {
            mANative.initReadAOF(name, initState, initCondition);
        } else if (rdb) {
            mANative.initReadRDB(name, initState, initCondition);
        } else {
            throw new RuntimeException("mode must be least one of aof or rdb");
        }


    }

    public synchronized void set(final String key, final Object data, long expire) throws Exception {
        checkInit();

        NativeRecord record = Native.setNative(name, key, data, expire);
        if (!persist) {
            return;
        }

        if (aof) {
            if (strictMode) {
                strictAof(key, record, max_io_wait_time);
            } else {
                mANative.appendAOF(name, key, record);
            }
        } else if (rdb) {
            rdb_change_count++;
            if (rdb_change_count % rdb_save_count == 0 || (last_rdb_save_time > 0 && last_rdb_save_time - System.currentTimeMillis() > rdb_save_time)) {
                if (mANative.nativeForkRDB(name)) {
                    last_rdb_save_time = System.currentTimeMillis();
                }
            }
        }
        NativeRecord.release(record);
    }

    public synchronized void ladd(final String key, final Object data) throws Exception {
        checkInit();

        NativeRecord record = Native.laddNative(name, key, data);
        if (!persist) {
            return;
        }

        if (aof) {
            if (strictMode) {
                strictAof(key, record, max_io_wait_time);
            } else {
                mANative.appendAOF(name, key, record);
            }
        } else if (rdb) {
            rdb_change_count++;
            if (rdb_change_count % rdb_save_count == 0 || (last_rdb_save_time > 0 && last_rdb_save_time - System.currentTimeMillis() > rdb_save_time)) {
                if (mANative.nativeForkRDB(name)) {
                    last_rdb_save_time = System.currentTimeMillis();
                }
            }
        }
        NativeRecord.release(record);
    }

    public synchronized Object get(final String key) throws Exception {
        checkInit();
        return Native.getNative(name, key);
    }

    public synchronized void delete(String key) throws Exception {
        checkInit();
        Native.removeNative(name, key);

        if (!persist) {
            return;
        }

        if (aof) {
            if (strictMode) {
                strictAof(key, null, max_io_wait_time);
            } else {
                mANative.appendAOF(name, key, null);
            }
        } else if (rdb) {
            rdb_change_count++;
            if (rdb_change_count % rdb_save_count == 0 || (last_rdb_save_time > 0 && last_rdb_save_time - System.currentTimeMillis() > rdb_save_time)) {
                if (mANative.nativeForkRDB(name)) {
                    last_rdb_save_time = System.currentTimeMillis();
                }
            }
        }
    }

    public synchronized void sync() throws Exception {
        checkInit();
        if (!persist) {
            return;
        }
        if (aof) {
            mANative.syncAOF(name);
        } else if (rdb) {
            mANative.syncRDB(name);
        }
    }

    private void strictAof(String key, NativeRecord record, int max_io_wait_time) throws Exception {
        try {
            lock.lock();
            Condition aofWriteCondition = lock.newCondition();
            mANative.appendStrictAOF(name, key, record, lock, aofWriteCondition);
            boolean success = aofWriteCondition.await(max_io_wait_time, TimeUnit.MILLISECONDS);
            if (!success) {
                System.err.println("write aof overtime :" + name);
            }
        } catch (Exception e) {
            throw e;
        } finally {
            lock.unlock();
        }
    }

    private void checkInit() throws Exception {
        if (initState.get() == STATE_LOADED) {
            return;
        } else if (initState.get() == STATE_FAIL) {
            throw new ARedisException("aredis init fail :" + name);
        } else if (initState.get() == STATE_LOADING) {
            lock.lock();
            if (initState.get() == STATE_LOADING) {
                initCondition.await(max_io_wait_time, TimeUnit.MILLISECONDS);
                if (initState.get() != STATE_LOADED) {
                    lock.unlock();
                    throw new ARedisException("aredis init wait overtime :" + name);
                }
            } else if (initState.get() == STATE_FAIL) {
                lock.unlock();
                throw new ARedisException("aredis init fail :" + name);
            }
            lock.unlock();
        }
    }
}
