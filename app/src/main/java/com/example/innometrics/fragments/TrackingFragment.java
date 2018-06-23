package com.example.innometrics.fragments;

import android.Manifest;
import android.app.AppOpsManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

import com.example.innometrics.CollectedMetricsActivity;
import com.example.innometrics.R;
import com.example.innometrics.TrackingSettingsActivity;
import com.example.innometrics.services.LocationService;
import com.example.innometrics.services.ForegroundAppService;
import com.example.innometrics.utils.ApplicationUtils;


/**
 * One of the MainActivity fragments.
 * 3 Buttons: start (stop) tracking, go to tracking settings or tracked data activity
 * Handles permissions: if location permission is off - explains why it needs it and attempts to request the permission,
 * to allow PACKAGE_USAGE_STATS user needs to manually go to settings and allow it
 * The application will ask to proceed to settings (directly) where this permission can be enabled
 */
public class TrackingFragment extends Fragment implements View.OnClickListener {
    public static final String TAG = "TrackingFragment";
    public static final boolean DEBUG = ApplicationUtils.DEBUG;

    private static final String sButtonStrSavedStateKey = "buttonStr";

    public static final String PREFS_TRACK_APPS = "switch_track_foreground_app";
    public static final String PREFS_TRACK_LOCATION = "switch_track_location";
//    public static final String PREFS_TRACK_WIFI_DATA = "switch_track_wifi_data";
    private static final int PERMISSION_LOCATION = 1;


