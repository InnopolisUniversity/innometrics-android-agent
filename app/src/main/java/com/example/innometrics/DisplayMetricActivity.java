package com.example.innometrics;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.ListView;
import android.widget.SimpleAdapter;

import com.example.innometrics.adapters.LegendAdapter;
import com.example.innometrics.adapters.LegendItem;
import com.example.innometrics.utils.ConnectionUtils;
import com.example.innometrics.server.Connection;
import com.example.innometrics.server.ResponseObject;
import com.example.innometrics.server.ServerRequestItem;
import com.example.innometrics.utils.ApplicationUtils;
import com.example.innometrics.utils.PieChartValueFormatter;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.LegendEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.github.mikephil.charting.utils.MPPointF;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * DisplayMetricActivity displays some of the metrics: urls (PieChart) or those with int type (Simple Graph).
 * Other kinds of metrics are downloaded, but cannot be shown (yet).
 * Metric data is stored in separate SharedPreferences with key = "metric" + metricId.
 * If data is always there, old data is shown. To update a user has to press sync button in action bar.
 * I violated android name convention here to treat non-final variables as any other final in code.
 * Raw metrics and Composite metrics are converted to format, where they can be treated as the same.
 * If server format changes - change final constants and methods:
 * @see DisplayMetricActivity#prepareDataForRawMetrics()
 * @see DisplayMetricActivity#setConstantNamesForRawData()
 * @see DisplayMetricActivity#prepareDataForCompositeMetrics()
 * @see DisplayMetricActivity#setConstantNamesForCompositeData()
 * P.S.: I am not very happy about this approach
 * @author reshreshus@gmail.com
 */
public class DisplayMetricActivity extends BasicActivity {
    public static final String TAG = "DisplayMetricActivity";
    private static final boolean DEBUG = ApplicationUtils.DEBUG;

    public static final String PREFS_METRIC = "mMetric";
    public static final String PREFS_METRIC_EDITOR_KEY = "mMetric";

    public static final String METRIC_TYPE_RAW = "R";
    public static final String METRIC_TYPE_COMPOSITE = "C";
    public static final String METRIC_TYPE_RAW_PREPARED = "RP";
    public static final String METRIC_TYPE_COMPOSITE_PREPARED = "CP";

    public static final String RAW_METRICS_MEASUREMENTS = "measurements";
    public static final String RAW_METRIC_VALUE = "value";
    public static final String RAW_METRIC_ID = "id";
    public static final String RAW_METRIC_ACTIVITY_ID = "activity_id";
    public static final String RAW_METRIC_VALUE_TYPE = "type";
    public static final String RAW_METRIC_NAME = "name";
    public static final String RAW_METRIC_VALUE_NAME = "name";
    public static final String RAW_METRIC_ACTIVITY = "entity";
    public static final String RAW_METRIC_FIELD = "name";


    public static final String C_METRIC_TYPE = "type";
    public static final String C_METRIC_NAME = "name";
    public static final String C_METRIC_INFO = "info";
    public static final String C_METRIC_ID = "id";
    public static final String C_METRIC_PARTICIPATION = "participation";
    public static final String INFO_C_METRIC_PARTICIPATION = C_METRIC_PARTICIPATION;
    public static final String C_METRIC_VALUE = "value";

    public static String METRIC_VALUE_NAME = "value_name";
    public static String INFO = "info";
    public static String METRIC_TYPE = "type";
    public static String METRIC_VALUE_TYPE = "value_type";
    public static String METRIC_NAME ="metric_name";
    public static String X_VALUES = "x_values";
    public static String Y_VALUES = "y_values";
    public static String INFO_METRIC_VALUE = C_METRIC_VALUE;
    public static String METRIC_ACTIVITY = "activity";
    public static String METRIC_FIELD = "field";
    public static final String PIE_NORMALIZED_FLAG = "pie_chart_type";
    public static String METRIC_ID = RAW_METRIC_ID;

