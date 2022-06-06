package com.example.rcbleproject.Database;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.rcbleproject.ControlledPort;

import java.util.ArrayList;
import java.util.List;

public class DatabaseAdapterControlledPorts extends DatabaseAdapter{
    public static final String TABLE_NAME = "controlled_ports";
    public static final String ELEMENT_ID = "element_id";
    public static final String AXIS_NUM = "axis_num";
    public static final String DEVICE_ADDRESS = "device_address";
    public static final String DEVICE_PORT_NUM = "device_port_num";
    public static final String DISPLAY_ID = "display_id";
    public static final String DIRECTION_OF_ROTATION = "direction_of_rotation";

    public DatabaseAdapterControlledPorts(Context context){ super(context); }

    public static void createTable(SQLiteDatabase db){
        db.execSQL("CREATE TABLE " + TABLE_NAME + " ("
                + ELEMENT_ID + " INTEGER NOT NULL, "
                + AXIS_NUM + " INTEGER NOT NULL CHECK(" + AXIS_NUM + " >= 0 AND " + AXIS_NUM + " < 2), "
                + DEVICE_ADDRESS + " TEXT NOT NULL, "
                + DEVICE_PORT_NUM + " INTEGER NOT NULL CHECK(" + DEVICE_PORT_NUM + " >= 0 AND "
                    + DEVICE_PORT_NUM + " < 4), "
                + DISPLAY_ID + " INTEGER NOT NULL, "
                + DIRECTION_OF_ROTATION + " INTEGER NOT NULL CHECK(" + DIRECTION_OF_ROTATION + " = 0"
                    + " OR " + DIRECTION_OF_ROTATION + " = 1),"
                + "FOREIGN KEY (" + ELEMENT_ID + ") REFERENCES " + DatabaseAdapterElementsControl.TABLE_NAME
                    + "(" + DatabaseAdapterElementsControl.ID + ") ON DELETE CASCADE, "
                + "FOREIGN KEY (" + DEVICE_ADDRESS + ") REFERENCES " + DatabaseAdapterForDevices.TABLE_NAME
                    + "(" + DatabaseAdapterForDevices.DEVICE_ADDRESS + ") ON DELETE CASCADE, "
                + "FOREIGN KEY (" + DISPLAY_ID + ") REFERENCES " + DatabaseAdapterDisplays.TABLE_NAME
                    + "(" + DatabaseAdapterDisplays.ID + ") ON DELETE CASCADE, "
                + "PRIMARY KEY(" + DEVICE_ADDRESS + ", " + DISPLAY_ID + ", " + DEVICE_PORT_NUM + "));");
    }

    public String[] getColumns(){
        return new String[] {ELEMENT_ID, AXIS_NUM, DEVICE_ADDRESS, DEVICE_PORT_NUM, DISPLAY_ID,
                DIRECTION_OF_ROTATION};
    }

    public ArrayList<ControlledPort> getControlledPortsByElementIDAndAxisNum(long elementID, int axisNum){
        Cursor cursor =  database.query(TABLE_NAME, getColumns(), ELEMENT_ID + " = "
                        + elementID + " AND " + AXIS_NUM + " = " + axisNum, null,
                null, null, null);
        ArrayList controlledPorts = new ArrayList();
        while (cursor.moveToNext()){
            ControlledPort controlledPort = new ControlledPort();
            controlledPort.elementID = elementID;
            controlledPort.axisNum = axisNum;
            controlledPort.displayID = cursor.getLong(cursor.getColumnIndexOrThrow(DISPLAY_ID));
            controlledPort.deviceAddress = cursor.getString(cursor.getColumnIndexOrThrow(DEVICE_ADDRESS));
        }
        return controlledPorts;
    }
}
