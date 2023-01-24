package com.example.rcbleproject.Database;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.example.rcbleproject.ProfileControl;

import java.security.PublicKey;
import java.util.ArrayList;

public class DatabaseAdapterProfilesControl extends DatabaseAdapter{
    public static final String TABLE_NAME = "profiles_control";
    public static final String ID = "_id";
    public static final String PROFILE_NAME = "profile_name";
    public static final String NUMBER_OF_SCREENS = "number_of_screens";
    public static final String GRID_ALIGNMENT = "grid_alignment";   /* 0 - сетка скрыта, выравнивания нет
                                                                     1 - сетка показана, элементы выравнены по узлам*/

    public DatabaseAdapterProfilesControl(Context context){
        super(context);
        open();
    }

    public static void createTable(SQLiteDatabase db){
        db.execSQL("CREATE TABLE " + TABLE_NAME
                    + " (" + ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + PROFILE_NAME + " TEXT NOT NULL CHECK(" + PROFILE_NAME +" != ''), "
                    + NUMBER_OF_SCREENS + " INTEGER DEFAULT 1 CHECK(" + NUMBER_OF_SCREENS + " > 0 "
                        + "AND + " + NUMBER_OF_SCREENS + " <= 5), "
                    + GRID_ALIGNMENT + " INTEGER DEFAULT 1 CHECK("
                        + GRID_ALIGNMENT + " = 0 OR " + GRID_ALIGNMENT + " = 1));");

        /*db.execSQL("INSERT INTO " + TABLE_NAME +
                   " (" + PROFILE_NAME + ") VALUES ('Example');");
        db.execSQL("INSERT INTO " + TABLE_NAME +
                " (" + PROFILE_NAME + ") VALUES ('Geckos Control');");*/
    }

    public String[] getColumns(){
        return new String[]{ID, PROFILE_NAME, NUMBER_OF_SCREENS, GRID_ALIGNMENT};
    }

    public long insert(String name){
        ContentValues contentValues = new ContentValues();
        contentValues.put(PROFILE_NAME, name);
        return database.insert(TABLE_NAME, null, contentValues);
    }

    public long delete(long profileId){
        return database.delete(TABLE_NAME,
                    ID + " = ?",
                               new String[]{String.valueOf(profileId)});
    }

    public Cursor getProfile_cursor(long id){
        return database.query(TABLE_NAME, new String[]{ID, PROFILE_NAME}, ID + " = " + id,
                null, null, null, null);
    }

    public ProfileControl getProfile(long id){
        Cursor c = getProfile_cursor(id);
        if (!c.moveToFirst()) {
            Log.v("APP_TAG", "cursor is empty");
            return null;
        }
        ProfileControl profileControl = new ProfileControl(c.getLong(c.getColumnIndexOrThrow(ID)));
        profileControl.setName(c.getString(c.getColumnIndexOrThrow(PROFILE_NAME)));
        return profileControl;
    }

    public int getNumOfScreens(long profileId){
        Cursor cursor = database.query(TABLE_NAME, new String[]{NUMBER_OF_SCREENS},
                ID + " = " + profileId, null, null, null, null);
        if (!cursor.moveToFirst()) return 0;
        return cursor.getInt(cursor.getColumnIndexOrThrow(NUMBER_OF_SCREENS));
    }

    public boolean getProfileGridAlignment(long profileId){
        Cursor cursor = database.query(TABLE_NAME, new String[] {GRID_ALIGNMENT},
                ID + " = " + profileId, null, null, null, null);
        if (!cursor.moveToFirst()) return true;
        return cursor.getInt(cursor.getColumnIndexOrThrow(GRID_ALIGNMENT)) != 0;
    }

    public int updateProfileName(long profileId, String name){
        ContentValues contentValues = new ContentValues();
        contentValues.put(PROFILE_NAME, name);
        return database.update(TABLE_NAME, contentValues, ID + " = " + profileId, null);
    }

    public int updateProfileGridAlignment(long profileId, boolean gridVisibility){
        ContentValues contentValues = new ContentValues();
        contentValues.put(GRID_ALIGNMENT, gridVisibility);
        return database.update(TABLE_NAME, contentValues, ID + " = " + profileId, null);
    }

    public int updateProfileNumOfDisplays(long profileId, int numOfDisplays){
        ContentValues contentValues = new ContentValues();
        contentValues.put(NUMBER_OF_SCREENS, numOfDisplays);
        return database.update(TABLE_NAME, contentValues, ID + " = " + profileId, null);
    }

    public ArrayList<ProfileControl> getProfiles(boolean isReverse){
        ArrayList<ProfileControl> list = new ArrayList<>();
        Cursor cursor = getAllRows(isReverse);
        while (cursor.moveToNext()){
            int idx = cursor.getColumnIndex(ID);
            if (idx < 0) return null;
            ProfileControl profileControl = new ProfileControl(cursor.getLong(idx));
            idx = cursor.getColumnIndex(PROFILE_NAME);
            if (idx < 0) return null;
            profileControl.setName(cursor.getString(idx));
            list.add(profileControl);
        }
        cursor.close();
        return list;
    }

    public Cursor getProfiles_cursor(boolean isReverse){
        return getAllRows(isReverse);
    }

    private Cursor getAllRows(boolean isReverse){
        if (!isReverse)
            return database.query(TABLE_NAME, getColumns(), null,
                              null, null, null, null);
        return database.query(TABLE_NAME, getColumns(), null,
                null, null, null, ID + " DESC");
    }
}
