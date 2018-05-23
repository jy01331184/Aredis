package aredis;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.Process;
import android.os.RemoteException;

import aredis.cfg.AredisConfig;
import aredis.process.MultiProcessHandle;
import aredis.process.ProcessSupport;

/**
 * Created by tianyang on 18/5/11.
 */
public class MultiProcessService extends Service {

    public static final String KEY_CONFIG = ":config";

    private AredisConfig config;

    MultiProcessHandle.Stub stub = new MultiProcessHandle.Stub() {
        @Override
        public NativeRecord get(String node, String key) throws RemoteException {

            ProcessSupport aredis = AredisFactory.getProcessSupport(MultiProcessService.this, node, config);

            if (aredis == null) {
                throw new IllegalStateException("no aredis instance in Process:" + Process.myPid() + " with name:" + node);
            }

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
            ProcessSupport aredis = AredisFactory.getProcessSupport(MultiProcessService.this, node, config);

            if (aredis == null) {
                throw new IllegalStateException("no aredis instance in Process:" + Process.myPid() + " with name:" + node);
            }

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

        config = (AredisConfig) intent.getSerializableExtra(KEY_CONFIG);

        return stub;
    }


}
