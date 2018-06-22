package com.example.innometrics.local_data;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Delete;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;

import java.util.List;

@Dao
public interface LocationItemDao {
    @Query("SELECT * FROM LocationItem")
    List<LocationItem> getAll();

    @Insert
    void insertAll(LocationItem... locationItems);

    @Delete
    void delete(LocationItem locationItem);

    @Query("DELETE FROM LocationItem")
    public void clear();
}