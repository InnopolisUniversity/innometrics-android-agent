package com.example.innometrics.local_data;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Delete;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;

import java.util.List;

@Dao
public interface ForegroundAppDao {
    @Query("SELECT * FROM foregroundapp")
    List<ForegroundApp> getAll();

    @Insert
    void insertAll(ForegroundApp... foregroundApps);

    @Delete
    void delete(ForegroundApp foregroundApp);

    @Query("SELECT * FROM foregroundapp WHERE starting_time = :time")
    ForegroundApp getById(long time);

    @Query("DELETE FROM foregroundapp")
    public void clear();
}