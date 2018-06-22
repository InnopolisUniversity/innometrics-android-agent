package com.example.innometrics.adapters;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.example.innometrics.DisplayMetricActivity;
import com.example.innometrics.R;
import com.example.innometrics.utils.ConnectionUtils;

import org.json.JSONException;
import java.util.ArrayList;


/**
 * Adapter for RecyclerView in MetricsFragment for metrics
 * Contains array of MetricItems:
 * @see MetricItem
 */
public class MetricsAdapter extends RecyclerView.Adapter<MetricsAdapter.MetricViewHolder> {
    private Context mContext;
    private ArrayList<MetricItem> metricItems;
    /**
     * @param mExpandedPosition is used to compare type of position of a ViewHolder in RecyclerView
     *                          @see com.example.innometrics.adapters.MetricsAdapter#onBindViewHolder(MetricViewHolder, int)
     */
    private int mExpandedPosition = -1;


    public MetricsAdapter(Context mContext, ArrayList<MetricItem> metricItems) {
        this.mContext = mContext;
        this.metricItems = metricItems;
    }

    @NonNull
    @Override
    public MetricViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View holder = LayoutInflater.from(mContext).inflate(R.layout.metric_item, parent, false);
        return new MetricViewHolder(holder);
    }

    @Override
    public void onBindViewHolder(@NonNull final MetricViewHolder holder, final int position) {
        final MetricItem current = metricItems.get(position);
        holder.mName.setText(current.getName());
        holder.mId.setText(Integer.toString(current.getId()));
        if(current.getType().equals("R")){
            holder.mType.setText(mContext.getResources().getString(R.string.metric_type_raw_str));
        } else if (current.getType().equals("C")){
            holder.mType.setText(mContext.getResources().getString(R.string.metric_type_composite_str));
        } else {
            holder.mType.setText(current.getType());
        }
        final boolean isExpanded = position == mExpandedPosition;
        holder.mMetricInfo.setVisibility(isExpanded?View.VISIBLE:View.GONE);
        try {
            holder.mTextViewDetails.setText(current.getInfo().toString(4));
        } catch (JSONException e) {
            e.printStackTrace();
        }
        holder.itemView.setActivated(isExpanded);
        holder.arrow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //check if a ViewHolder is expanded
                if (isExpanded){
                    mExpandedPosition = - 1;
                } else {
                    mExpandedPosition = position;
                }
                notifyItemChanged(position);
            }
        });

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(mContext, DisplayMetricActivity.class);
                intent.putExtra(ConnectionUtils.METRICS_METRIC_ID, current.getId());
                mContext.startActivity(intent);
            }
        });

    }

    @Override
    public int getItemCount() {
        return metricItems.size();
    }

    public class MetricViewHolder extends RecyclerView.ViewHolder {
        public ImageView arrow;
        public TextView mId;
        public TextView mType;
        public TextView mName;
        public RelativeLayout mMetricInfo;
        public TextView mTextViewDetails;

        public MetricViewHolder(View itemView) {
            super(itemView);
            arrow = itemView.findViewById(R.id.metric_details_arrow);
            mId = itemView.findViewById(R.id.metric_id);
            mType = itemView.findViewById(R.id.metric_type);
            mName = itemView.findViewById(R.id.metric_name);
            mMetricInfo = itemView.findViewById(R.id.metric_info);
            mTextViewDetails = itemView.findViewById(R.id.text_view_details);
        }
    }
}
