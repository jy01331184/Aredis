package aredis;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;

import aredis.cfg.AredisConfig;
import aredis.ext.ARedisException;
import aredis.process.AredisListener;
import aredis.process.MultiProcessHandle;

/**
 * Created by tianyang on 18/5/11.
 */
class ProcessARedisCache implements Aredis {

    private MultiProcessHandle handle;
    private String name;

    ProcessARedisCache(final Context context, final String name, final AredisListener listener) {
        this.name = name;

        context.getApplicationContext().bindService(new Intent(context, MultiProcessService.class), new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                handle = MultiProcessHandle.Stub.asInterface(service);
                listener.onReady();
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
            }
        }, Context.BIND_AUTO_CREATE);

    }

    ProcessARedisCache(final Context context, final String name, final AredisListener listener, AredisConfig config) {
        this.name = name;

        Intent intent = new Intent(context, MultiProcessService.class);
        intent.putExtra(MultiProcessService.KEY_CONFIG, config);

        context.getApplicationContext().bindService(intent, new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                handle = MultiProcessHandle.Stub.asInterface(service);
                listener.onReady();
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
            }
        }, Context.BIND_AUTO_CREATE);

    }

    public synchronized Object get(final String key) throws ARedisException {

        if (handle != null) {
            NativeRecord record = null;
            try {
                record = handle.get(name, key);
            } catch (RemoteException e) {
                ARedisException exception = new ARedisException("ProcessARedisCache get fail by RemoteException");
                e.initCause(e);
                throw exception;
            }
            if (record != null) {
                return AType.getValue(record.type, record.value, 0, record.value.length);
            }
        } else {
            throw new ARedisException("ProcessARedisCache not ready");
        }

        return null;
    }

    public synchronized void set(String key, Object object, long expire) throws ARedisException {
        if (handle != null) {
            byte type = AType.getType(object);
            byte[] data = AType.getTypeBytes(object);
            try {
                handle.set(name, key, type, data, expire);
            } catch (RemoteException e) {
                ARedisException exception = new ARedisException("ProcessARedisCache set fail by RemoteException");
                e.initCause(e);
                throw exception;
            }
        } else {
            throw new ARedisException("ProcessARedisCache not ready");
        }
    }

    @Override
    public void delete(String key) throws ARedisException {
        throw new ARedisException("no impl in ProcessARedisCache");
    }

    @Override
    public void ladd(String key, Object data) throws ARedisException {
        throw new ARedisException("no impl in ProcessARedisCache");
    }

    @Override
    public void sync() throws ARedisException {
        throw new ARedisException("no impl in ProcessARedisCache");
    }
}
