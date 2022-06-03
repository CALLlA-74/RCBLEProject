package com.example.rcbleproject.Database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "RCBLE.db"; // название бд
    private static final int ActualVersion = 1; // версия базы данных

    public DatabaseHelper(Context context){
        super(context, DATABASE_NAME, null, ActualVersion);
    }

    @Override
    public void onCreate(SQLiteDatabase db){
        DatabaseAdapterProfilesControl.createTable(db);
        DatabaseAdapterForDevices.createTable(db);
        DatabaseAdapterDisplays.createTable(db);
        DatabaseAdapterElementsControl.createTable(db);
        DatabaseAdapterControlledPorts.createTable(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion){}
}
