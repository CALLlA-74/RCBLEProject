package com.example.rcbleproject.Database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

public class DatabaseAdapter {

    protected SQLiteDatabase database;
    protected DatabaseHelper dbHelper;

    public DatabaseAdapter(Context context) {
        dbHelper = new DatabaseHelper(context);
    }

    public void open(){
        database = dbHelper.getWritableDatabase();
    }

    public void close(){
        database.close();
    }
}
