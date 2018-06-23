package com.example.innometrics.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;
import android.widget.Toast;

import com.example.innometrics.R;
import com.example.innometrics.server.Connection;
import com.example.innometrics.server.ResponseObject;
import com.example.innometrics.server.ServerRequestItem;
import com.example.innometrics.utils.ApplicationUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.HttpURLConnection;
import java.util.concurrent.ExecutionException;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;

/**
 * Connection utils consists of methods for working with requests
 * and constants to use throughout application, regarding server.
 */
public class ConnectionUtils {
    public static final String TAG = "ConnectionUtils";
    public static final boolean DEBUG = ApplicationUtils.DEBUG;
    public static final boolean ERROR = ApplicationUtils.ERROR;

    //server urls
    public static final String DOMAIN_NAME = "http://188.130.155.78:8000/";
    public static final String URL_REGISTER = "http://188.130.155.78:8000/register/";
    public static final String URL_LOGIN_TOKEN = "http://188.130.155.78:8000/api-token-auth/";
    public static final String URL_PROJECTS = "http://188.130.155.78:8000/project/";
    public static final String URL_METRICS = "http://188.130.155.78:8000/projects/metrics/";
    public static final String URL_METRIC_DATA = "http://188.130.155.78:8000/projects/metrics/&s/data";
    public static final String URL_ACTIVITIES = "http://188.130.155.78:8000/projects/metrics/activities/";
    public static final String URL_VALUES = "http://188.130.155.78:8000/projects/metrics/values/";

    public static final String URL_SEND_ACTIVITIES = "http://188.130.155.78:8000/activities/";

    //for json objects from server

    //register and login
    public static final String REGISTER_USERNAME = "username";
    public static final String REGISTER_EMAIL = "email";
    public static final String REGISTER_PASSWORD = "password";
    public static final String REGISTER_TOKEN = "token";
    public static final String REGISTER_NON_FIELD_ERRORS = "non_field_errors";
    public static final String LOGIN_USERNAME = REGISTER_USERNAME;
    public static final String LOGIN_PASSWORD = REGISTER_PASSWORD;
    public static final String LOGIN_NON_FIELD_ERRORS = REGISTER_NON_FIELD_ERRORS;
    //response objects
    public static final String ACTIVITIES = "activities";
    public static final String ACTIVITY_NAME = "name";
    public static final String ACTIVITY_PROPERTIES_NAME = "properties";
    public static final String ACTIVITY_PROPERTY_NAME = "name";
    public static final String ACTIVITY_PROPERTY_TYPE = "type";

    public static final String METRICS = "metrics";
    public static final String METRICS_METRIC_NAME = "name";
    public static final String METRICS_METRIC_ID = "id";
    public static final String METRICS_METRIC_TYPE = "type";
    public static final String METRICS_METRIC_INFO = "info";
    //for shared preferences
    public static final String PREFS_USER = "userPrefs";
    public static final String PREFS_USER_TOKEN = REGISTER_TOKEN;
    public static final String PREFS_USER_USERNAME = REGISTER_USERNAME;
    public static final String PREFS_USER_PASSWORD = REGISTER_PASSWORD;
    public static final String PREFS_USER_EMAIL = REGISTER_EMAIL;

    public static final String PREFS_ACTIVITIES = "activities";
    public static final String PREFS_ACTIVITIES_ACTIVITIES = PREFS_ACTIVITIES;
    public static final String PREFS_METRICS = "metrics";
    public static final String PREFS_METRICS_METRICS = PREFS_METRICS;

    //for sending data
    public static final String SEND_ACTIVITIES = "activities";
    public static final String SEND_ACTIVITY_NAME = "name";
    public static final String SEND_ACTIVITY_COMMENT = "comment";
    public static final String SEND_ACTIVITY_MEASUREMENTS = "measurements";
    public static final String SEND_ACTIVITY_MEASUREMENT_TYPE = "type";
    public static final String SEND_ACTIVITY_MEASUREMENT_NAME = "name";
    public static final String SEND_ACTIVITY_MEASUREMENT_VALUE = "value";


    public static ResponseObject request(ServerRequestItem requestItem){
        try {
            //.get() is used, so UI waits when request will finish (and doesn't load views)
            //therefore it is good only for small requests
            ResponseObject result =  new Connection().execute(requestItem).get();
            return result;
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * @return true if user prefs are correct
     */
    public static boolean userIsLoggedIn(SharedPreferences prefs) {
        if (DEBUG) Log.d(TAG, "userIsLoggedIn method");
        String token = userIsLoggedInRequest(prefs.getString(PREFS_USER_USERNAME, ""), prefs.getString(PREFS_USER_PASSWORD, ""));
        if (DEBUG) Log.d(TAG, "userIsLoggedIn method token: " + token);
        if (!token.equals("")) {
            SharedPreferences.Editor editor = prefs.edit().putString(PREFS_USER_TOKEN, token);
            editor.apply();
            if (DEBUG) Log.d(TAG, "token.equals(\"\")");
            return true;
        }
        if (DEBUG) Log.d(TAG, "token.equals(\"\") false");
        return false;
    }

    /**
     * check if user's username and password are correct
     * @return token or ""
     */
    public static String userIsLoggedInRequest(String username, String password){
        JSONObject loginAndPassword = new JSONObject();
        try {
            loginAndPassword.accumulate(LOGIN_USERNAME, username);
            loginAndPassword.accumulate(LOGIN_PASSWORD, password);
            ResponseObject answer = request(new ServerRequestItem(URL_LOGIN_TOKEN, null, loginAndPassword.toString()));
            if (answer == null){
                if (ERROR) Log.e(TAG, "answer is null!");
                return "";
            }
            if (answer.getResponseCode() == HttpURLConnection.HTTP_OK){
                JSONObject body = answer.getResponse();
                return body.getString(REGISTER_TOKEN);
            } else return "";
        } catch (JSONException e) {
            e.printStackTrace();
            return "";
        }
    }

    /**
     * @return true if connection is available
     * @param showToast - show Toast "No Internet Connection"
     */
    public static boolean networkAvailable(Context context, boolean showToast){
        if (isNetworkConnected(context)){
            return true;
        } else {
            if (showToast){
                Toast.makeText(context, context.getResources().getString(R.string.no_network_toast_message), Toast.LENGTH_LONG).show();
            }
            return false;
        }
    }

    public static boolean isNetworkConnected(Context context) {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }
}
