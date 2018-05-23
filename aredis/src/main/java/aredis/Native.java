package aredis;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import aredis.ext.ARedisException;

/**
 * Created by tianyang on 17/11/17.
 */
public class Native {

    private ExecutorService executorService;
    private boolean available = true;
    private ReentrantLock lock;
    private boolean waitTask = false;

    static {
        try {
            System.loadLibrary("stlport_shared");
            System.loadLibrary("aredis");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Native(ReentrantLock lock, ExecutorService executorService) {
        this.lock = lock;
        this.executorService = executorService;
    }

    public void initReadAOF(final String name, final NativeListener listener, final Condition condition) {
        listener.onResult(ARedisCache.STATE_LOADING);
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    long time = System.currentTimeMillis();
                    int result = readAof(name);
                    System.out.println("init use time:" + (System.currentTimeMillis() - time));

                    lock.lock();
                    listener.onResult(result);
                    condition.signalAll();
                    lock.unlock();
                } catch (Exception e) {
                    lock.lock();
                    listener.onResult(ARedisCache.STATE_FAIL);
                    condition.signalAll();
                    lock.unlock();
                    e.printStackTrace();
                }
            }
        });
    }

    public void initReadRDB(final String name, final NativeListener listener, final Condition condition) {
        listener.onResult(ARedisCache.STATE_LOADING);
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    long time = System.currentTimeMillis();
                    int result = readRdb(name);
                    System.out.println("init use time:" + (System.currentTimeMillis() - time));
                    lock.lock();
                    listener.onResult(result);
                    condition.signalAll();
                    lock.unlock();
                } catch (Exception e) {
                    e.printStackTrace();
                    lock.lock();
                    listener.onResult(ARedisCache.STATE_FAIL);
                    condition.signalAll();
                    lock.unlock();
                }
            }
        });
    }

    /**
     * 使用native存储形式,fork进程写rdb
     */
    public boolean nativeForkRDB(final String name, final int ptr) throws ARedisException {
        if (available) {
            available = false;
            try {
                executorService.execute(new Runnable() {
                    @Override
                    public void run() {
                        asyncDoForkTask(name, ptr);
                    }
                });
            } catch (Throwable throwable) {
                available = true;
                ARedisException exception = new ARedisException("nativeForkRDB fail :" + name);
                exception.initCause(throwable);
                throw exception;
            }
            return true;
        } else {
            waitTask = true;
            return false;
        }
    }

    private void asyncDoForkTask(String name, int ptr) {
        try {
            if (lock.tryLock()) {
                System.out.println("start fork:" + Thread.currentThread().getName() + "-" + name);
                forkNative(name, ptr);
                System.out.println("done:" + Thread.currentThread().getName() + "-" + name);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (waitTask == true) {
                waitTask = false;
                asyncDoForkTask(name, ptr);
            } else {
                available = true;
            }
            lock.unlock();
        }
    }

    public void syncRDB(String name, int ptr) throws ARedisException {
        try {
            if (lock.tryLock(1000, TimeUnit.MILLISECONDS)) {
                try {
                    syncRdb(name, ptr);
                    waitTask = false;
                } catch (Throwable throwable) {
                    ARedisException exception = new ARedisException("syncRDB " + name + " fail");
                    exception.initCause(throwable);
                    throw exception;
                } finally {
                    lock.unlock();
                }
            } else {
                throw new ARedisException("syncRDB " + name + " over time");
            }
        } catch (InterruptedException e) {
            ARedisException exception = new ARedisException("syncRDB " + name + " fail");
            exception.initCause(e);
            throw exception;
        }

    }

    public void syncAOF(String name, int ptr) throws ARedisException {
        try {
            if (lock.tryLock(1000, TimeUnit.MILLISECONDS)) {
                try {
                    syncAof(name, ptr);
                } catch (Throwable throwable) {
                    ARedisException exception = new ARedisException("syncAOF " + name + " fail");
                    exception.initCause(throwable);
                    throw exception;
                } finally {
                    lock.unlock();
                }
            } else {
                throw new ARedisException("syncAOF " + name + " over time");
            }
        } catch (InterruptedException e) {
            ARedisException exception = new ARedisException("syncAOF " + name + " fail");
            exception.initCause(e);
            throw exception;
        }
    }

    public void appendStrictAOF(final String name, final String key, final NativeRecord record, final ReentrantLock lock, final Condition condition) {
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    lock.lock();
                    if (record == null) {
                        deleteAof(name, key);
                    } else {
                        writeAof(name, key, record);
                    }
                } catch (Throwable throwable) {
                    throwable.printStackTrace();
                } finally {
                    try {
                        condition.signal();
                        lock.unlock();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    public void appendAOF(final String name, final String key, final NativeRecord record) {
        if (record == null) {
            deleteAof(name, key);
        } else {
            writeAof(name, key, record);
        }
    }

    public static synchronized NativeRecord setNative(String name, int ptr, String key, Object value, long expire) throws ARedisException {
        byte type = AType.getType(value);
        byte[] values = AType.getTypeBytes(value);
        set(name, ptr, key, type, values, expire);
        NativeRecord record = NativeRecord.acquire();
        record.type = type;
        record.value = values;
        record.expire = expire;
        return record;
    }

    public static synchronized NativeRecord setRawNative(String name, int ptr, String key, byte type, byte[] values, long expire) throws ARedisException {
        set(name, ptr, key, type, values, expire);
        NativeRecord record = NativeRecord.acquire();
        record.type = type;
        record.value = values;
        record.expire = expire;
        return record;
    }

    public static synchronized NativeRecord laddNative(String name, int ptr, String key, Object value) throws ARedisException {
        byte type = AType.getType(value);
        byte[] values = AType.getTypeBytes(value);
//        byte[] len = Util.makeSingleLength(values);
//        byte[] wrappedValue = new byte[1 + len.length + values.length];
//        wrappedValue[0] = type;
//        System.arraycopy(len, 0, wrappedValue, 1, len.length);
//        System.arraycopy(values, 0, wrappedValue, 1 + len.length, values.length);

        NativeRecord record = ladd(name, ptr, key, type, values);

        return record;
    }

    public static synchronized Object getNative(String name, int ptr, String key) throws ARedisException {
        NativeRecord record = get(name, ptr, key);

        if (record != null) {
            Object object = AType.getValue(record.type, record.value, 0, record.value.length);
            NativeRecord.release(record);
            return object;
        }

        return null;
    }

    public static synchronized NativeRecord getRaw(String name, int ptr, String key) throws ARedisException {
        NativeRecord record = get(name, ptr, key);

        return record;
    }

    public static void removeNative(String name, int ptr, String key) {
        remove(name, ptr, key);
    }


    /**
     * 本地方法区
     */

    private native int readAof(String name);

    private native int readRdb(String name);

    private static native void writeAof(String name, String key, NativeRecord record);

    private static native void deleteAof(String name, String key);

    private static native void syncAof(String name, int ptr);

    private static native void syncRdb(String name, int ptr);

    private static native void set(String name, int ptr, String key, byte type, byte[] value, long expire);

    private static native NativeRecord ladd(String name, int ptr, String key, byte type, byte[] value);

    private static native NativeRecord get(String name, int ptr, String key);

    private static native void remove(String name, int ptr, String key);

    private static native int forkNative(String cache, int ptr);


    static interface NativeListener {
        void onResult(int result);
    }
}
