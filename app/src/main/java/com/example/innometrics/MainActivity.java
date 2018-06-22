package com.example.innometrics;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.Fragment;
import android.support.v7.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.example.innometrics.fragments.ActivitiesFragment;
import com.example.innometrics.fragments.TrackingFragment;
import com.example.innometrics.fragments.MetricsFragment;
import com.example.innometrics.fragments.ProfileFragment;
import com.example.innometrics.utils.ConnectionUtils;
import com.example.innometrics.server.Connection;
import com.example.innometrics.server.ResponseObject;
import com.example.innometrics.server.ServerRequestItem;
import com.example.innometrics.utils.ApplicationUtils;
import com.ittianyu.bottomnavigationviewex.BottomNavigationViewEx;


/**
 * MainActivity is the first activity which is invoked when Innometrics is opening.
 * It has bottom navigation and 4 fragments: ActivitiesFragment, MetricsFragment, TrackingFragment and ProfileFragment.
 * Also, it has action bar, which is shared across all fragments.
 * The activity associates bottom navigation, sets default user preferences,
 * downloads data (activities and metrics (without details) from server (if needed) and inflates fragments.
 * In case there is no network, the application continues working with old data or shows that there is no network.
 * Action bar has syncing button, which updates all data if it can.
 * Please, take into account Android source code documentation:
 Non-public, non-static field names start with m.
 Static field names start with s.
 Other fields start with a lower case letter.
 Public static final fields (constants) are ALL_CAPS_WITH_UNDERSCORES.
 */

public class MainActivity extends BasicActivity {
    public static final String TAG = "MainActivity";
    private ProgressDialog mProgressDialog = null;
    private BottomNavigationViewEx mBottomNav;
    private static final boolean DEBUG = ApplicationUtils.DEBUG;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //https://stackoverflow.com/questions/19545889/app-restarts-rather-than-resumes
        if (!isTaskRoot()
                && getIntent().hasCategory(Intent.CATEGORY_LAUNCHER)
                && getIntent().getAction() != null
                && getIntent().getAction().equals(Intent.ACTION_MAIN)) {

            finish();
            return;
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //when data is downloading the progress dialog is shown
        setProgressDialog();
        //it is required to set default values from xml to default preferences. If user changed preferences - they will stay
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
        mBottomNav = findViewById(R.id.main_bottom_navigation);
        mBottomNav.setOnNavigationItemSelectedListener(navListener);
        mBottomNav.enableShiftingMode(false);
        mBottomNav.enableAnimation(false);
        mBottomNav.enableItemShiftingMode(false);
        if (savedInstanceState == null) {
            setApplicationData(false);
        }
    }

    private void setProgressDialog(){
        mProgressDialog = new ProgressDialog(this, R.style.AppBaseDialog);
        mProgressDialog.setIndeterminate(true);
        mProgressDialog.setCancelable(false);
        mProgressDialog.setMessage("Preparing user data...");
    }

    @Override
    protected void onDestroy() {
        //to prevent errors when the app is destroying
        dismissProgressDialog();
        super.onDestroy();
    }

    private void setApplicationData(boolean syncing) {
        if (DEBUG){
            if (syncing){
                Log.d(TAG, "syncing in setApplicationData");
            } else {
                Log.d(TAG, "setApplicationData");
            }
        }
        //get prefs for activities and metrics
        //check if they are present
        //if yes, do nothing
        //if no, try to download data from server and put into the prefs (in async task)
        SharedPreferences activitiesPrefs = getSharedPreferences(ConnectionUtils.PREFS_ACTIVITIES, MODE_PRIVATE);
        SharedPreferences metricsPrefs = getSharedPreferences(ConnectionUtils.PREFS_METRICS, MODE_PRIVATE);
        if (syncing
                || (!activitiesPrefs.contains(ConnectionUtils.PREFS_ACTIVITIES_ACTIVITIES)
                || !metricsPrefs.contains(ConnectionUtils.PREFS_METRICS_METRICS))){
            if (networkAvailable(true)) {
                if (!loginRequired()){
                    if (DEBUG) Log.d(TAG, "setApplicationDataFromServer");
                    new DownloadDataAndPutIntoMetricsTask(mProgressDialog).execute();
                }
            } else {
                if (!syncing) {
                    Intent intent = new Intent(this, NoNetworkActivity.class);
                    startActivity(intent);
                }
            }
        } else {
            if (DEBUG) Log.d(TAG, "Data is here already");
            selectPreviousBottomView();
        }
    }

