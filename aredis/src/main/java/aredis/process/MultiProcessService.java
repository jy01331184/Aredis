package aredis.process;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteException;

import aredis.ARedisCache;
import aredis.NativeRecord;

/**
 * Created by tianyang on 18/5/11.
 */
public class MultiProcessService extends Service {


    public MultiProcessService() {

    }

    MultiProcessHandle.Stub stub = new MultiProcessHandle.Stub() {
        @Override
        public NativeRecord get(String node, String key) throws RemoteException {

            ARedisCache aredis = ARedisCache.create(MultiProcessService.this, node);

            try {
                NativeRecord record = aredis.getRaw(key);
                return record;
            } catch (Exception e) {
                e.printStackTrace();
                RemoteException ex = new RemoteException("remote get fail");
                ex.initCause(e);
                throw ex;
            }
        }

        @Override
        public void set(String node, String key, byte type, byte[] data, long expire) throws RemoteException {
            ARedisCache aredis = ARedisCache.create(MultiProcessService.this, node);

            try {
                aredis.setRaw(key, type, data, expire);
            } catch (Exception e) {
                e.printStackTrace();
                RemoteException ex = new RemoteException("remote set fail");
                ex.initCause(e);
                throw ex;
            }
        }
    };

    @Override
    public IBinder onBind(Intent intent) {
        return stub;
    }


}
