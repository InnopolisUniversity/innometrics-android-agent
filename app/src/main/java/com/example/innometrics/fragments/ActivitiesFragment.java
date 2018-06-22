package com.example.innometrics.fragments;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.example.innometrics.ActivityDetails;
import com.example.innometrics.R;
import com.example.innometrics.utils.ConnectionUtils;
import com.example.innometrics.utils.ApplicationUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * ActivitiesFragments is one of the fragments of MainActivity
 * It displays activities of a user (e.g. Windows activity) in a ListView.
 * Pressing an activity will show it in details (measurements information)
 */
public class ActivitiesFragment extends Fragment {
    public static final String TAG = "ActivitiesFragment";
    public static final boolean DEBUG = ApplicationUtils.DEBUG;

    private ListView mActivitiesListView;
    private ArrayAdapter<String> mAdapter;
    private JSONArray mActivitiesJSON;

    public static final String ACTIVITY_DETAILS_KEY = "activityDetails";
    public static final String ACTIVITIES_ARRAY = "activities";


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_activities, container, false);
        mActivitiesListView = view.findViewById(R.id.activities_list_view);
        try {
            mActivitiesJSON = getActivities();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        setListView();
        return view;
    }

    /**
     * Get activities from SharedPreferences and put them into ListView
     * @return array of activities
     * @throws JSONException
     */
    public JSONArray getActivities() throws JSONException {
        SharedPreferences activitiesPrefs = getActivity().getSharedPreferences(ConnectionUtils.PREFS_ACTIVITIES, Context.MODE_PRIVATE);
        assert activitiesPrefs.contains(ConnectionUtils.PREFS_ACTIVITIES_ACTIVITIES) : "activitiesPrefs should contain activities";
        JSONObject activitiesJSON = new JSONObject(activitiesPrefs.getString(ConnectionUtils.PREFS_ACTIVITIES_ACTIVITIES, "default value"));
        assert activitiesJSON.has(ACTIVITIES_ARRAY) : "activities json should contain activities array";
        JSONArray activitiesArray = activitiesJSON.getJSONArray(ACTIVITIES_ARRAY);
        if (DEBUG) Log.d(TAG, "activities prefs array: " + activitiesArray.toString());
        return activitiesArray;
    }

    /**
     * Bind activities data and the ListView
     */
    private void setListView(){
        ArrayList<String> activitiesStrings = new ArrayList<>();
        try {
            for (int i = 0; i < mActivitiesJSON.length(); i++) {
                activitiesStrings.add(mActivitiesJSON.getJSONObject(i).getString(ConnectionUtils.ACTIVITY_NAME));
            }
            if (mAdapter == null) {
                mAdapter = new ArrayAdapter<>(getContext(),
                        R.layout.custom_item, //data holder
                        R.id.task_content, // view holder
                        activitiesStrings); // array of data
                mActivitiesListView.setAdapter(mAdapter);
            } else {
                mAdapter.clear();
                mAdapter.addAll(activitiesStrings);
                mAdapter.notifyDataSetChanged();
            }
            mActivitiesListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> adapter, View view, int position, long arg) {
                    try {
                        JSONObject activity = mActivitiesJSON.getJSONObject(position);
                        Intent activityDetails = new Intent(getContext(), ActivityDetails.class);
                        activityDetails.putExtra(ACTIVITY_DETAILS_KEY, activity.toString());
                        startActivity(activityDetails);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            });
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}