    private Button mButtonTrackingService;
    private ImageButton mButtonSettings;
    private ImageButton mButtonCollectedData;
    private SharedPreferences mDefaultPreferences;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_tracking, container, false);
        setButtons(v);
        if (savedInstanceState != null) {
            mButtonTrackingService.setText(savedInstanceState.getString(sButtonStrSavedStateKey));
        }
        if (isMyServiceRunning(LocationService.class) || isMyServiceRunning(ForegroundAppService.class)) {
            mButtonTrackingService.setText(getResources().getString(R.string.button_stop_service_str));
        }
        mButtonTrackingService.setOnClickListener(this);
        mButtonSettings.setOnClickListener(this);
        mButtonCollectedData.setOnClickListener(this);

        setHasOptionsMenu(true);

        return v;
    }
    public void setButtons(View view) {
        mButtonTrackingService = view.findViewById(R.id.button_tracking_service);
        mButtonSettings = view.findViewById(R.id.button_tracking_settings);
        mButtonCollectedData = view.findViewById(R.id.button_tracking_data);
        mDefaultPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(sButtonStrSavedStateKey, (String) mButtonTrackingService.getText());
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.button_tracking_service:
                if (!isMyServiceRunning(LocationService.class) && !isMyServiceRunning(ForegroundAppService.class)) {
                    //if button is for start - start and make button for stop and versa.
                    //one has to check permissions and ask to grant them
                    boolean started = false;
                    if (mDefaultPreferences.getBoolean(PREFS_TRACK_APPS, false)) {
                        if (isPackageUsageStatsGranted()){
                            if (DEBUG) Log.d(TAG, "track apps");
                            started = true;
                            getActivity().startService(new Intent(getContext(), ForegroundAppService.class));
                        } else {
                            showTrackingPermissionDescription();
                        }
                    }
                    if (mDefaultPreferences.getBoolean(PREFS_TRACK_LOCATION, false) && checkLocationPermissions()) {
                        if (DEBUG) Log.d(TAG, "track location");
                        started = true;
                        getActivity().startService(new Intent(getContext(), LocationService.class));
                    }
                    if (started){
                        mButtonTrackingService.setText(getResources().getString(R.string.button_stop_service_str));
                    }
                } else {
                    if (DEBUG) Log.d(TAG, "stop services");
                    getActivity().stopService(new Intent(getContext(), LocationService.class));
                    getActivity().stopService(new Intent(getContext(), ForegroundAppService.class));
                    mButtonTrackingService.setText(getResources().getString(R.string.button_start_service_str));
                }
                break;
            case R.id.button_tracking_settings:
                if (DEBUG) Log.d(TAG, "button settings clicked");
                if (isMyServiceRunning(LocationService.class) || isMyServiceRunning(ForegroundAppService.class)){
                    Toast.makeText(getContext(), getResources().getString(R.string.cant_do_due_to_services_toast_message), Toast.LENGTH_LONG).show();
                } else {
                    Intent toSettings = new Intent(getContext(), TrackingSettingsActivity.class);
                    startActivity(toSettings);
                }
                break;
            case R.id.button_tracking_data:
                if (DEBUG) Log.d(TAG, "button tracking data");
                Intent toShowCollectedData = new Intent(getContext(), CollectedMetricsActivity.class);
                startActivity(toShowCollectedData);
                break;
        }
    }

    private void showTrackingPermissionDescription() {
        AlertDialog.Builder alertBuilder = new AlertDialog.Builder(getContext());
        alertBuilder.setCancelable(true);
        alertBuilder.setTitle("Package usage stats permission is needed");
        alertBuilder.setMessage("Innometrics needs to track your foreground apps.\n" +
                        "A user only can disable or enable this functionality in an app in phone's settings.\n" +
                        "Would you like to proceed to phone's settings?"
                        );
        alertBuilder.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS));
            }
        });
        alertBuilder.setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        AlertDialog alert = alertBuilder.create();
        alert.show();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.tracking_fragment_action_menu, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case R.id.action_tracking_info:
                showTrackingInfoDialog();
                return true;
            default: return super.onOptionsItemSelected(item);
        }
    }

    private void showTrackingInfoDialog() {
        AlertDialog.Builder alertBuilder = new AlertDialog.Builder(getContext());
        alertBuilder.setCancelable(true);
        alertBuilder.setTitle(getResources().getString(R.string.tracking_info_title));
        alertBuilder.setMessage(getResources().getString(R.string.tracking_info_message));
        alertBuilder.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        AlertDialog alert = alertBuilder.create();
        alert.show();
    }

    private boolean checkLocationPermissions() {
        // Here, thisActivity is the current activity
        if (ContextCompat.checkSelfPermission(getActivity(),
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            if (DEBUG) Log.d(TAG, "permission wasn't granted");
            // Permission is not granted
            // Should we show an explanation?
            if (shouldShowRequestPermissionRationale(
                    Manifest.permission.ACCESS_FINE_LOCATION)) {
                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
                AlertDialog.Builder alertBuilder = new AlertDialog.Builder(getContext());
                alertBuilder.setCancelable(true);
                alertBuilder.setTitle(getResources().getString(R.string.location_permission_needed_title));
                alertBuilder.setMessage(getResources().getString(R.string.location_permission_needed_message));
                alertBuilder.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        requestPermissions(
                                new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                                PERMISSION_LOCATION);
                    }
                });

                AlertDialog alert = alertBuilder.create();
                alert.show();
            } else {
                // No explanation needed; request the permission
                ActivityCompat.requestPermissions(getActivity(),
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        PERMISSION_LOCATION);

                // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                // app-defined int constant. The callback method gets the
                // result of the request.
            }
        } else {
            return true;
        }
        return false;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (DEBUG) Log.d(TAG, "onRequestPermissionsResult");
        switch (requestCode) {
            case PERMISSION_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.
                    if (DEBUG) Log.d(TAG, "permission is granted after asking");
                    getActivity().startService(new Intent(getContext(), LocationService.class));
                    mButtonTrackingService.setText(getResources().getString(R.string.button_stop_service_str));
                } else {
                    Toast.makeText(getContext(), getResources().getString(R.string.cant_track_location_permission), Toast.LENGTH_LONG).show();
                }
                return;
            }
            // other 'case' lines to check for other
            // permissions this app might request.
        }
    }

    private boolean isPackageUsageStatsGranted(){
        boolean granted;
        AppOpsManager appOps = (AppOpsManager) getContext().getSystemService(Context.APP_OPS_SERVICE);
        int mode = appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(), getContext().getPackageName());
        if (mode == AppOpsManager.MODE_DEFAULT) {
            granted = (getContext().checkCallingOrSelfPermission(android.Manifest.permission.PACKAGE_USAGE_STATS) == PackageManager.PERMISSION_GRANTED);
        } else {
            granted = (mode == AppOpsManager.MODE_ALLOWED);
        }
        return granted;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (isMyServiceRunning(LocationService.class) || isMyServiceRunning(ForegroundAppService.class)) {
            mButtonTrackingService.setText(getResources().getString(R.string.button_stop_service_str));
        } else {
            mButtonTrackingService.setText(getResources().getString(R.string.button_start_service_str));
        }
    }

    private boolean isMyServiceRunning(Class<?> serviceClass) {
        return ApplicationUtils.isMyServiceRunning(serviceClass, getActivity());
    }
}
