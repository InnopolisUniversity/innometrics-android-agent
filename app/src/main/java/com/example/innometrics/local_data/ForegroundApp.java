package com.example.innometrics.local_data;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;

@Entity
public class ForegroundApp {
//    @PrimaryKey(autoGenerate = true)
//    private int id;

    @ColumnInfo(name = "foreground_application_name")
    private String foregroundApplicationName;

    @PrimaryKey
    @ColumnInfo(name = "starting_time")
    private long startingTime;


//    public void setId(int id) {
//        this.id = id;
//    }
//
//    public int getId() {
//
//        return id;
//    }

    public void setForegroundApplicationName(String foregroundApplicationName) {
        this.foregroundApplicationName = foregroundApplicationName;
    }

    public void setStartingTime(long startingTime) {
        this.startingTime = startingTime;
    }

    public String getForegroundApplicationName() {

        return foregroundApplicationName;
    }

    public long getStartingTime() {
        return startingTime;
    }
}
