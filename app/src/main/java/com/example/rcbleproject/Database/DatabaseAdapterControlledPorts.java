package com.example.rcbleproject.Database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

public class DatabaseAdapterControlledPorts extends DatabaseAdapter{
    public static final String TABLE_NAME = "controlled_ports";
    public static final String ELEMENT_ID = "element_id";
    public static final String AXIS_NUM = "axis_num";
    public static final String DEVICE_ADDRESS = "device_address";
    public static final String DEVICE_PORT_NUM = "device_port_num";
    public static final String DISPLAY_ID = "display_id";

    public DatabaseAdapterControlledPorts(Context context){ super(context); }

    public static void createTable(SQLiteDatabase db){
        db.execSQL("CREATE TABLE " + TABLE_NAME + " ("
                + ELEMENT_ID + " INTEGER NOT NULL, "
                + AXIS_NUM + " INTEGER NOT NULL CHECK(" + AXIS_NUM + " >= 0 AND " + AXIS_NUM + " < 2), "
                + DEVICE_ADDRESS + " TEXT NOT NULL, "
                + DEVICE_PORT_NUM + " INTEGER NOT NULL CHECK(" + DEVICE_PORT_NUM + " >= 0 AND "
                    + DEVICE_PORT_NUM + " < 4), "
                + DISPLAY_ID + " INTEGER NOT NULL, "
                + "FOREIGN KEY (" + ELEMENT_ID + ") REFERENCES " + DatabaseAdapterElementsControl.TABLE_NAME
                    + "(" + DatabaseAdapterElementsControl.ID + ") ON DELETE CASCADE, "
                + "FOREIGN KEY (" + DEVICE_ADDRESS + ") REFERENCES " + DatabaseAdapterForDevices.TABLE_NAME
                    + "(" + DatabaseAdapterForDevices.DEVICE_ADDRESS + ") ON DELETE CASCADE, "
                + "FOREIGN KEY (" + DISPLAY_ID + ") REFERENCES " + DatabaseAdapterDisplays.TABLE_NAME
                    + "(" + DatabaseAdapterDisplays.ID + ") ON DELETE CASCADE, "
                + "PRIMARY KEY (" + DEVICE_ADDRESS + ", " + DISPLAY_ID + ", " + DEVICE_PORT_NUM + "));");
    }


}
