package alipay.com.aredis_test;

import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.support.v7.app.AppCompatActivity;
import android.util.AndroidRuntimeException;
import android.util.Log;
import android.util.Printer;
import android.view.View;

import java.util.List;

import aredis.ARedisCache;
import aredis.cfg.AredisDefaultConfig;
import aredis.persist.APojoManager;
import testbean.Team;

public class MainActivity extends AppCompatActivity {

    private ARedisCache cache;

    private List<Object> list;

    HandlerThread handlerThread = new HandlerThread("loop");
    private Handler handler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        APojoManager.regist(Team.class, 10);
        cache = ARedisCache.create(this, "test", new AredisDefaultConfig() {
            @Override
            public AredisType getType() {
                return AredisType.RDB;
            }

            @Override
            public boolean persist() {
                return true;
            }

            @Override
            public int getRdbSaveCount() {
                return 1;
            }
        });


        try {

            final Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    System.out.println("========================");
                    System.out.println(Log.getStackTraceString(new AndroidRuntimeException("ANR")));
                    System.out.println("========================");
                }
            };

            handlerThread.start();
            handler = new Handler(handlerThread.getLooper());
            Looper.getMainLooper().setMessageLogging(new Printer() {
                @Override
                public void println(String x) {
                    if (x.startsWith(">>>>>")) {
                        handler.removeCallbacks(runnable);
                        handler.postDelayed(runnable, 5000);
                    } else {
                        handler.removeCallbacks(runnable);
                    }
                }
            });

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

                    cache.set("k1","v1",0);
                    cache.set("k2","v2",0);
                    cache.set("k3","v3",0);

                    if (list == null) {
//                        list = new ArrayList<Object>();
//                        cache.ladd("keyold", "a");
                    } else {
//                        Set list = new HashSet();
//                        Team team = new Team();
//                        team.name = "LA";
//
//                        team.slary = 110.4f;
//                        team.playoff = true;
//
//                        team.players = new ArrayList<>();
//                        team.players.add(new Player("kuzma44", 22));
//                        team.players.add(new Player("randle", 22));
//                        team.players.add(new Player("ball", 19));
//                        team.players.add(new Player("ingram", 20));
//                        list.add(17.8);
//                        list.add(team);
//                        list.add(false);
//                        String longKey = UUID.randomUUID().toString();
//                        list.add(longKey);
//                        Player player = new Player();
//                        player.name = "黝黑蜗壳";
//                        list.add(player);
//                        cache.ladd("keynew", list);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });


        findViewById(R.id.btn2).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    System.out.println(cache.get("k1"));
                    System.out.println(cache.get("k2"));
                    System.out.println(cache.get("k3"));
                    //System.out.println(cache.get("keynew"));
//                    System.out.println("sleep");
//                    Thread.sleep(5500);
//                    System.out.println("wake");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }
}
