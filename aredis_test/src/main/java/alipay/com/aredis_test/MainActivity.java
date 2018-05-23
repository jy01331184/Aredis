package alipay.com.aredis_test;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

import aredis.Aredis;
import aredis.ext.ARedisException;
import aredis.persist.APojoManager;
import testbean.Team;

public class MainActivity extends AppCompatActivity {

    private Aredis cache;

    private List<Object> list;

    HandlerThread handlerThread = new HandlerThread("loop");
    private Handler handler;
    private ServiceConnection sc, sc1;
    private Messenger messenger;
    private Messenger messenger1;

    int base = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        try {
            APojoManager.regist(Team.class, 10);
        } catch (ARedisException e) {
            e.printStackTrace();
        }
//        cache = AredisFactory.create(this, "test", new AredisDefaultConfig() {
//            @Override
//            public AredisType getType() {
//                return AredisType.RDB;
//            }
//
//            @Override
//            public boolean persist() {
//                return true;
//            }
//
//            @Override
//            public int getRdbSaveCount() {
//                return 5;
//            }
//
//            @Override
//            public int getMaxIoWaitTime() {
//                return 1000;
//            }
//        });


        try {
//
//            final Runnable runnable = new Runnable() {
//                @Override
//                public void run() {
//                    System.out.println("========================");
//                    System.out.println(Log.getStackTraceString(new AndroidRuntimeException("ANR")));
//                    System.out.println("========================");
//                }
//            };
//
//            handlerThread.start();
//            handler = new Handler(handlerThread.getLooper());
//            Looper.getMainLooper().setMessageLogging(new Printer() {
//                @Override
//                public void println(String x) {
//                    if (x.startsWith(">>>>>")) {
//                        handler.removeCallbacks(runnable);
//                        handler.postDelayed(runnable, 5000);
//                    } else {
//                        handler.removeCallbacks(runnable);
//                    }
//                }
//            });

//            System.out.println("====="+TrustManagerFactory.getDefaultAlgorithm());
//            TrustManagerFactory fac = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
//            KeyStore keyStore = null;
//            fac.init(keyStore);
//            TrustManager[] mgrs = fac.getTrustManagers();
//            for (TrustManager mgr : mgrs) {
//                System.out.println(mgr.getClass()+":"+mgr);
//            }

        } catch (Exception e) {
            e.printStackTrace();
        }
//        KeyChain.createInstallIntent()
        findViewById(R.id.btn1).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    System.out.println(cache.get("k1"));

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });


        findViewById(R.id.btn2).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    bindService(new Intent(MainActivity.this, TestService.class), sc, Context.BIND_AUTO_CREATE);
                    try {
                        if (messenger != null)
                            messenger.send(Message.obtain());
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
//                try {
//                    cache.ladd("k1","黝黑蜗壳"+base++);
//                } catch (Exception e) {
//                    e.printStackTrace();
//                }
            }
        });

        findViewById(R.id.btn3).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    bindService(new Intent(MainActivity.this, TestService1.class), sc1, Context.BIND_AUTO_CREATE);
                    try {
                        if (messenger1 != null)
                            messenger1.send(Message.obtain());
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
//                try {
//                    cache.delete("k1");
//                } catch (Exception e) {
//                    e.printStackTrace();
//                }
            }
        });

        sc = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                System.out.println("sccc:" + service);
                messenger = new Messenger(service);
                try {
                    messenger.send(Message.obtain());
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {

            }
        };

        sc1 = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                System.out.println("sccc:" + service);
                messenger1 = new Messenger(service);
                try {
                    messenger1.send(Message.obtain());
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {

            }
        };
        test();
    }

    void test() {

        try {
            Class<?> activityManagerNativeCls = Class.forName("android.app.ActivityManagerNative");
            Class<?> iActivityManagerCls = Class.forName("android.app.IActivityManager");


            Method getDefaultMethod = activityManagerNativeCls.getDeclaredMethod("getDefault", new Class[]{});

            getDefaultMethod.setAccessible(true);

            Object activityManager = getDefaultMethod.invoke(null);

            Field mRemoteField = activityManager.getClass().getDeclaredField("mRemote");

            mRemoteField.setAccessible(true);

            System.out.println(activityManager + ":" + mRemoteField.get(activityManager));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            unbindService(sc);
            unbindService(sc1);
        } catch (Exception e) {
        }

    }
}
