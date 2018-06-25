package com.example.innometrics.services;

import android.annotation.TargetApi;
import android.app.ActivityManager;
import android.app.Service;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;

import com.example.innometrics.local_data.AppDatabase;
import com.example.innometrics.local_data.ForegroundApp;
import com.example.innometrics.local_data.ForegroundAppDao;
import com.example.innometrics.utils.ApplicationUtils;

import java.util.Date;
import java.util.List;


/**
 * Service to track foreground apps.
 * Basically, every mSearchInterval milliseconds attempts to get package name of a foreground app
 */
public class ForegroundAppService extends Service {
    //The thing is, I allowed api level 19 and higher. If api level was 21 and higher, the process would be different.
    public static final String TAG = "ForegroundAppService";
    public static final boolean DEBUG = ApplicationUtils.DEBUG;


    public final static int INTERVAL_IN_MILLIS = 1000;
    private ActivityManager mAm;
    private Handler mHandler;
    private HandlerThread mHandlerThread;
    private TrackAppsRunnable mTrackAppsRunnable;
    private UsageStatsManager mUsm;
    private ForegroundAppDao mDao;
    private ForegroundApp mCurrentApp;
    private PowerManager mPowerManager;
    private long mSearchInterval = 5 * 1000;
    private long mServiceStartingTime;


    @Override
    public void onCreate() {
        super.onCreate();
        mAm = (ActivityManager) getBaseContext().getSystemService(ACTIVITY_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mUsm = (UsageStatsManager) getSystemService(Context.USAGE_STATS_SERVICE);
        }
        mDao = AppDatabase.getInstance(getBaseContext()).foregroundAppDao();
        mHandlerThread = new HandlerThread("TrackForegroundApp");
        mPowerManager = (PowerManager)getSystemService(Context.POWER_SERVICE);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (DEBUG) Log.d(TAG, "OnStartCommand");
        mServiceStartingTime = System.currentTimeMillis();
        mHandlerThread.start();
        mHandler = new Handler(mHandlerThread.getLooper());
        mTrackAppsRunnable = new TrackAppsRunnable();
        //insert first app
        Thread thread = new Thread() {
            @Override
            public void run() {
                //in getForegroundApp we search for usage in mSearchInterval time
                //But maybe first time we start tracking the app is already out of this interval
                //So we increase interval until we finally get last used app (current app)
                long previousInterval = mSearchInterval;
                while ((mCurrentApp = getForegroundApp()).getStartingTime() == 0){
                    mSearchInterval = mSearchInterval*2;
                    if (DEBUG) Log.d(TAG, "starting time zero");
                }
                mSearchInterval = previousInterval;
                mCurrentApp.setStartingTime(System.currentTimeMillis());
            }
        };
        thread.start();
        mHandler.postDelayed(mTrackAppsRunnable, INTERVAL_IN_MILLIS);
        return super.onStartCommand(intent, flags, startId);
    }

    private class TrackAppsRunnable implements Runnable {
        @Override
        public void run() {
            //check if screen is on or phone is interactive, depending on version
            if ((Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH && mPowerManager.isInteractive())
                    || (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT_WATCH && mPowerManager.isScreenOn())) {
                ForegroundApp newApp = getForegroundApp();
                if (DEBUG) Log.d(TAG, "new app: " + newApp.getForegroundApplicationName() + " | " + newApp.getStartingTime());
                if (DEBUG) Log.d(TAG, "old app: " + mCurrentApp.getForegroundApplicationName() + " | " + mCurrentApp.getStartingTime());
                //getForegroundApp returns starting time of an activity, not application, so we save it only when packageName changes.
                if (!mCurrentApp.getForegroundApplicationName().equals(newApp.getForegroundApplicationName())) {
                    //app time didn't change in mSearchInterval time, i.e. the app is still running last activity.
                    if (newApp.getStartingTime() != 0){
                        mDao.insertAll(mCurrentApp);
                        mCurrentApp = newApp;
                    }
                }
            } else {
                ForegroundApp sleeping = new ForegroundApp();
                sleeping.setStartingTime(System.currentTimeMillis());
                sleeping.setForegroundApplicationName("sleeping");
                if (!mCurrentApp.getForegroundApplicationName().equals(sleeping.getForegroundApplicationName())) {
                    if (DEBUG) Log.d(TAG, "sleeping mode");
                    mDao.insertAll(mCurrentApp);
                    mCurrentApp = sleeping;
                }
            }
            mHandler.postDelayed(this, INTERVAL_IN_MILLIS);
        }
    }

    @Override
    public void onDestroy() {
        if (DEBUG) Log.d(TAG, "onDestroy");
        mHandler.removeCallbacks(mTrackAppsRunnable);
        mHandlerThread.quitSafely();
        super.onDestroy();
        Thread thread = new Thread() {
            @Override
            public void run() {
                //put last if it isn't there (it didn't get to our runnable)
                ForegroundApp last = mDao.getById(mCurrentApp.getStartingTime());
                if (DEBUG) Log.d(TAG, "mCurrentApp: " + mCurrentApp.getForegroundApplicationName() + " | " + mCurrentApp.getStartingTime());

                if (last == null){
                    if (DEBUG) Log.d(TAG, "insertLast");
                    mDao.insertAll(mCurrentApp);
                } else {
                    if (DEBUG) Log.d(TAG, "last " + last.getForegroundApplicationName() + " | " + last.getStartingTime());
                    //lastTimeUsed was before we started the service
                    //and current app could be already in database
                    //then we just put only "end_tracking", which is wrong
                    //To avoid that, we put this app again with time of starting service:
                    ForegroundApp startDummy = new ForegroundApp();
                    startDummy.setForegroundApplicationName(mCurrentApp.getForegroundApplicationName());
                    startDummy.setStartingTime(mServiceStartingTime);
                    mDao.insertAll(startDummy);
                }
                //to understand when user stopped tracking put dummy Foreground app with current time.
                ForegroundApp endDummy = new ForegroundApp();
                endDummy.setForegroundApplicationName("end_tracking");
                endDummy.setStartingTime(System.currentTimeMillis());
                mDao.insertAll(endDummy);
            }
        };
        thread.start();
    }

    public ForegroundApp getForegroundApp() {
        ForegroundApp currentApp;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            //get activity from UsageStatsManager
            return getLollipopForegroundApp();
        } else {
            //get data from ActivityManager
            long time = System.currentTimeMillis();
            currentApp = new ForegroundApp();
            String packageName = mAm.getRunningTasks(1).get(0).topActivity.getClassName();
            currentApp.setForegroundApplicationName(packageName);
            currentApp.setStartingTime(time);
            return currentApp;
        }
    }


    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private ForegroundApp getLollipopForegroundApp() {
        Date date = new Date();
        //queryUsageStats doesn't give sorted list, so we have to find recently used by ourselves
        List<UsageStats> queryUsageStats = mUsm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, date.getTime() - mSearchInterval, date.getTime());
        long recentTime = 0;
        String recentPkg = "";
        for (int i = 0; i < queryUsageStats.size(); i++) {
            UsageStats stats = queryUsageStats.get(i);
            if (stats.getLastTimeUsed() > recentTime) {
                recentTime = stats.getLastTimeUsed();
                recentPkg = stats.getPackageName();
            }
        }
        ForegroundApp currentApp = new ForegroundApp();
        currentApp.setStartingTime(recentTime);
        currentApp.setForegroundApplicationName(recentPkg);
        return currentApp;
    }

    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
