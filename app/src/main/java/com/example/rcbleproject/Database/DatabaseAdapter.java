package com.example.rcbleproject.Database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

public class DatabaseAdapter {

    protected SQLiteDatabase database;
    protected DatabaseHelper dbHelper;
    protected Context context;

    public DatabaseAdapter(Context context) {
        dbHelper = new DatabaseHelper(context.getApplicationContext());
        this.context = context;
    }

    public void open(){
        if (database != null && database.isOpen()) return;
        database = dbHelper.getWritableDatabase();
    }

    public void close(){
        database.close();
    }
}
