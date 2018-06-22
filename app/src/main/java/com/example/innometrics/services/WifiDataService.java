package com.example.innometrics.services;

import android.app.Service;
import android.content.Intent;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.util.Log;

import com.example.innometrics.local_data.AppDatabase;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Used to track BSSIDs of access points.
 * Deprecated and not working now.
 */
@Deprecated
public class WifiDataService extends Service {

    private WifiManager mWifiManager;

    public WifiDataService() {
    }

    public static final String TAG = "WifiDataService";
    public final static int INTERVAL_IN_MILLIS = 30000;

    private Handler mHandler;
    private HandlerThread mHandlerThread;
    private TrackWifiRunnable mTrackWifiRunnable;

//    private WifiDataDao mDao;

    @Override
    public void onCreate() {
        super.onCreate();
//        mDao = AppDatabase.getInstance(getBaseContext()).wifiDataDao();
        mHandlerThread = new HandlerThread("TrackWifiBSSIDs");
        mWifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "OnStartCommand");
        mHandlerThread.start();
        mHandler = new Handler(mHandlerThread.getLooper());
        mTrackWifiRunnable = new TrackWifiRunnable();
        mHandler.postDelayed(mTrackWifiRunnable, INTERVAL_IN_MILLIS);
        return super.onStartCommand(intent, flags, startId);
    }

    private class TrackWifiRunnable implements Runnable{

        @Override
        public void run() {
            //get wifi data
            //put it to database
            //TODO: check if wifi connected?
            List<ScanResult> wifiList = mWifiManager.getScanResults();
            if (wifiList.size() > 0) {
                Log.d(TAG, "number of access points BSSIDs: " + wifiList.size());
            }
            List<String> bssids = new ArrayList<>(wifiList.size());
            for (int i = 0; i < wifiList.size(); i++) {
                bssids.add(wifiList.get(i).BSSID);
            }
            convertToMacAddressLike(bssids);
            Map<String, List<Integer>> bssidsAndRssis = new HashMap<>();
            for (int i = 0; i < bssids.size(); i++) {
                if (bssidsAndRssis.containsKey(bssids.get(i))){
                    List<Integer> rssis = bssidsAndRssis.get(bssids.get(i));
                    rssis.add(wifiList.get(i).level);
                } else {
                    List<Integer> rssis = new ArrayList<>();
                    rssis.add(wifiList.get(i).level);
                    bssidsAndRssis.put(bssids.get(i), rssis);
                }
            }
            Log.d(TAG, "bssids and rssis: ");
            for (Map.Entry<String, List<Integer>> entry : bssidsAndRssis.entrySet())
            {
                Log.d(TAG, "key: " + entry.getKey());
                Log.d(TAG, "values: ");
                for (int i = 0; i < entry.getValue().size(); i++) {
                    Log.d(TAG, Integer.toString(entry.getValue().get(i)));
                }
            }
            mHandler.postDelayed(this, INTERVAL_IN_MILLIS);
        }
    }

    private void convertToMacAddressLike(List<String> bssids){
        for (int i = 0; i < bssids.size(); i++) {
            bssids.set(i, bssids.get(i).substring(0, 11));
        }
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy");
        mHandlerThread.quitSafely();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
