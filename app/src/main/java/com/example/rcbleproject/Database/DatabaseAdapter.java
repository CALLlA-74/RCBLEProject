package com.example.rcbleproject.Database;

import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;

import com.example.rcbleproject.BaseAppActivity;

public class DatabaseAdapter {

    protected SQLiteDatabase database;
    protected DatabaseHelper dbHelper;
    protected BaseAppActivity context;

    public DatabaseAdapter(BaseAppActivity context) {
        dbHelper = new DatabaseHelper(context.getApplicationContext());
        this.context = context;
    }

    public void open(){
        if (database != null && database.isOpen()) return;
        database = dbHelper.getWritableDatabase();
        database.execSQL("PRAGMA foreign_keys=ON");
    }

    public void close(){
        database.close();
    }

    protected abstract class DoingInBackAsync<Params, Progress, Result> extends AsyncTask<Params, Progress, Result>{
        protected final BaseAppActivity activity;

        public DoingInBackAsync(BaseAppActivity activity){ this.activity = activity; }

        @Override
        protected void onPostExecute(Result result){
            if (activity == null) return;
            activity.notifyDataSetChanged();
        }
    }
}
