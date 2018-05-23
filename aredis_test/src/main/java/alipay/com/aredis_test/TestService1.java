package alipay.com.aredis_test;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.Process;
import android.support.annotation.Nullable;

import aredis.Aredis;
import aredis.AredisFactory;
import aredis.cfg.AredisDefaultConfig;
import aredis.process.AredisListener;

/**
 * Created by tianyang on 18/5/4.
 */
public class TestService1 extends Service {

    private Aredis aRedisCache;

    private boolean ready;

    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            set();
        }
    };

    Messenger messenger = new Messenger(handler);

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return messenger.getBinder();
    }


    void set() {

        System.out.println("set le:" + Process.myPid() + "---" + Thread.currentThread());
        if (aRedisCache == null) {
            aRedisCache = AredisFactory.createProcessAredis(this, "test", new AredisListener() {
                @Override
                public void onReady() {
                    ready = true;
                }
            }, new AredisDefaultConfig());
        }

        try {
            if (ready) {
                System.out.println(aRedisCache.get("k1"));
                aRedisCache.set("k1", "慕斯尽力了", 0);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
