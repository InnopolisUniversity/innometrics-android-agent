package com.example.innometrics.server;

import android.os.AsyncTask;
import android.util.Log;

import com.example.innometrics.utils.ApplicationUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.HttpURLConnection;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * this class is AsyncTask, but mostly serves as
 * a container for static method requestToServer,
 * which is used in other AsyncTasks.
 */
public class Connection extends AsyncTask<ServerRequestItem, Void, ResponseObject> {
    private static final String TAG = "Connection";
    public static final boolean DEBUG = ApplicationUtils.DEBUG;
    public static final boolean ERROR = ApplicationUtils.ERROR;

    public static OkHttpClient sClient = new OkHttpClient();

    public static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    @Override
    protected ResponseObject doInBackground(ServerRequestItem... params) {
        return requestToServer(params);
    }

    /**
     *
     * @param params ServerRequestItem consists must consist of url and/or token and/or url
     *               @see ServerRequestItem
     * @return ResponseObject which consist of JSON response from server and response code
     *              @see ResponseObject
     */
    public static ResponseObject requestToServer(ServerRequestItem... params){
        if (params.length > 1){
            throw new IllegalArgumentException("Should be only 1 parameter");
        }
        //depending on ServerRequestItem Connection requests differently
        //if body is null, then request method is GET, otherwise POST
        //if token is not null, then Connection adds header "Authorization"
        //The application didn't need other types of requests so far (22.06.2018)
        ServerRequestItem requestItem = params[0];
        if (DEBUG) Log.d(TAG,"REQUEST url: " + requestItem.getUrl());
        if (DEBUG) Log.d(TAG,"REQUEST token: " + requestItem.getToken());
        if (DEBUG) Log.d(TAG,"REQUEST body: " + requestItem.getBody());
        Request request;
        if (requestItem.getToken() == null){
            if (requestItem.getBody() == null) {
                request = new Request.Builder()
                        .url(requestItem.getUrl())
                        .build();
                if (ERROR) Log.e(TAG, "WRONG REQUEST");
            } else {
                RequestBody body = RequestBody.create(JSON,requestItem.getBody());
                request = new Request.Builder()
                        .url(requestItem.getUrl())
                        .post(body)
                        .build();
            }
        } else {
            String tokenAugmented = "Token ".concat(requestItem.getToken());
            if (requestItem.getBody() == null) {
                request = new Request.Builder()
                        .url(requestItem.getUrl())
                        .header("Authorization", tokenAugmented)
                        .build();
            } else {
                RequestBody body = RequestBody.create(JSON, requestItem.getBody());
                request = new Request.Builder()
                        .url(requestItem.getUrl())
                        .header("Authorization", tokenAugmented)
                        .post(body)
                        .build();
            }
        }
        if (DEBUG) Log.d(TAG, request.headers().toString());
        try {
            Response response = sClient.newCall(request).execute();
            if (DEBUG) Log.d(TAG, "HTTP CODE: " + response.code());
            try {
                String responseBody  = response.body().string();
                if (responseBody == null) {
                    if (ERROR) Log.e(TAG, "responseBody is null!");
                } else if (DEBUG) Log.d(TAG, "response: " + responseBody);

                return new ResponseObject(response.code(), new JSONObject(responseBody));
            } catch (JSONException e) {
                e.printStackTrace();
                return null;
            }
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}