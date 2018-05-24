package aredis;

import android.content.Context;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import aredis.cfg.AredisConfig;
import aredis.ext.ARedisException;
import aredis.process.ProcessSupport;

/**
 * Created by magic.yang
 * AREDIS IMPL
 */

class ARedisCache implements Aredis, ProcessSupport {
    static final int STATE_LOADING = 1, STATE_LOADED = 2, STATE_FAIL = -1, STATE_CONCURRENT = -2;

    private long last_rdb_save_time;
    private int max_io_wait_time = 1000;
    private long rdb_save_time = 10 * 60 * 1000;//10 min
    private int rdb_change_count = 0;
    private int rdb_save_count = 50;
    private boolean persist = true;
    private boolean rdb = false;
    private boolean aof = false;
    private boolean strictMode = false;
    private volatile int initState = 0;
    private int nativePtr;
    private String name;
    private Native mANative;
    private ReentrantLock lock = new ReentrantLock();
    private Condition initCondition = lock.newCondition();

    private Native.NativeListener nativeListener = new Native.NativeListener() {
        @Override
        public void onResult(int result) {

            switch (result) {
                case STATE_CONCURRENT:
                case STATE_FAIL:
                case STATE_LOADING:
                case STATE_LOADED:
                    initState = result;
                    break;
                default:
                    initState = STATE_LOADED;
                    nativePtr = result;
                    break;
            }

        }
    };

    private ExecutorService executorService = new ThreadPoolExecutor(1, 1,
            60L, TimeUnit.SECONDS,
            new LinkedBlockingQueue<Runnable>(),
            new ThreadFactory() {
                @Override
                public Thread newThread(Runnable r) {
                    return new Thread(r, "Native Pool");
                }
            });

    ARedisCache(Context context, String kvname, AredisConfig config) {

        rdb = config.getType() == AredisConfig.AredisType.RDB;
        aof = config.getType() == AredisConfig.AredisType.AOF;
        rdb_save_time = config.getRdbSaveInterval();
        rdb_save_count = config.getRdbSaveCount();
        strictMode = config.useAofStrictMode();
        persist = config.persist();

        name = new File(context.getFilesDir(), kvname).getAbsolutePath();
        mANative = new Native(lock, executorService);

        if (aof) {
            mANative.initReadAOF(name, nativeListener, initCondition);
        } else if (rdb) {
            mANative.initReadRDB(name, nativeListener, initCondition);
        } else {
            throw new RuntimeException("mode must be least one of aof or rdb");
        }
    }

    public synchronized void set(final String key, final Object data, long expire) throws ARedisException {
        checkInit();

        NativeRecord record = mANative.setNative(name, nativePtr, key, data, expire);
        if (!persist) {
            NativeRecord.release(record);
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
                if (mANative.nativeForkRDB(name, nativePtr)) {
                    last_rdb_save_time = System.currentTimeMillis();
                }
            }
        }
        NativeRecord.release(record);
    }

    public synchronized void setRaw(String key, byte type, byte[] data, long expire) throws ARedisException {
        checkInit();
        NativeRecord record = mANative.setRawNative(name, nativePtr, key, type, data, expire);
        if (aof) {
            if (strictMode) {
                strictAof(key, record, max_io_wait_time);
            } else {
                mANative.appendAOF(name, key, record);
            }
        } else if (rdb) {
            rdb_change_count++;
            if (rdb_change_count % rdb_save_count == 0 || (last_rdb_save_time > 0 && last_rdb_save_time - System.currentTimeMillis() > rdb_save_time)) {
                if (mANative.nativeForkRDB(name, nativePtr)) {
                    last_rdb_save_time = System.currentTimeMillis();
                }
            }
        }
        NativeRecord.release(record);
    }

    public synchronized void ladd(final String key, final Object data) throws ARedisException {
        checkInit();

        NativeRecord record = mANative.laddNative(name, nativePtr, key, data);
        if (!persist) {
            NativeRecord.release(record);
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
                if (mANative.nativeForkRDB(name, nativePtr)) {
                    last_rdb_save_time = System.currentTimeMillis();
                }
            }
        }
        NativeRecord.release(record);
    }

    public synchronized Object get(final String key) throws ARedisException {
        checkInit();
        return mANative.getNative(name, nativePtr, key);
    }

    public synchronized NativeRecord getRaw(String key) throws ARedisException {
        checkInit();
        return mANative.getRaw(name, nativePtr, key);
    }

    public synchronized void delete(String key) throws ARedisException {
        checkInit();
        mANative.removeNative(name, nativePtr, key);

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
                if (mANative.nativeForkRDB(name, nativePtr)) {
                    last_rdb_save_time = System.currentTimeMillis();
                }
            }
        }
    }

    public synchronized void sync() throws ARedisException {
        checkInit();
        if (!persist) {
            return;
        }
        if (aof) {
            mANative.syncAOF(name, nativePtr);
        } else if (rdb) {
            mANative.syncRDB(name, nativePtr);
        }
    }

    private void strictAof(String key, NativeRecord record, int max_io_wait_time) throws ARedisException {
        try {
            lock.lock();
            Condition aofWriteCondition = lock.newCondition();
            mANative.appendStrictAOF(name, key, record, lock, aofWriteCondition);
            boolean success = aofWriteCondition.await(max_io_wait_time, TimeUnit.MILLISECONDS);
            if (!success) {
                throw new ARedisException("write strict aof overtime :" + name + " with key:" + key);
            }
        } catch (InterruptedException e) {
            ARedisException exception = new ARedisException("write strict aof fail :" + name + " with key:" + key);
            exception.initCause(e);
            throw exception;
        }
    }

    private void checkInit() throws ARedisException {
        if (initState == STATE_LOADED) {
            return;
        } else if (initState == STATE_FAIL) {
            throw new ARedisException("aredis init fail :" + name);
        } else if (initState == STATE_LOADING) {
            lock.lock();
            if (initState == STATE_LOADING) {
                boolean overtime;
                try {
                    overtime = !initCondition.await(max_io_wait_time, TimeUnit.MILLISECONDS);
                } catch (InterruptedException e) {
                    ARedisException exception = new ARedisException("aredis init interrupt");
                    exception.initCause(e);
                    lock.unlock();
                    throw exception;
                }

                if (overtime && initState != STATE_LOADED) {
                    lock.unlock();
                    throw new ARedisException("aredis init over time :" + name);
                } else if (initState == STATE_CONCURRENT) {
                    throw new ARedisException("aredis not support more than one process :" + name);
                } else if (initState != STATE_LOADED) {
                    lock.unlock();
                    throw new ARedisException("aredis init fail :" + name);
                }
            } else if (initState == STATE_FAIL) {
                lock.unlock();
                throw new ARedisException("aredis init fail :" + name);
            } else if (initState == STATE_CONCURRENT) {
                lock.unlock();
                throw new ARedisException("aredis not support more than one process :" + name);
            }
            lock.unlock();
        } else if (initState == STATE_CONCURRENT) {
            throw new ARedisException("aredis not support more than one process :" + name);
        }
    }
}
