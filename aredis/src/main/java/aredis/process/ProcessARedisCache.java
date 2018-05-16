package aredis.process;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;

import java.util.HashMap;
import java.util.Map;

import aredis.AType;
import aredis.NativeRecord;

/**
 * Created by tianyang on 18/5/11.
 */
public class ProcessARedisCache {


    private static Map<String, ProcessARedisCache> caches = new HashMap<>();

    private MultiProcessHandle handle;
    private String name;


    public static synchronized ProcessARedisCache create(Context context, String name) {
        ProcessARedisCache redis = caches.get(name);
        if (redis == null) {
            redis = new ProcessARedisCache(context, name);
            caches.put(name, redis);
        }

        return redis;
    }

    private ProcessARedisCache(final Context context, final String name) {
        this.name = name;

        context.getApplicationContext().bindService(new Intent(context, MultiProcessService.class), new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                System.out.println("try to bind remote:" + Thread.currentThread());
                handle = MultiProcessHandle.Stub.asInterface(service);
                System.out.println("success to bind remote");
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
            }
        }, Context.BIND_AUTO_CREATE);

    }

    public synchronized Object get(final String key) throws Exception {

        if (handle != null) {
            NativeRecord record = handle.get(name, key);
            if (record != null) {
                return AType.getValue(record.type, record.value, 0, record.value.length);
            }
        } else {
            System.err.println("handle not init");
        }

        return null;

    }

    public synchronized void set(String key, Object object, long expire) throws Exception {
        if (handle != null) {
            byte type = AType.getType(object);
            byte[] data = AType.getTypeBytes(object);
            handle.set(name, key, type, data, expire);

        } else {
            System.err.println("handle not init");
        }

    }
}
