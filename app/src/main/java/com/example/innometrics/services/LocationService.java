package com.example.innometrics.services;

import android.Manifest;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.ActivityCompat;
import android.util.Log;

import com.example.innometrics.local_data.AppDatabase;
import com.example.innometrics.local_data.LocationItem;
import com.example.innometrics.local_data.LocationItemDao;
import com.example.innometrics.utils.ApplicationUtils;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;

import java.util.List;

public class LocationService extends Service implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener {
    private static final String TAG = "LocationService";
    public static final boolean DEBUG = ApplicationUtils.DEBUG;
    public static final boolean ERROR = ApplicationUtils.ERROR;

    public static final int INTERVAL = 5000;
    public static final int FASTEST_INTERVAL = INTERVAL / 2;
    //minimal change in meters to previous location to log
    public static final double MINIMAL_CHANGE = 5;
    //if Location.getAccuracy returns 10, then there's a 68% chance the true location of the device is within 10 meters of the reported coordinates
    public static final int MINIMAL_ACCURACY = 14;


    private boolean currentlyProcessingLocation = false;
    private LocationRequest mLocationRequest;
    private GoogleApiClient mGoogleApiClient;
    private FusedLocationProviderClient mFusedLocationClient;
    private LocationItemDao mDao;
    private LocationItem mLastLocationItem;

    @Override
    public void onCreate() {
        super.onCreate();
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(getApplicationContext());
        mDao = AppDatabase.getInstance(getBaseContext()).locationDao();
        if (mDao == null) {
            if (ERROR) Log.e(TAG, "DAO IS NULL!");
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startTracking();
        startLocationUpdates();
        return START_NOT_STICKY;
    }

    private void startLocationUpdates() {
        if (DEBUG) Log.d(TAG, "startLocationUpdates");
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            if (DEBUG) Log.d(TAG, "no permission");
            return;
        }
        mFusedLocationClient.getLastLocation().addOnSuccessListener(new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {
                if (location != null) {
                    if (DEBUG) Log.d(TAG, "get location at the beginning");
                    saveLocation(location);
                } else {
                    if (DEBUG) Log.d(TAG, "location at the beginning is null");
                }
            }
        });
    }

    @Override
    public void onLocationChanged(Location location) {
        if (location != null) {
            if (ERROR) Log.e(TAG, "position: " + location.getLatitude() + ", " + location.getLongitude() + " accuracy: " + location.getAccuracy());
            saveLocation(location);
        }
    }

    private void saveLocation(final Location location){
        Thread thread = new Thread(){
            @Override
            public void run() {
                LocationItem locationItem = new LocationItem();
                locationItem.setLatitude(ApplicationUtils.round(location.getLatitude(), 7));
                locationItem.setLongitude(ApplicationUtils.round(location.getLongitude(), 7));
                long time = System.currentTimeMillis();
                locationItem.setTime(time);
                boolean canInsert = false;
                if (mLastLocationItem != null){
                    float [] dist = new float[1];
                    Location.distanceBetween(mLastLocationItem.getLatitude(), mLastLocationItem.getLongitude(),
                            locationItem.getLatitude(), locationItem.getLongitude(), dist);
                    Float distanceChange = dist[0];
                    if (DEBUG) Log.d(TAG, "distanceChange: " + distanceChange);
                    if (DEBUG) Log.d(TAG, "time before: " + mLastLocationItem.getTime());
                    if (DEBUG) Log.d(TAG, "time now:    " + time);
                    if (location.getAccuracy() < MINIMAL_ACCURACY)
                    if (mLastLocationItem.getTime() != locationItem.getTime()
                            && distanceChange > MINIMAL_CHANGE) {
                        canInsert = true;
                    }
                } else {
                    canInsert = true;
                }
                if (canInsert){
                    if (DEBUG) Log.d(TAG, "location saved");
                    mLastLocationItem = locationItem;
                    mDao.insertAll(locationItem);
                } else {
                    if (DEBUG) Log.d(TAG, "location omitted");
                }
            }
        };
        thread.start();
    }

    private void startTracking() {
        if (DEBUG) Log.d(TAG, "startTracking");
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        if (!mGoogleApiClient.isConnected() || !mGoogleApiClient.isConnecting()) {
            mGoogleApiClient.connect();
        } else {
            if (ERROR) Log.e(TAG, "unable to connect to google play services.");
        }
    }

    @Override
    public void onDestroy() {
        if (DEBUG) Log.d(TAG, "onDestroy");
        Thread thread = new Thread(){
            @Override
            public void run() {
                List<LocationItem> locationItems = mDao.getAll();
                if (DEBUG) Log.d(TAG, "locations: ");
                for (int i = 0; i < locationItems.size(); i++) {
                    LocationItem item = locationItems.get(i);
                    if (DEBUG) Log.d(TAG, item.getLatitude() + ", " + item.getLongitude() + ", time: " + item.getTime());
                }
            }
        };
        thread.start();
        stopLocationUpdates();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void stopLocationUpdates() {
        if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
    }

    /**
     * Called by Location Services when the request to connect the
     * client finishes successfully. At this point, you can
     * request the current location or start periodic updates
     */
    @Override
    public void onConnected(Bundle bundle) {
        if (DEBUG) Log.d(TAG, "onConnected");

        mLocationRequest = LocationRequest.create();
        mLocationRequest.setInterval(INTERVAL);
        mLocationRequest.setFastestInterval(FASTEST_INTERVAL);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        try {
            //TODO: FusedLocationApi is deprecated, change to FusedLocationProviderClient
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
//            FusedLocationProviderClient fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(getApplicationContext());
        } catch (SecurityException se) {
            if (ERROR) Log.e(TAG, "Go into settings and find Gps Tracker app and enable Location.");
        }
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        if (ERROR) Log.e(TAG, "onConnectionFailed");
        stopLocationUpdates();
        stopSelf();
    }

    @Override
    public void onConnectionSuspended(int i) {
        if (ERROR) Log.e(TAG, "onConnectionSuspended");
    }
}