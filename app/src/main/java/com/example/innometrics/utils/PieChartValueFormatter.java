package com.example.innometrics.utils;

import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.formatter.IValueFormatter;
import com.github.mikephil.charting.utils.ViewPortHandler;

import java.text.DecimalFormat;

/**
 * Class that adds "%" when displaying PieChart metric (urls)
 */
public class PieChartValueFormatter implements IValueFormatter{

    private DecimalFormat mFormat;

    public PieChartValueFormatter () {
        mFormat = new DecimalFormat("###,###,##0.0"); // use one decimal
    }

    @Override
    public String getFormattedValue(float value, Entry entry, int dataSetIndex, ViewPortHandler viewPortHandler) {
        return mFormat.format(value).concat("%");
    }
}