    public static final String METRIC_VALUE_TYPE_INT = "int";
    public static final String METRIC_FIELD_URL = "url";
    public static final String METRIC_FIELD_URL_NORMALIZED = "url_normalized";

    private ListView mMetricDescriptionListView;
    private RecyclerView mMetricLegendRecyclerView;
    private SimpleAdapter mAdapter;
    private ArrayList<LegendItem> mLegendItems;
    private JSONObject mMetric;
    private String mMetricId;
    private SharedPreferences mMetricPrefs;
    private ProgressDialog mProgressDialog = null;
    //how many slices should pie chart display
    private int numberOfSlices = 10;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_display_metric);
        Intent toGetId = getIntent();
        Bundle extras = toGetId.getExtras();
        mMetricId = Integer.toString(extras.getInt(ConnectionUtils.METRICS_METRIC_ID));
        setProgressDialog();
        try {
            if (setMetricData()) {
                setMetric();
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void setProgressDialog(){
        mProgressDialog = new ProgressDialog(this, R.style.AppBaseDialog);
        mProgressDialog.setIndeterminate(true);
        mProgressDialog.setCancelable(false);
        mProgressDialog.setMessage("Preparing metrics...");
    }

    @Override
    protected void onDestroy() {
        dismissProgressDialog();
        super.onDestroy();
    }

    private boolean setMetricData() throws JSONException {
        //return true if we set metrics from prefs, false if from server
        //show that we are downloading
        //TODO: maybe GSON to string converter is more efficient and I should consider using it instead
        //set mMetric data either from SharedPreferences (if we have them) or from Server
        if(DEBUG) Log.d(TAG, "setMetricData");
        if(DEBUG) Log.d(TAG, "mMetric id: " + mMetricId);
        String prefsName = PREFS_METRIC.concat(mMetricId);
        mMetricPrefs = getSharedPreferences(prefsName, MODE_PRIVATE);
        if(DEBUG) Log.d(TAG, "metricsPrefs: " + mMetricPrefs.getAll());
        if (mMetricPrefs.contains(PREFS_METRIC_EDITOR_KEY)){ //if we already have it saved
            if(DEBUG) Log.d(TAG, "set data from prefs");
            mMetric =  new JSONObject(mMetricPrefs.getString(PREFS_METRIC_EDITOR_KEY, "nope"));
            if(DEBUG) Log.d(TAG, "mMetric from prefs: " + mMetric.toString());
            return true;
        } else {
            setMetricDataFromServer();
            return false;
        }
    }

    private JSONObject setMetricDataFromServer(){
        if (networkAvailable(true)) {
            loginRequired();
            if(DEBUG) Log.d(TAG, "setMetricDataFromServer");
            SharedPreferences userPrefs = getSharedPreferences(ConnectionUtils.PREFS_USER, MODE_PRIVATE);
            String url = ConnectionUtils.URL_METRICS.concat(mMetricId).concat("/data/"); //TODO: REFACTOR
            final ServerRequestItem getMetricData = new ServerRequestItem(url, userPrefs.getString(ConnectionUtils.PREFS_USER_TOKEN, "token"), null);
            ConnectAndShowDialogTask task = new ConnectAndShowDialogTask(mProgressDialog);
            task.execute(getMetricData);
        }
        return null;
    }

    /**
     * AsyncTask for downloading data of a metric and showing ProgressDialog
     */
    public class ConnectAndShowDialogTask extends AsyncTask<ServerRequestItem, Void, ResponseObject> {
        private static final String TAG = "Connect&ShowDialogTask";
        private ProgressDialog mProgressDialog;
        public ConnectAndShowDialogTask(ProgressDialog mProgressDialog) {
            this.mProgressDialog = mProgressDialog;
        }

        @Override
        protected void onPreExecute() {
            if(DEBUG) Log.d(TAG, "onPreExecute");
            if (mProgressDialog != null){
                mProgressDialog.show();
            }
            super.onPreExecute();
        }

        @Override
        protected ResponseObject doInBackground(ServerRequestItem... params) {
            return Connection.requestToServer(params);
        }

        @Override
        protected void onPostExecute(ResponseObject response) {
            if(DEBUG) Log.d(TAG, "onPostExecute");
            dismissProgressDialog();
            if (response != null) {
                DisplayMetricActivity.this.mMetric = response.getResponse();
            } else if(DEBUG) Log.d(TAG, "response is null!");
            try {
                setMetric();
            } catch (JSONException e) {
                e.printStackTrace();
            }
            super.onPostExecute(response);
        }

        private void dismissProgressDialog() {
            if (isDestroyed()) {
                return;
            }
            DisplayMetricActivity.this.dismissProgressDialog();
        }
    }

    private void dismissProgressDialog() {
        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            mProgressDialog.dismiss();
        }
    }

    /**
     * Prepare composite metrics: adding metric value type and changing metric_type
     * @throws JSONException
     */
    private void prepareDataForCompositeMetrics() throws JSONException {
        if(DEBUG) Log.d(TAG, "prepareDataForCompositeMetrics");
        mMetric.accumulate(METRIC_VALUE_TYPE, METRIC_VALUE_TYPE_INT);
        mMetric.put(METRIC_TYPE, METRIC_TYPE_COMPOSITE_PREPARED);
    }

    /**
     * Prepare raw metrics: for every measurement take value and put into list,
     * which will be JSONArray y_values
     * Put metric type, change metric_value_type and metric_id
     * Put it into
     * @throws JSONException
     */
    private void prepareDataForRawMetrics() throws JSONException {
        if(DEBUG) Log.d(TAG, "prepareDataForRawMetrics");
        JSONArray measurements = mMetric.getJSONArray(RAW_METRICS_MEASUREMENTS);
        ArrayList<String> values = new ArrayList<>();
        for (int i = 0; i < measurements.length(); i++)
            values.add(measurements.getJSONObject(i).getString(RAW_METRIC_VALUE));
        JSONObject row = measurements.getJSONObject(0);
        row.accumulate(Y_VALUES, new JSONArray(values));
        row.put(METRIC_VALUE_TYPE, row.getString(RAW_METRIC_VALUE_TYPE));
        row.put(METRIC_TYPE, METRIC_TYPE_RAW_PREPARED);
        row.put(METRIC_ID, mMetric.getString(RAW_METRIC_ID));
        mMetric = row;
    }

    private void setConstantNamesForRawData() {
        METRIC_NAME = RAW_METRIC_NAME;
        METRIC_VALUE_NAME = RAW_METRIC_VALUE_NAME;
        METRIC_ACTIVITY = RAW_METRIC_ACTIVITY;
        METRIC_FIELD =  RAW_METRIC_FIELD;
    }

    private void setConstantNamesForCompositeData() {
        METRIC_NAME = C_METRIC_NAME;
        METRIC_TYPE = C_METRIC_TYPE;
        INFO = C_METRIC_INFO;
    }

    /**
     * Put current metric data into corresponding preferences (key="metric" + metricId)
     */
    private void storeMetric(){
        SharedPreferences.Editor editor = mMetricPrefs.edit();
        editor.putString(PREFS_METRIC_EDITOR_KEY, mMetric.toString());
        editor.apply();
    }

    /**
     * Check what kind of metric is given and show corresponding charts and information (if any)
     * @throws JSONException
     */
    private void setMetric() throws JSONException {
        if(DEBUG) Log.d(TAG, "setMetric");
        String metricType = mMetric.getString("type");
        if(DEBUG) Log.d(TAG, "metric type: " + metricType);
        setContentView(R.layout.activity_display_metric);
        switch (metricType) {
            case METRIC_TYPE_COMPOSITE:
                prepareDataForCompositeMetrics();
                setConstantNamesForCompositeData();
                setMetricForPreparedCompositeMetric();
                break;
            case METRIC_TYPE_RAW:
                prepareDataForRawMetrics();
                setConstantNamesForRawData();
                setMetricForPreparedRawMetric();
                break;
            case METRIC_TYPE_COMPOSITE_PREPARED:
                setConstantNamesForCompositeData();
                setMetricForPreparedCompositeMetric();
                break;
            case METRIC_TYPE_RAW_PREPARED:
                setConstantNamesForRawData();
                setMetricForPreparedRawMetric();
                break;
        }
        storeMetric();
    }

    private void setMetricForPreparedCompositeMetric() throws JSONException {
        setLineChartForCompositeMetric();
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            setInformationForCompositeMetric();
        }
    }

    /**
     * Set chart for raw data.
     * It could be something with int type - display line chart
     * Or it could be urls, for which PieChart is shown
     * @throws JSONException
     */
    private void setMetricForPreparedRawMetric() throws JSONException {
        if(DEBUG) Log.d(TAG, "prepared data for raw: " + mMetric.toString());
        if(DEBUG) Log.d(TAG, "y_values of prepared data " + mMetric.getString(Y_VALUES));
        String field = mMetric.getString(METRIC_FIELD);
        if(DEBUG) Log.d(TAG, "field " + field);
        switch (field) {
            case METRIC_FIELD_URL:
                if(DEBUG) Log.d(TAG, "mMetric field url");
                setPieChartForRawMetric(numberOfSlices);
                break;
            default:
                String value_type = mMetric.getString(METRIC_VALUE_TYPE);
                if(DEBUG) Log.d(TAG, "value type: " + value_type);
                if (value_type.equals(METRIC_VALUE_TYPE_INT)) {
                    setLineChartForRawMetric();
                    if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
                        setInformationForRawMetric();
                    }
                } else {
                    if(DEBUG) Log.d(TAG, "defaultView, no chart");
                }
        }
    }

    /**
     * @return Map Domain Name : number of encounters (note it doesn't know about duration)
     * Check if data for pie chart is already normalized.
     * If so - convert y_values of mMetric to the map
     * If not - get data from mMetric, extract their domain names (get the map), save to prefs new metrics.
     */
    private Map<String, Integer> getNormalizedDomainNames() throws JSONException {
        if(DEBUG) Log.d(TAG, "getNormalizedDomainNames");
        Map<String, Integer> domainNames;
        if (mMetric.has(PIE_NORMALIZED_FLAG)){
            if(DEBUG) Log.d(TAG, "domain names from prefs");
            domainNames = ApplicationUtils.convertToMap(mMetric.getJSONObject(Y_VALUES));
        } else {
            if(DEBUG) Log.d(TAG, "domain names from server");
            JSONArray jsonArray = mMetric.getJSONArray(Y_VALUES);
            domainNames = ApplicationUtils.getDomainNamesFromURLs(jsonArray);
            JSONObject domainNamesJSON = new JSONObject(domainNames);
            mMetric.put(Y_VALUES, domainNamesJSON);
            mMetric.put(PIE_NORMALIZED_FLAG, "");
            String prefsName = PREFS_METRIC.concat(mMetric.getString(METRIC_ID));
            SharedPreferences prefs = getSharedPreferences(prefsName, MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString(PREFS_METRIC_EDITOR_KEY, mMetric.toString());
            editor.apply();
            if(DEBUG) Log.d(TAG, "domain names" + domainNamesJSON.toString());
        }
        return domainNames;
    }


    /**
     * Set PieChart:
     * Set view -> put as many colors as we can -> put data ->
     * Get legend entries -> Make different legend - RecyclerView with domain name and color:
     * @see LegendAdapter
     * @param n - number of slices in the chart. Can be more than number of actual data entries
     */
    private void setPieChartForRawMetric(int n) {
        if(DEBUG) Log.d(TAG, "setPieChartForRawMetric");
        setContentView(R.layout.display_metric_pie_chart);
        try {
            PieChart chart = findViewById(R.id.pie_chart);
            List<PieEntry> entries = ApplicationUtils.getPieChartData(getNormalizedDomainNames(), n);
            PieDataSet dataSet = new PieDataSet(entries, "websites");
            dataSet.setDrawIcons(true);
            dataSet.setDrawValues(true);
            dataSet.setValueTextSize(11f);
            dataSet.setSliceSpace(3f);
            dataSet.setIconsOffset(new MPPointF(0, 40));
            dataSet.setSelectionShift(5f);
            ArrayList<Integer> colors = new ArrayList<>();
            //just add lots of colors
            for (int c : ColorTemplate.COLORFUL_COLORS)
                colors.add(c);
            for (int c : ColorTemplate.LIBERTY_COLORS)
                colors.add(c);
            for (int c : ColorTemplate.VORDIPLOM_COLORS)
                colors.add(c);
            for (int c : ColorTemplate.JOYFUL_COLORS)
                colors.add(c);
            for (int c : ColorTemplate.PASTEL_COLORS)
                colors.add(c);
            colors.add(ColorTemplate.getHoloBlue());
            dataSet.setColors(colors);

            PieData data = new PieData(dataSet);
            data.setValueFormatter(new PieChartValueFormatter());
            chart.highlightValues(null);
            chart.setEntryLabelColor(Color.BLUE);
            if(DEBUG) Log.d(TAG, "PieDataSet size " + entries.size());
            chart.setData(data);
            chart.setDrawEntryLabels(false);
            chart.getDescription().setEnabled(false);
            Legend l = chart.getLegend();
            l.setEnabled(false);
            LegendEntry[] legendEntries = l.getEntries();
            mLegendItems = new ArrayList<>();
            for (int i = 0; i < legendEntries.length - 1; i++) {
                mLegendItems.add(new LegendItem(legendEntries[i].formColor, legendEntries[i].label));
            }
            setPieChartLegend();
            chart.setUsePercentValues(true);
            chart.setExtraBottomOffset(20f);
            chart.invalidate();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /**
     * Find RecyclerView
     * set Adapter with data:
     * @see LegendAdapter
     */
    private void setPieChartLegend(){
        if(DEBUG) Log.d(TAG, "setPieChartLegend");
        mMetricLegendRecyclerView = findViewById(R.id.pie_chart_recycler_view);
        mMetricLegendRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        LegendAdapter adapter = new LegendAdapter(this, mLegendItems);
        mMetricLegendRecyclerView.setAdapter(adapter);
    }

    /**
     * Prepares information for raw metric and invokes setting RecyclerView
     * @see DisplayMetricActivity#setInformation(HashMap)
     * @throws JSONException
     */
    private void setInformationForRawMetric() throws JSONException {
        if(DEBUG) Log.d(TAG, "setInformationForRawMetric");
        mMetricDescriptionListView = findViewById(R.id.metric_description_list_view);
        if(DEBUG) Log.d(TAG, "listView found");
        HashMap<String, String> details = new HashMap<>();
        details.put("Metric name", mMetric.getString(METRIC_NAME));
        details.put("Activity", mMetric.getString(METRIC_ACTIVITY));
        setInformation(details);
    }

    /**
     * Prepares information for composite metric and invokes setting RecyclerView
     * @see DisplayMetricActivity#setInformation(HashMap)
     * @throws JSONException
     */
    private void setInformationForCompositeMetric() throws JSONException {
        mMetricDescriptionListView = findViewById(R.id.metric_description_list_view);
        HashMap<String, String> details = new HashMap<>();
        details.put("Metric name", mMetric.getString(METRIC_NAME));
        //TODO: more information here
        setInformation(details);
    }

    /**
     * Puts pairs of metric description into ListView using SimpleAdapter
     * @param details - metric description
     */
    private void setInformation(HashMap<String, String> details) {
        if(DEBUG) Log.d(TAG, "setInformation");
        mMetricDescriptionListView = findViewById(R.id.metric_description_list_view);
        List<Map<String, String>> list = new ArrayList<>();
        Map<String, String> map;
        for (Map.Entry<String, String> entry : details.entrySet()) {
            map = new HashMap<>();
            map.put("property_label", entry.getKey());
            map.put("property_information", entry.getValue());
            list.add(map);
        }
        mAdapter = new SimpleAdapter(this, list, R.layout.metric_description_item, new String[] { "property_label", "property_information" }, new int[] {R.id.metric_property_label, R.id.metric_property_information});
        mMetricDescriptionListView.setAdapter(mAdapter);
    }

    /**
     * Prepares yValues to display LineChart
     * @throws JSONException
     */
    private void setLineChartForRawMetric() throws JSONException {
        if(DEBUG) Log.d(TAG, "line chart raw mMetric");
        setContentView(R.layout.activity_display_metric);
        String label = mMetric.getString(METRIC_NAME);
        List<Float> yValues = ApplicationUtils.floatArrayListFromJSONArray(mMetric.getJSONArray(Y_VALUES));
        setLineChart(null, yValues, label);
    }

    /**
     * Prepares xValues and yValues to display LineChart
     * @throws JSONException
     */
    private void setLineChartForCompositeMetric() throws JSONException {
        if(DEBUG) Log.d(TAG, "line chart composite mMetric");
        String label = mMetric.getString(METRIC_NAME);
        List<Float> xValues = ApplicationUtils.floatArrayListFromJSONArray(mMetric.getJSONArray(X_VALUES));
        List<Float> yValues = ApplicationUtils.floatArrayListFromJSONArray(mMetric.getJSONArray(Y_VALUES));
        setLineChart(xValues, yValues, label);
    }

    /**
     * Set LineChart
     * @param x_values generated if null
     * @param y_values required
     * @param label Name of a metric
     */
    private void setLineChart(List<Float> x_values, List<Float> y_values, String label){
        LineDataSet dataSet = getLineData(x_values, y_values, label);
        assert dataSet != null : "DataSet can't be null";
        dataSet.setDrawFilled(true);
        dataSet.setFillColor(Color.BLUE);
        dataSet.setColor(R.color.colorPrimary);
        dataSet.setHighlightEnabled(true);
        dataSet.setDrawHighlightIndicators(true);
        dataSet.setHighLightColor(Color.RED);
        LineData data = new LineData(dataSet);
        LineChart chart = findViewById(R.id.line_chart);
        chart.setData(data);
        chart.invalidate();
    }

    //x_values can be null
    //label can be null

    /**
     *
     * @param xValues x axis data, generated if null
     * @param yValues y axis data, required4
     * @param label
     * @return LineDataSet from given data
     * LineDataSet needs to be filled list of Entry objects
     */
    public static LineDataSet getLineData(List<Float> xValues, List<Float> yValues, String label){
        if(DEBUG) Log.d(TAG, "getLineData");
        if (yValues == null) return null;
        if (label == null) label = "";
        if (xValues == null){
            xValues = new ArrayList<>();
            for (int i = 0; i < yValues.size(); i++)
                xValues.add((float) i);
        }
        List<Entry> entries = new ArrayList<>();
        int valuesLength = Math.min(xValues.size(), yValues.size());
        for (int i = 0; i < valuesLength; i++)
            entries.add(new Entry(xValues.get(i), yValues.get(i)));
        if(DEBUG) Log.d(TAG, "entries size: " + entries.size());
        return new LineDataSet(entries, label);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_action_bar_menu, menu);
        return true;
    }

    private void sync(){
        if(DEBUG) Log.d(TAG, "SYNCING..");
        setMetricDataFromServer();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case R.id.action_sync:
                sync();
                return true;
            default: return super.onOptionsItemSelected(item);
        }
    }
}
