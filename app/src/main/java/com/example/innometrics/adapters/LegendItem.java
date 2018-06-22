package com.example.innometrics.adapters;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.View;

public class LegendItem{
    private int color;
    private String text;

    public void setColor(int color) {
        this.color = color;
    }

    public void setText(String text) {
        this.text = text;
    }

    public int getColor() {

        return color;
    }

    public String getText() {
        return text;
    }

    public LegendItem(int color, String text) {
        this.color = color;
        this.text = text;
    }
}
