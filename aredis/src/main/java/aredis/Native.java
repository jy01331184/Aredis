package aredis;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

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

    public void initReadAOF(final String name, final AtomicInteger initState, final Condition condition) {
        initState.set(ARedisCache.STATE_LOADING);
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    long time = System.currentTimeMillis();
                    int result = readAof(name);
                    System.out.println("init use time:" + (System.currentTimeMillis() - time));
                    lock.lock();
                    initState.set(result);
                    condition.signalAll();
                    lock.unlock();
                } catch (Exception e) {
                    lock.lock();
                    initState.set(ARedisCache.STATE_FAIL);
                    condition.signalAll();
                    lock.unlock();
                    e.printStackTrace();
                }
            }
        });
    }

    public void initReadRDB(final String name, final AtomicInteger initState, final Condition condition) {
        initState.set(ARedisCache.STATE_LOADING);
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    long time = System.currentTimeMillis();
                    int result = readRdb(name);
                    System.out.println("init use time:" + (System.currentTimeMillis() - time));
                    lock.lock();
                    initState.set(result);
                    condition.signalAll();
                    lock.unlock();
                } catch (Exception e) {
                    e.printStackTrace();
                    lock.lock();
                    initState.set(ARedisCache.STATE_FAIL);
                    condition.signalAll();
                    lock.unlock();
                }
            }
        });
    }

    /**
     * 使用native存储形式,fork进程写rdb
     */
    public boolean nativeForkRDB(final String name) throws Exception {
        if (available) {
            available = false;
            try {
                executorService.execute(new Runnable() {
                    @Override
                    public void run() {
                        asyncDoForkTask(name);
                    }
                });
            } catch (Throwable throwable) {
                available = true;
                throwable.printStackTrace();
            }
            return true;
        } else {
            waitTask = true;
            return false;
        }
    }

    private void asyncDoForkTask(String name) {
        try {
            Thread.sleep(2000);
            if (lock.tryLock()) {
                System.out.println("start fork:" + Thread.currentThread().getName() + "-" + name);
                forkNative(name);
                System.out.println("done:" + Thread.currentThread().getName() + "-" + name);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (waitTask == true) {
                waitTask = false;
                asyncDoForkTask(name);
            } else {
                available = true;
            }
            lock.unlock();
        }
    }

    public boolean syncRDB(String name) throws Exception {
        if (lock.tryLock(1000, TimeUnit.MILLISECONDS)) {
            try {
                syncRdb(name);
                waitTask = false;
            } catch (Throwable throwable) {
                throwable.printStackTrace();
                return false;
            } finally {
                lock.unlock();
            }
            return true;
        } else {
            return false;
        }
    }

    public boolean syncAOF(String name) throws Exception {
        if (!available) {
            return false;
        }

        try {
            syncAof(name);
        } catch (Throwable throwable) {
            throwable.printStackTrace();
            return false;
        }
        return true;
    }

    public void appendStrictAOF(final String name, final String key, final NativeRecord record, final ReentrantLock lock, final Condition condition) {
        available = false;
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    if (record == null) {
                        deleteAof(name, key);
                    } else {
                        writeAof(name, key, record);
                    }
                } catch (Throwable throwable) {
                    throwable.printStackTrace();
                } finally {
                    try {
                        lock.lock();
                        condition.signal();
                        available = true;
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

    public static synchronized NativeRecord setNative(String name, String key, Object value, long expire) throws Exception {
        byte type = AType.getType(value);
        byte[] values = AType.getTypeBytes(value);
        set(name, key, type, values, expire);
        NativeRecord record = NativeRecord.acquire();
        record.type = type;
        record.value = values;
        record.expire = expire;
        return record;
    }

    public static synchronized NativeRecord setRawNative(String name, String key, byte type, byte[] values, long expire) throws Exception {
        set(name, key, type, values, expire);
        NativeRecord record = NativeRecord.acquire();
        record.type = type;
        record.value = values;
        record.expire = expire;
        return record;
    }


    public static synchronized NativeRecord laddNative(String name, String key, Object value) throws Exception {
        byte type = AType.getType(value);
        byte[] values = AType.getTypeBytes(value);
//        byte[] len = Util.makeSingleLength(values);
//        byte[] wrappedValue = new byte[1 + len.length + values.length];
//        wrappedValue[0] = type;
//        System.arraycopy(len, 0, wrappedValue, 1, len.length);
//        System.arraycopy(values, 0, wrappedValue, 1 + len.length, values.length);

        NativeRecord record = ladd(name, key, type, values);

        return record;
    }

    public static synchronized Object getNative(String name, String key) throws Exception {
        NativeRecord record = get(name, key);

        if (record != null) {
            Object object = AType.getValue(record.type, record.value, 0, record.value.length);
            NativeRecord.release(record);
            return object;
        }

        return null;
    }

    public static synchronized NativeRecord getRaw(String name, String key) throws Exception {
        NativeRecord record = get(name, key);

        return record;
    }

    public static void removeNative(String name, String key) {
        remove(name, key);
    }


    /**
     * 本地方法区
     */

    private native int readAof(String name);

    private native int readRdb(String name);

    private static native void writeAof(String name, String key, NativeRecord record);

    private static native void deleteAof(String name, String key);

    private static native void syncAof(String name);

    private static native void syncRdb(String name);

    private static native void set(String name, String key, byte type, byte[] value, long expire);

    private static native NativeRecord ladd(String name, String key, byte type, byte[] value);

    private static native NativeRecord get(String name, String key);

    private static native void remove(String name, String key);

    private static native int forkNative(String cache);

}
