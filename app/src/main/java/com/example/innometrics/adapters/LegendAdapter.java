package com.example.innometrics.adapters;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.example.innometrics.R;

import java.util.ArrayList;

/**
 * Adapter for PieChart legend (RecyclerView)
 * Contains array of LegendItems:
 * @see LegendItem
 */
public class LegendAdapter extends RecyclerView.Adapter<LegendAdapter.LegendViewHolder>{
    private Context mContext;
    private ArrayList<LegendItem> legendItems;


    public LegendAdapter(Context mContext, ArrayList<LegendItem> legendItems) {
        this.mContext = mContext;
        this.legendItems = legendItems;
    }

    @NonNull
    @Override
    public LegendAdapter.LegendViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View holder = LayoutInflater.from(mContext).inflate(R.layout.pie_chart_legend_item, parent, false);
        return new LegendAdapter.LegendViewHolder(holder);
    }

    @Override
    public void onBindViewHolder(@NonNull final LegendAdapter.LegendViewHolder holder, final int position) {
        final LegendItem current = legendItems.get(position);
        holder.domain.setText(current.getText());
        holder.colorHolder.setBackgroundColor(current.getColor());
    }

    @Override
    public int getItemCount() {
        return legendItems.size();
    }


    public class LegendViewHolder extends RecyclerView.ViewHolder {
        public TextView domain;
        public View colorHolder;

        public LegendViewHolder(View itemView) {
            super(itemView);
            domain = itemView.findViewById(R.id.pie_metric_domain_name);
            colorHolder = itemView.findViewById(R.id.pie_metric_domain_color);
        }
    }
}
