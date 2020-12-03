package com.example.innometrics.fragments;

import android.app.ProgressDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;

import com.example.innometrics.R;
import com.example.innometrics.local_data.AppDatabase;
import com.example.innometrics.local_data.LocationItem;
import com.example.innometrics.local_data.LocationItemDao;
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
 * It shows collected locations and time when they have been stored.
 * Has action bar to clear collected data or upload and clear
 * @see ForegroundAppsFragment - similar funcitonality (with more explanation)
 * This fragment also handles actions on a ListView: long click on a location - then you can copy coordinates.
 * This is done by using conext menu
 * @see LocationsFragment#onCreateView
 * @see LocationsFragment#onCreateContextMenu
 * @see LocationsFragment#onContextItemSelected
 */
public class LocationsFragment extends Fragment {
    public static final String TAG = "LocationsFragment";
    public static final boolean DEBUG = ApplicationUtils.DEBUG;
    public static final boolean ERROR = ApplicationUtils.ERROR;

    private SimpleAdapter mAdapter;
    private ListView locationsListView;
    private ProgressDialog mProgressDialog;
    private List<LocationItem> mLocations;
    private LocationItemDao mDao;

    private static final int sDownloadFromLocal = 0;
    private static final int sUploadToServer = 1;
    private static final  int sClearLocalData = 2;
    private int action;


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_locations, container, false);
        locationsListView = view.findViewById(R.id.locations_list_view);
        mDao = AppDatabase.getInstance(getContext()).locationDao();
        setHasOptionsMenu(true);
        //one has to register context menu on a ListView
        registerForContextMenu(locationsListView);
        setProgressDialog();
        if (savedInstanceState == null){
            setData();
        }
        return view;
    }

    private void setProgressDialog(){
        mProgressDialog = new ProgressDialog(getContext(), R.style.AppBaseDialog);
        mProgressDialog.setIndeterminate(true);
        mProgressDialog.setCancelable(false);
        mProgressDialog.setMessage(getResources().getString(R.string.locations_progress_dialog_str));
    }

    private void setData() {
        action = sDownloadFromLocal;
        new LargeTask(mProgressDialog).execute();
    }

    private void uploadData() {
        action = sUploadToServer;
        new LargeTask(mProgressDialog).execute();
    }

    public JSONObject getRequestBody() throws JSONException {
        if (DEBUG) Log.d(TAG, "getRequestBody");
        JSONObject response = new JSONObject();
        JSONArray activities = new JSONArray();
        for (int i = 0; i < mLocations.size(); i++) {
//            LocationItem item = mLocations.get(i);
//            JSONObject activity = new JSONObject();
//            activity.accumulate(ConnectionUtils.SEND_ACTIVITY_NAME, "Android Agent. Locations.");
//            activity.accumulate(ConnectionUtils.SEND_ACTIVITY_COMMENT, "");
//            JSONArray measurements = new JSONArray();
//            JSONObject latitude = new JSONObject();
//            latitude.accumulate(ConnectionUtils.SEND_ACTIVITY_MEASUREMENT_TYPE, "double");
//            latitude.accumulate(ConnectionUtils.SEND_ACTIVITY_MEASUREMENT_NAME, "latitude");
//            latitude.accumulate(ConnectionUtils.SEND_ACTIVITY_MEASUREMENT_VALUE, item.getLatitude());
//            JSONObject longitude = new JSONObject();
//            longitude.accumulate(ConnectionUtils.SEND_ACTIVITY_MEASUREMENT_TYPE, "double");
//            longitude.accumulate(ConnectionUtils.SEND_ACTIVITY_MEASUREMENT_NAME, "longitude");
//            longitude.accumulate(ConnectionUtils.SEND_ACTIVITY_MEASUREMENT_VALUE, item.getLatitude());
//            JSONObject time = new JSONObject();
//            time.accumulate(ConnectionUtils.SEND_ACTIVITY_MEASUREMENT_TYPE, "long");
//            time.accumulate(ConnectionUtils.SEND_ACTIVITY_MEASUREMENT_NAME, "time");
//            time.accumulate(ConnectionUtils.SEND_ACTIVITY_MEASUREMENT_VALUE, item.getLatitude());
//            measurements.put(latitude);
//            measurements.put(longitude);
//            measurements.put(time);
//            activity.accumulate(ConnectionUtils.SEND_ACTIVITY_MEASUREMENTS, measurements);
//
//            activities.put(activity);
        }
        //response.accumulate(ConnectionUtils.SEND_ACTIVITIES, activities);
        response.accumulate(ConnectionUtils.SEND_ACTIVITIES, activities);
        return response;
    }

    public class LargeTask extends AsyncTask<ServerRequestItem, Void, Void> {
        private static final String TAG = "LargeTask";
        private ProgressDialog pd;
        private List<LocationItem> items;
        public LargeTask(ProgressDialog pd) {
            this.pd = pd;
        }

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
                    JSONObject requestBody = null;
                    try {
                        requestBody = getRequestBody();
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

        @Override
        protected void onPostExecute(Void response) {
            if (DEBUG) Log.d(TAG, "onPostExecute");
            dismissProgressDialog();
            switch (action){
                case sDownloadFromLocal:
                    LocationsFragment.this.mLocations = items;
                    break;
                case sUploadToServer:
                    mLocations.clear();
                    break;
                case sClearLocalData:
                    mLocations.clear();
                    break;
            }
            try {
                setListView();
            } catch (Exception e){
                if (ERROR) Log.e(TAG, "Attempt to invoke virtual method 'java.lang.Object android.content.Context.getSystemService(java.lang.String)' on a null object reference");
            }
            super.onPostExecute(response);
        }

        private void dismissProgressDialog() {
            if (mProgressDialog != null && mProgressDialog.isShowing()) {
                mProgressDialog.dismiss();
            }
        }
    }

    private void setListView() {
        if (DEBUG) Log.d(TAG, "setListView");
        if (DEBUG) Log.d(TAG, "mLocations size: " + mLocations.size());
        List<Map<String, String>> list = new ArrayList<>();
        Map<String, String> map;
        Date date;
        int count = mLocations.size();
        for(int i = 0; i < count; i++) {
            map = new HashMap<>();
            date = new Date(mLocations.get(i).getTime());
            map.put("value", mLocations.get(i).getLatitude() + ", " + mLocations.get(i).getLongitude());
            map.put("time", sFormat.format(date));
            list.add(map);
        }
        mAdapter = new SimpleAdapter(getContext(), list, R.layout.collected_data_item, new String[] { "value", "time" },
                new int[] {R.id.collected_metric_value, R.id.collected_metric_time});
        locationsListView.setAdapter(mAdapter);
        mAdapter.notifyDataSetChanged();
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        getActivity().getMenuInflater().inflate(R.menu.location_item_menu, menu);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        switch (item.getItemId())
        {
            case R.id.option_copy_coordinates:
                ClipboardManager clipboard = (ClipboardManager) getActivity().getSystemService(Context.CLIPBOARD_SERVICE);
                String text = mLocations.get(info.position).getLatitude() + ", " + mLocations.get(info.position).getLongitude();
                ClipData clip = ClipData.newPlainText("copied text", text);
                clipboard.setPrimaryClip(clip);
                return true;
        }
        return super.onContextItemSelected(item);
    }

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
            Toast.makeText(getContext(), getResources().getString(R.string.cant_upload_locations_toast_message), Toast.LENGTH_LONG).show();
        }
        return super.onOptionsItemSelected(item);
    }

    private void clearData() {
        action = sClearLocalData;
        new LargeTask(mProgressDialog).execute();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mLocations != null){
            setListView();
        }
    }
}