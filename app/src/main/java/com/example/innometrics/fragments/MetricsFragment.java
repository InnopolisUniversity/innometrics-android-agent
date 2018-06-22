package com.example.innometrics.fragments;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.innometrics.adapters.MetricItem;
import com.example.innometrics.adapters.MetricsAdapter;
import com.example.innometrics.R;
import com.example.innometrics.utils.ConnectionUtils;
import com.example.innometrics.utils.ApplicationUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * MetricsFragments is one of the fragments of MainActivity
 * It displays metrics of a user (and if you press on arrow - a metric's details in json) in a ListView.
 * Pressing a metric will open new activity with metric data represented (if can be)
 */
public class MetricsFragment extends Fragment{
    public static final String TAG = "MetricsFragment";
    public static final boolean DEBUG = ApplicationUtils.DEBUG;

    public static final String METRICS_ARRAY = "metrics";

    private RecyclerView mMetricsRecyclerView;
    private JSONArray mMetricsJSON;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_metrics, container, false);
        mMetricsRecyclerView = view.findViewById(R.id.metrics_recycle_view);
        mMetricsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        mMetricsRecyclerView.setHasFixedSize(true);

        try {
            mMetricsJSON = getMetrics();
            setMetrics();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return view;
    }

    /**
     * Get metrics from SharedPreferences and put them into RecyclerView
     * @return array of metrics
     * @throws JSONException
     */
    private JSONArray getMetrics() throws JSONException {
        //get metrics from prefs
        SharedPreferences metricsPrefs = getActivity().getSharedPreferences(ConnectionUtils.PREFS_METRICS, Context.MODE_PRIVATE);
        assert metricsPrefs.contains(ConnectionUtils.PREFS_METRICS_METRICS) : "metricsPrefs should contain metrics";
        JSONObject metricsJSON = new JSONObject(metricsPrefs.getString(ConnectionUtils.PREFS_METRICS_METRICS, "default value"));
        assert metricsJSON.has(METRICS_ARRAY) : "metrics json should contain metrics array";
        JSONArray metricsArray = metricsJSON.getJSONArray(METRICS_ARRAY);
        if (DEBUG) Log.d(TAG, "metrics prefs array: " + metricsArray.toString());
        return metricsArray;
    }


    /**
     * To put data into RecyclerView we have to make an adapter
     * @see MetricsAdapter
     * @throws JSONException
     */
    private void setMetrics() throws JSONException {
        if (DEBUG) Log.d(TAG, "setMetrics");
        ArrayList<MetricItem> metrics = new ArrayList<>();
        for (int i = 0; i < mMetricsJSON.length(); i++) {
            JSONObject metricJson = mMetricsJSON.getJSONObject(i);
            MetricItem metric = new MetricItem(metricJson.getInt(ConnectionUtils.METRICS_METRIC_ID),
                    metricJson.getString(ConnectionUtils.METRICS_METRIC_NAME),
                    metricJson.getString(ConnectionUtils.METRICS_METRIC_TYPE),
                    metricJson.getJSONObject(ConnectionUtils.METRICS_METRIC_INFO));
            metrics.add(metric);
        }
        MetricsAdapter adapter = new MetricsAdapter(getContext(), metrics);
        mMetricsRecyclerView.setAdapter(adapter);
    }
}