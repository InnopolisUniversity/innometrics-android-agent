package com.example.innometrics.fragments;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;

import com.example.innometrics.R;
import com.example.innometrics.local_data.AppDatabase;
import com.example.innometrics.local_data.ForegroundApp;
import com.example.innometrics.local_data.ForegroundAppDao;
import com.example.innometrics.services.ForegroundAppService;
import com.example.innometrics.utils.ConnectionUtils;
import com.example.innometrics.server.ResponseObject;
import com.example.innometrics.server.ServerRequestItem;
import com.example.innometrics.utils.ApplicationUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.example.innometrics.server.Connection.requestToServer;
import static com.example.innometrics.utils.ApplicationUtils.sFormat;
/**
 * One of the fragments of CollectedMetricActivity
 * @see com.example.innometrics.CollectedMetricsActivity
 * It shows collected foreground apps: pachage name and last time used (starting time)
 * Has action bar to clear collected data or upload and clear
 * @see LocationsFragment - similar functionality.
 */

//I failed to make some basic class for both fragments due to unexpected errors.
public class ForegroundAppsFragment extends Fragment {
    public static final String TAG = "ForegroundAppsFragment";
    public static final boolean DEBUG = ApplicationUtils.DEBUG;
    public static final boolean ERROR = ApplicationUtils.ERROR;

    private SimpleAdapter mAdapter;
    private ListView appsListView;
    private ForegroundAppDao mDao;
    private ProgressDialog mProgressDialog;
    private List<ForegroundApp> mApps;

    private static final int sDownloadFromLocal = 0;
    private static final int sUploadToServer = 1;
    private static final  int sClearLocalData = 2;

