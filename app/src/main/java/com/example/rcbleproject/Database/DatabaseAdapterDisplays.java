package com.example.rcbleproject.Database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import androidx.annotation.NonNull;

import com.example.rcbleproject.BaseAppActivity;
import com.example.rcbleproject.Container;
import com.example.rcbleproject.R;

import java.util.ArrayList;

public class DatabaseAdapterDisplays extends DatabaseAdapter{
    public static final String TABLE_NAME = "displays";
    public static final String ID = "_id";
    public static final String DISPLAY_INDEX = "display_index";
    public static final String PROFILE_ID = "profile_id";

    public DatabaseAdapterDisplays(BaseAppActivity context) {
        super(context);
        open();
    }

    public static void createTable(SQLiteDatabase db, @NonNull Context context){
        int maxNumOfDisplays = context.getResources().getInteger(R.integer.maxNumOfDisplays);
        db.execSQL("CREATE TABLE " + TABLE_NAME
                + " (" + ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + DISPLAY_INDEX + " INTEGER NOT NULL CHECK(" + DISPLAY_INDEX + " >= 0 AND "
                    + DISPLAY_INDEX + " < " + maxNumOfDisplays + "), "
                + PROFILE_ID + " INTEGER NOT NULL, "
                + "FOREIGN KEY (" + PROFILE_ID +") REFERENCES "
                + DatabaseAdapterProfilesControl.TABLE_NAME + "("
                + DatabaseAdapterProfilesControl.ID + ") ON DELETE CASCADE, "
                + "UNIQUE(" + DISPLAY_INDEX + ", " + PROFILE_ID + "));");
    }

    public ArrayList<Long> getDisplaysByProfileID(long profileID){
        Cursor cursor = getDisplaysByProfileID_cursor(profileID);
        ArrayList<Long> res = new ArrayList<>();
        int idIdx = cursor.getColumnIndexOrThrow(ID);
        while (cursor.moveToNext()) res.add(cursor.getLong(idIdx));
        return res;
    }

    private Cursor getDisplaysByProfileID_cursor(long profileID){
        return database.query(TABLE_NAME, getAllCols(), PROFILE_ID + " = " + profileID,
                null, null, null, DISPLAY_INDEX + " ASC");
    }

    public long insert(long profileID, int displayIndex){
        ContentValues contentValues = new ContentValues();
        contentValues.put(PROFILE_ID, profileID);
        contentValues.put(DISPLAY_INDEX, displayIndex);
        long id = database.insert(TABLE_NAME, null, contentValues);
        Container.getDbElementsControl(context).insertUnknownTypeElement(id);
        return id;
    }

    public void updateIndexByID(long displayID, int displayIndex){
        ContentValues contentValues = new ContentValues();
        contentValues.put(DISPLAY_INDEX, displayIndex);
        database.update(TABLE_NAME, contentValues, ID + " = " + displayID, null);
    }

    public int deleteDisplayByID(Long displayID){
        if (displayID == null) return 0;
        return database.delete(TABLE_NAME, ID + " = " + displayID, null);
    }

    public String[] getAllCols(){ return new String[]{ID, DISPLAY_INDEX, PROFILE_ID}; }
}
