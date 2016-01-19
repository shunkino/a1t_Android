package jp.ad.wide.sfc.shunkin;

import java.io.IOException;
import java.util.Collection;

import android.os.Bundle;
import android.os.RemoteException;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconConsumer;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.Identifier;
import org.altbeacon.beacon.MonitorNotifier;
import org.altbeacon.beacon.RangeNotifier;
import org.altbeacon.beacon.Region;
import org.altbeacon.beacon.powersave.BackgroundPowerSaver;
import org.altbeacon.beacon.startup.BootstrapNotifier;
import org.altbeacon.beacon.startup.RegionBootstrap;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.FormBody;

public class MainActivity extends AppCompatActivity implements BeaconConsumer  {
    private final OkHttpClient client = new OkHttpClient();
    public void post_http(String uuid) throws Exception {
        RequestBody formBody = new FormBody.Builder()
//            変数におきかえる
                .add("personName", "しゅんきの")
                .add("password", "shunkinkin")
                .add("placeUUID", uuid)
                .add("twitter", "@shunki9")
                .build();
        Request request = new Request.Builder()
                .url("http://203.178.139.222:3000/people/refresh")
                .post(formBody)
                .build();

        Response response = client.newCall(request).execute();
        if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);

        System.out.println(response.body().string());
    }


    private BeaconManager mBeaconManager;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    post_http("0");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
        Log.i("Ble" ,"Hello, world");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mBeaconManager = BeaconManager.getInstanceForApplication(this);
        String IBEACON_FORMAT = "m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24";
        mBeaconManager.getBeaconParsers().add(new BeaconParser().setBeaconLayout(IBEACON_FORMAT));
        mBeaconManager.bind(this);


        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
    @Override
    public void onBeaconServiceConnect() {
        Identifier uuid = Identifier.parse("9BEBC6C2-7487-4F17-91E6-179BD2AC48A1");
        Identifier major = Identifier.parse("8");
        Identifier minor = Identifier.parse("1");
        final Region mregion = new Region("jp.ad.wide.sfc.shunkin", uuid, major, minor);

        mBeaconManager.setMonitorNotifier(new MonitorNotifier() {
            @Override
            public void didEnterRegion(Region region) {
                // 領域への入場を検知
                Log.i("Ble", "enterd region");
                try {
                    mBeaconManager.startRangingBeaconsInRegion(mregion);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void didExitRegion(Region region) {
                Log.i("Ble", "Exit region");
                try {
                    mBeaconManager.stopRangingBeaconsInRegion(mregion);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
                try {
                    post_http("0");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void didDetermineStateForRegion(int i, Region region) {
                Log.i("Ble", "Change Detected");
            }
        });
        mBeaconManager.setRangeNotifier(new RangeNotifier() {
            @Override
            public void didRangeBeaconsInRegion(Collection<Beacon> beacons, Region region) {
                // 検出したビーコンの情報を全部Logに書き出す
                for (Beacon beacon : beacons) {
                    Log.i("debug", "UUID:" + beacon.getId1() + ", major:" + beacon.getId2() + ", minor:" + beacon.getId3() + ", Distance:" + beacon.getDistance() + ",RSSI" + beacon.getRssi() + ", TxPower" + beacon.getTxPower());
                    String uuid = String.valueOf(beacon.getId1());
                    Log.i("debug", uuid);
                    if (beacons.size() > 0) {
                        try {
                            post_http(uuid);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    } else {
                        try {
                            post_http("0");
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
                Log.i("debug", String.valueOf(beacons.size()));
            }
            });
            try {
                // モニタリングの開始
                mBeaconManager.startMonitoringBeaconsInRegion(mregion);
            } catch(RemoteException e) {
                e.printStackTrace();
            }

        try {
//            mBeaconManager.startMonitoringBeaconsInRegion(new Region("jp.ad.wide.sfc.shunkin", uuid, major, minor));
            mBeaconManager.startMonitoringBeaconsInRegion(mregion);

        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }
//    @Override
//    protected void onResume() {
//        super.onResume();
//    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        mBeaconManager.unbind(this);
    }
}