    private int action;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_foreground_apps, container, false);
        appsListView = view.findViewById(R.id.foreground_apps_list_view);
        mDao = AppDatabase.getInstance(getContext()).foregroundAppDao();
        setHasOptionsMenu(true);
        setProgressDialog();
        setData();
        return view;
    }

    private void setProgressDialog(){
        mProgressDialog = new ProgressDialog(getContext(), R.style.AppBaseDialog);
        mProgressDialog.setIndeterminate(true);
        mProgressDialog.setCancelable(false);
        mProgressDialog.setMessage(getResources().getString(R.string.foreground_apps_progress_dialog));
    }

    private void setData() {
        action = sDownloadFromLocal;
        new LargeTask(mProgressDialog).execute();
    }

    /**
     * Action bar has 2 buttons
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (DEBUG) Log.d(TAG, "onOptionsItemSelected");
        //upper action menu
        if (!ApplicationUtils.isMyServiceRunning(ForegroundAppService.class, getActivity())){
            switch (item.getItemId()){
                case R.id.action_upload_local_data:
                    if (ConnectionUtils.isNetworkConnected(getContext())) {
                        if (DEBUG) Log.d(TAG, "action upload");
                        uploadData();
                    } else {
                        Toast.makeText(getContext(), getResources().getString(R.string.no_network_toast_message), Toast.LENGTH_SHORT).show();
                    }
                    return true;
                case R.id.action_clear_local_data: {
                    if (DEBUG) Log.d(TAG, "action clear");
                    clearData();
                    return true;
                }
            }
        } else {
            Toast.makeText(getContext(), getResources().getString(R.string.cant_upload_apps_toast_message), Toast.LENGTH_LONG).show();
        }
        return super.onOptionsItemSelected(item);
    }

    private void uploadData() {
        action = sUploadToServer;
        new LargeTask(mProgressDialog).execute();
    }

    private void clearData() {
        action = sClearLocalData;
        new LargeTask(mProgressDialog).execute();
    }

    /**
     * Working with database and uploading data needs to be done in separate thread
     * This AsyncTask handles 3 types of actions: download from local, upload to server and clear.
     * Also, shows ProgressDialog
     */
    public class LargeTask extends AsyncTask<ServerRequestItem, Void, Void> {
        private static final String TAG = "LargeTask";
        private ProgressDialog pd;
        public LargeTask(ProgressDialog pd) {
            this.pd = pd;
        }
        private List<ForegroundApp> items;

        @Override
        protected void onPreExecute() {
            if (DEBUG) Log.d(TAG, "onPreExecute");
            if (pd != null){
                if (DEBUG) Log.d(TAG, "Progress dialog is not null, show");
                pd.show();
            }
            super.onPreExecute();
        }

        @Override
        protected Void doInBackground(ServerRequestItem... params) {
            switch (action){
                case sDownloadFromLocal:
                    items = mDao.getAll();
                    if (DEBUG) Log.d(TAG, "mLocations size: " + items.size());
                    break;
                case sUploadToServer:
                    try {
                        JSONObject requestBody = getRequestBody();
                        SharedPreferences prefs = getContext().getSharedPreferences(ConnectionUtils.PREFS_USER, Context.MODE_PRIVATE);
                        ServerRequestItem requestItem = new ServerRequestItem(ConnectionUtils.URL_SEND_ACTIVITIES, prefs.getString(ConnectionUtils.PREFS_USER_TOKEN, ""), requestBody.toString());
                        ResponseObject responseObject = requestToServer(requestItem);
                        if (responseObject.getResponseCode() != HttpURLConnection.HTTP_CREATED){
                            if (DEBUG) Log.d(TAG, "activities didn't make it");
                        } else {
                            if (DEBUG) Log.d(TAG, "They made it! Activities!");
                            mDao.clear();
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    break;
                case sClearLocalData:
                    mDao.clear();
                    break;
            }
            return null;
        }

        protected void onPostExecute(Void response) {
            if (DEBUG) Log.d(TAG, "onPostExecute");
            dismissProgressDialog();
            switch (action){
                case sDownloadFromLocal:
                    ForegroundAppsFragment.this.mApps = items;
                    break;
                case sUploadToServer:
                    mApps.clear();
                    break;
                case sClearLocalData:
                    mApps.clear();
                    break;
            }
            //after task is completed, update view
            try {
                setListView();
            } catch (Exception e){
                //TODO: if we change fragment too fast we get this error:
                if (ERROR) Log.e(TAG, "Attempt to invoke virtual method 'java.lang.Object android.content.Context.getSystemService(java.lang.String)' on a null object reference");
                //it is a problem with AsyncTask and BottomNavigation, which I didn't resolve
            }
            super.onPostExecute(response);
        }

        private void dismissProgressDialog() {
            if (mProgressDialog != null && mProgressDialog.isShowing()) {
                mProgressDialog.dismiss();
            }
        }
    }

    /**
     * @return JSONObject of collected activities
     * It is inefficient way of uploading data.
     * To change it - change the server first
     */
    public JSONObject getRequestBody() throws JSONException {
        if (DEBUG) Log.d(TAG, "getRequestBody");
        JSONObject response = new JSONObject();
        JSONArray activities = new JSONArray();
        for (int i = 0; i < mApps.size(); i++) {
            ForegroundApp item = mApps.get(i);
            JSONObject activity = new JSONObject();
            activity.accumulate(ConnectionUtils.SEND_ACTIVITY_NAME, "Android Agent. Foreground Applications");
            activity.accumulate(ConnectionUtils.SEND_ACTIVITY_COMMENT, "");
            JSONArray measurements = new JSONArray();
            JSONObject appName = new JSONObject();
            appName.accumulate(ConnectionUtils.SEND_ACTIVITY_MEASUREMENT_TYPE, "double");
            appName.accumulate(ConnectionUtils.SEND_ACTIVITY_MEASUREMENT_NAME, "package_name");
            appName.accumulate(ConnectionUtils.SEND_ACTIVITY_MEASUREMENT_VALUE, item.getForegroundApplicationName());
            JSONObject startingTime = new JSONObject();
            startingTime.accumulate(ConnectionUtils.SEND_ACTIVITY_MEASUREMENT_TYPE, "long");
            startingTime.accumulate(ConnectionUtils.SEND_ACTIVITY_MEASUREMENT_NAME, "starting_time");
            startingTime.accumulate(ConnectionUtils.SEND_ACTIVITY_MEASUREMENT_VALUE, item.getStartingTime());
            measurements.put(appName);
            measurements.put(startingTime);
            activity.accumulate(ConnectionUtils.SEND_ACTIVITY_MEASUREMENTS, measurements);
            activities.put(activity);
        }
        response.accumulate(ConnectionUtils.SEND_ACTIVITIES, activities);
        return response;
    }

    private void setListView() {
//        mAdapter.notifyDataSetChanged(); - didn't work for me, had to update whole list
        List<Map<String, String>> list = new ArrayList<>();
        Map<String, String> map;
        Date date;
        int count = mApps.size();
        for(int i = 0; i < count; i++) {
            map = new HashMap<>();
            date = new Date(mApps.get(i).getStartingTime());
            map.put("value", mApps.get(i).getForegroundApplicationName());
            map.put("time", sFormat.format(date));
            list.add(map);
        }
        //Simple adapter is used instead of ArrayAdapter so it can take 2 elements to display
        //the same job can be done using RecyclerView, but this is simpler and has nice separating line between rows
        mAdapter = new SimpleAdapter(getContext(), list, R.layout.collected_data_item, new String[] { "value", "time" }, new int[] {R.id.collected_metric_value, R.id.collected_metric_time});
        appsListView.setAdapter(mAdapter);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mApps != null){
            setListView();
        }
    }
}
