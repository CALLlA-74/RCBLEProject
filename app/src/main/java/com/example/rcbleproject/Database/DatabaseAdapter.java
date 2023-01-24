package com.example.rcbleproject.Database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

public class DatabaseAdapter {

    protected SQLiteDatabase database;
    protected DatabaseHelper dbHelper;

    public DatabaseAdapter(Context context) {
        dbHelper = new DatabaseHelper(context.getApplicationContext());
    }

    public void open(){
        if (database != null && database.isOpen()) return;
        database = dbHelper.getWritableDatabase();
    }

    public void close(){
        database.close();
    }
}
