package com.example.innometrics;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import com.example.innometrics.fragments.ActivitiesFragment;
import com.example.innometrics.utils.ConnectionUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Shows details of an activity: types and names of measurements
 * @see ActivitiesFragment
 */
public class ActivityDetails extends BasicActivity {
    private ListView activityListView;
    private TextView activityNameTextView;

    private SimpleAdapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_details);
        setUI();
        Intent intent = getIntent();
        String jsonStr = intent.getStringExtra(ActivitiesFragment.ACTIVITY_DETAILS_KEY);
        try {
            JSONObject jsonObject = new JSONObject(jsonStr);
            setActivityListView(jsonObject);
        } catch (JSONException e) {
        }
    }

    private void setUI() {
        activityListView = findViewById(R.id.activity_details_list_view);
        activityNameTextView = findViewById(R.id.activity_details_activity_name);
    }

    private void setActivityListView(JSONObject activity) throws JSONException {
        String activityName = activity.getString(ConnectionUtils.ACTIVITY_NAME);
        activityNameTextView.setText(activityName);

        JSONArray properties = activity.getJSONArray(ConnectionUtils.ACTIVITY_PROPERTIES_NAME);

        if (mAdapter == null) {
            List<Map<String, String>> list = new ArrayList<>();
            Map<String, String> map;
            int count = properties.length();
            for(int i = 0; i < count; i++) {
                map = new HashMap<>();
                JSONObject property = properties.getJSONObject(i);
                map.put("name", property.getString(ConnectionUtils.ACTIVITY_PROPERTY_NAME));
                map.put("type", property.getString(ConnectionUtils.ACTIVITY_PROPERTY_TYPE));
                list.add(map);
            }
            //Simple adapter is used instead of ArrayAdapter so it can take 2 elements to display
            //the same job can be done using RecyclerView, but this is simpler and has nice separating line between rows
            mAdapter = new SimpleAdapter(this, list, R.layout.activity_property_item, new String[] { "name", "type" }, new int[] {R.id.metric_property_label, R.id.metric_property_information});
            activityListView.setAdapter(mAdapter);
        } else {
            mAdapter.notifyDataSetChanged();
        }
    }

}
