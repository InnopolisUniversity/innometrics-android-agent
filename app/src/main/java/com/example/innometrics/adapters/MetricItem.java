package com.example.innometrics.adapters;

import org.json.JSONObject;

public class MetricItem {
    private int id;
    private String name;
    private String type;
    private JSONObject info;

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public JSONObject getInfo() {
        return info;
    }

    public MetricItem(int id, String name, String type, JSONObject info) {

        this.id = id;
        this.name = name;
        this.type = type;
        this.info = info;
    }
}