    private void sync() {
        if (DEBUG) Log.d(TAG, "SYNCING..");
        setApplicationData(true);
    }

    private void dismissProgressDialog() {
        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            mProgressDialog.dismiss();
        }
    }

    public class DownloadDataAndPutIntoMetricsTask extends AsyncTask<ServerRequestItem, Void, Void> {
        private static final String TAG = "Download&PutIntoPrefs";
        private ProgressDialog pd;
        public DownloadDataAndPutIntoMetricsTask(ProgressDialog pd) {
            this.pd = pd;
        }

        @Override
        protected void onPreExecute() {
            if (DEBUG) Log.d(TAG, "onPreExecute");
            if (pd != null){
                pd.show();
            }
            super.onPreExecute();
        }

        @Override
        protected Void doInBackground(ServerRequestItem... params) {
            //download information about activities (with details) and metrics (without details)
            //put them into corresponding preferences.
            SharedPreferences activitiesPrefs = getSharedPreferences(ConnectionUtils.PREFS_ACTIVITIES, MODE_PRIVATE);
            SharedPreferences metricsPrefs = getSharedPreferences(ConnectionUtils.PREFS_METRICS, MODE_PRIVATE);
            SharedPreferences preferences = getSharedPreferences(ConnectionUtils.PREFS_USER, MODE_PRIVATE);
            String token = preferences.getString(ConnectionUtils.PREFS_USER_TOKEN,"");

            ServerRequestItem getActivities = new ServerRequestItem(ConnectionUtils.URL_ACTIVITIES, token, null);
            ResponseObject activitiesResponse = Connection.requestToServer(getActivities);
            SharedPreferences.Editor editActivities = activitiesPrefs.edit();
            String activitiesStr = activitiesResponse.getResponse().toString();
            if (DEBUG) Log.d(TAG, "activitiesStr: " + activitiesStr);
            editActivities.putString(ConnectionUtils.PREFS_ACTIVITIES_ACTIVITIES, activitiesStr);
            editActivities.apply();

            ServerRequestItem getMetrics = new ServerRequestItem(ConnectionUtils.URL_METRICS, token, null);
            ResponseObject metricsResponse = Connection.requestToServer(getMetrics);
            SharedPreferences.Editor editMetrics = metricsPrefs.edit();
            String metricsStr = metricsResponse.getResponse().toString();
            if (DEBUG) Log.d(TAG, "metricsStr: " + metricsStr);
            editMetrics.putString(ConnectionUtils.PREFS_METRICS_METRICS, metricsStr);
            editMetrics.apply();
            return null;
        }

        @Override
        protected void onPostExecute(Void response) {
            //when we finish downloading we have to dismiss progress dialog
            //if we not downloading page
            if (DEBUG) Log.d(TAG, "onPostExecute");
            dismissProgressDialog();
            selectPreviousBottomView();
            super.onPostExecute(response);
        }

        private void dismissProgressDialog() {
            if (isDestroyed()) {
                return;
            }
            MainActivity.this.dismissProgressDialog();
        }
    }

    private void selectPreviousBottomView(){
        MenuItem selected = mBottomNav.getMenu().findItem(mBottomNav.getSelectedItemId());
        navListener.onNavigationItemSelected(selected);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_action_bar_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        //upper action menu
        switch (item.getItemId()){
            case R.id.action_sync:
                sync();
                return true;
            default: return super.onOptionsItemSelected(item);
        }
    }

    private BottomNavigationView.OnNavigationItemSelectedListener navListener =
            new BottomNavigationView.OnNavigationItemSelectedListener() {
                @Override
                public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                    Fragment selectedFragment = null;
                    switch (item.getItemId()) {
                        case R.id.nav_activities:
                            selectedFragment = new ActivitiesFragment();
                            break;
                        case R.id.nav_general:
                            selectedFragment = new TrackingFragment();
                            break;
                        case R.id.nav_profile:
                            selectedFragment = new ProfileFragment();
                            break;
                        case R.id.nav_metrics:
                            selectedFragment = new MetricsFragment();
                            break;
                    }
                    //To avoid "Can not perform this action after onSaveInstanceState" exception
                    //I used commitAllowingStateLoss() instead of commit()
                    getSupportFragmentManager().beginTransaction().replace(R.id.main_fragment_container,
                            selectedFragment).commitAllowingStateLoss();
                    return true;
                    }
            };
}