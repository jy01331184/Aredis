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

/**
 * Created by tianyang on 18/5/4.
 */
public class TestService extends Service {

    private Aredis aRedisCache;


    private Handler handler = new Handler(){
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

        System.out.println("set le:"+ Process.myPid()+"---"+Thread.currentThread());
        if (aRedisCache == null) {
            aRedisCache = AredisFactory.create(this,"test");
        }

        try {
            System.out.println(aRedisCache.get("k1"));
            aRedisCache.set("k1","慕斯尽力了",0);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
