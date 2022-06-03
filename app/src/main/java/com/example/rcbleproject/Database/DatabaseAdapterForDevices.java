package com.example.rcbleproject.Database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

public class DatabaseAdapterForDevices extends DatabaseAdapter{
    public static final String TABLE_NAME = "devices";
    public static final String ID = "_id";
    public static final String DEVICE_NAME = "device_name";
    public static final String DEVICE_ADDRESS = "device_address";
    public static final String DEVICE_STATE_CONNECTION = "device_state_connection"; /* статус соединения:
                                                                                           1 - соединено;
                                                                                           0 - соединение разорвано*/

    public DatabaseAdapterForDevices(Context context){ super(context);}

    public static void createTable(SQLiteDatabase db){
        db.execSQL("CREATE TABLE " + TABLE_NAME
                + " (" + ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + DEVICE_NAME + " TEXT NOT NULL CHECK (" + DEVICE_NAME + " != ''), "
                + DEVICE_ADDRESS + " TEXT NOT NULL UNIQUE CHECK(" + DEVICE_ADDRESS + " != ''), "
                + DEVICE_STATE_CONNECTION + " INTEGER NOT NULL CHECK (" + DEVICE_STATE_CONNECTION
                     + " = 0 OR " + DEVICE_STATE_CONNECTION + " = 1));");
    }

    public String[] getColumns(){
        return new String[]{ID, DEVICE_NAME, DEVICE_ADDRESS, DEVICE_STATE_CONNECTION};
    }

    public Cursor getConnectedDevices_cursor(){
        return database.query(TABLE_NAME, getColumns(), DEVICE_STATE_CONNECTION + " = 1",
                null, null, null, null);
    }

    public long insert(String name, String address, int state){
        ContentValues contentValues = new ContentValues();
        contentValues.put(DEVICE_NAME, name);
        contentValues.put(DEVICE_ADDRESS, address);
        contentValues.put(DEVICE_STATE_CONNECTION, state);
        return database.insert(TABLE_NAME, null, contentValues);
    }

    public long delete(long profileId){
        return database.delete(TABLE_NAME,
                ID + " = " + profileId, null);
    }

    public void updateName(long deviceId, String name){
        ContentValues contentValues = new ContentValues();
        contentValues.put(DEVICE_NAME, name);
        database.update(TABLE_NAME, contentValues, ID + " = " + deviceId, null);
    }

    public void updateState(long deviceId, int state){
        ContentValues contentValues = new ContentValues();
        contentValues.put(DEVICE_STATE_CONNECTION, state);
        database.update(TABLE_NAME, contentValues, ID + " = " + deviceId, null);
    }

    public long getDeviceStateConnection(String deviceAddress){
        Cursor c = database.query(TABLE_NAME, new String[]{DEVICE_STATE_CONNECTION},
                DEVICE_ADDRESS + " = '" + deviceAddress + "'", null, null,
                null, null);
        if (!c.moveToFirst()) return -1;
        return c.getLong(c.getColumnIndexOrThrow(DEVICE_STATE_CONNECTION));
    }

    public Cursor getDeviceById_cursor(long id){
        return database.query(TABLE_NAME, getColumns(), ID + " = " + id, null,
                null, null, null);
    }

    public Cursor getDeviceByAddress_cursor(String deviceAddress){
        return database.query(TABLE_NAME, getColumns(), DEVICE_ADDRESS + " = '" +
                        deviceAddress + "'", null, null, null, null);
    }
}
