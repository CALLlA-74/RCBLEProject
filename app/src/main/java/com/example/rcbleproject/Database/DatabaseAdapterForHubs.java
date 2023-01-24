package com.example.rcbleproject.Database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.rcbleproject.BluetoothHub;

public class DatabaseAdapterForHubs extends DatabaseAdapter{
    public static final String TABLE_NAME = "hubs";
    public static final String ID = "_id";
    public static final String HUB_NAME = "hub_name";
    public static final String HUB_ADDRESS = "hub_address";
    public static final String HUB_TYPE = "hub_type";
    public static final String HUB_STATE_CONNECTION = "hub_state_connection"; /* статус соединения:
                                                                                           1 - соединено;
                                                                                           0 - соединение разорвано*/

    public DatabaseAdapterForHubs(Context context){
        super(context);
        open();
    }

    public static void createTable(SQLiteDatabase db){
        db.execSQL("CREATE TABLE " + TABLE_NAME
                + " (" + ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + HUB_NAME + " TEXT NOT NULL CHECK (" + HUB_NAME + " != ''), "
                + HUB_ADDRESS + " TEXT NOT NULL UNIQUE CHECK(" + HUB_ADDRESS + " != ''), "
                + HUB_STATE_CONNECTION + " INTEGER NOT NULL CHECK (" + HUB_STATE_CONNECTION
                     + " = 0 OR " + HUB_STATE_CONNECTION + " = 1), "
                + HUB_TYPE + " INTEGER NOT NULL);");
    }

    public String[] getColumns(){
        return new String[]{ID, HUB_NAME, HUB_ADDRESS, HUB_TYPE, HUB_STATE_CONNECTION};
    }

    public Cursor getConnectedHubs_cursor(){
        return database.query(TABLE_NAME, getColumns(), HUB_STATE_CONNECTION + " = 1",
                null, null, null, null);
    }

    /*public ArrayList<Hub> getConnectedDevices(BluetoothAdapter bluetoothAdapter){
        if (!database.isOpen()) database = dbHelper.getWritableDatabase();
        Cursor cursor = getConnectedDevices_cursor();
        ArrayList<Hub> devices = new ArrayList<>();

        while (cursor.moveToNext()){
            String address = cursor.getString(cursor.getColumnIndexOrThrow(HUB_ADDRESS));
            final BluetoothDevice device = bluetoothAdapter.getRemoteDevice(address);
            Hub deviceApp = new Hub(device);
        }
        return devices;
    }*/

    public long insert(String name, String address, int state, BluetoothHub.HubTypes hubType){
        ContentValues contentValues = new ContentValues();
        contentValues.put(HUB_NAME, name);
        contentValues.put(HUB_ADDRESS, address);
        contentValues.put(HUB_STATE_CONNECTION, state);
        contentValues.put(HUB_TYPE, hubType.ordinal());
        return database.insert(TABLE_NAME, null, contentValues);
    }

    public void updateNameById(long hubId, String name){
        ContentValues contentValues = new ContentValues();
        contentValues.put(HUB_NAME, name);
        database.update(TABLE_NAME, contentValues, ID + " = " + hubId, null);
    }

    public void updateNameByAddress(String hubAddress, String name){
        ContentValues contentValues = new ContentValues();
        contentValues.put(HUB_NAME, name);
        database.update(TABLE_NAME, contentValues, HUB_ADDRESS + " = '" + hubAddress + "'", null);
    }

    public void updateNameAndState(long hubId, String newName, int state){
        ContentValues contentValues = new ContentValues();
        contentValues.put(HUB_NAME, newName);
        contentValues.put(HUB_STATE_CONNECTION, state);
        database.update(TABLE_NAME, contentValues, ID + " = " + hubId, null);
    }

    public long getHubStateConnection(String hubAddress){
        Cursor c = database.query(TABLE_NAME, new String[]{HUB_STATE_CONNECTION},
                HUB_ADDRESS + " = '" + hubAddress + "'", null, null,
                null, null);
        try {
            if (c == null || !c.moveToFirst()) return -1;
        } catch (Exception e) {return -1;}
        return c.getLong(c.getColumnIndexOrThrow(HUB_STATE_CONNECTION));
    }

    public Cursor getHubById_cursor(long id){
        return database.query(TABLE_NAME, getColumns(), ID + " = " + id, null,
                null, null, null);
    }

    public Cursor getHubByAddress_cursor(String hubAddress){
        return database.query(TABLE_NAME, getColumns(), HUB_ADDRESS + " = '" +
                hubAddress + "'", null, null, null, null);
    }
}
