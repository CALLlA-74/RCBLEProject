package com.example.rcbleproject.Database;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.rcbleproject.PortConnection;

import java.util.ArrayList;

public class DatabaseAdapterPortConnections extends DatabaseAdapter{
    public static final String TABLE_NAME = "port_connections";
    public static final String ELEMENT_ID = "element_id";
    public static final String AXIS_NUM = "axis_num";
    public static final String DEVICE_ADDRESS = "device_address";
    public static final String DEVICE_PORT_NUM = "device_port_num";
    public static final String DISPLAY_ID = "display_id";
    public static final String DIRECTION_OF_ROTATION = "direction_of_rotation";

    public DatabaseAdapterPortConnections(Context context){
        super(context);
        open();
    }

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
                + "FOREIGN KEY (" + DEVICE_ADDRESS + ") REFERENCES " + DatabaseAdapterForHubs.TABLE_NAME
                    + "(" + DatabaseAdapterForHubs.HUB_ADDRESS + ") ON DELETE CASCADE, "
                + "FOREIGN KEY (" + DISPLAY_ID + ") REFERENCES " + DatabaseAdapterDisplays.TABLE_NAME
                    + "(" + DatabaseAdapterDisplays.ID + ") ON DELETE CASCADE, "
                + "PRIMARY KEY(" + DEVICE_ADDRESS + ", " + DISPLAY_ID + ", " + DEVICE_PORT_NUM + "));");
    }

    public String[] getColumns(){
        return new String[] {ELEMENT_ID, AXIS_NUM, DEVICE_ADDRESS, DEVICE_PORT_NUM, DISPLAY_ID,
                DIRECTION_OF_ROTATION};
    }

    public ArrayList<PortConnection> getPortConnectionsByElementIDAndAxisNum(long elementID, int axisNum){
        Cursor cursor =  database.query(TABLE_NAME, getColumns(), ELEMENT_ID + " = "
                        + elementID + " AND " + AXIS_NUM + " = " + axisNum, null,
                null, null, null);
        ArrayList<PortConnection> portConnections = new ArrayList<>();
        long displayID;
        int portNum, direction;
        while (cursor.moveToNext()){
            displayID = cursor.getLong(cursor.getColumnIndexOrThrow(DISPLAY_ID));
            portNum = cursor.getInt(cursor.getColumnIndexOrThrow(DEVICE_PORT_NUM));
            direction = cursor.getInt(cursor.getColumnIndexOrThrow(DIRECTION_OF_ROTATION));
            portConnections.add(new PortConnection());
            //controlledPort.hubAddress = cursor.getString(cursor.getColumnIndexOrThrow(DEVICE_ADDRESS));
        }
        return portConnections;
    }

    public ArrayList<PortConnection> getPortConnectionsByDisplayID(long displayID){
        Cursor cursor =  database.query(TABLE_NAME, getColumns(), DISPLAY_ID + " = "
                        + displayID, null, null, null, null);
        ArrayList<PortConnection> portConnections = new ArrayList<>();
        long elementID;
        int portNum, direction, axisNum;
        while (cursor.moveToNext()){
            axisNum = cursor.getInt(cursor.getColumnIndexOrThrow(AXIS_NUM));
            elementID = cursor.getInt(cursor.getColumnIndexOrThrow(ELEMENT_ID));
            portNum = cursor.getInt(cursor.getColumnIndexOrThrow(DEVICE_PORT_NUM));
            direction = cursor.getInt(cursor.getColumnIndexOrThrow(DIRECTION_OF_ROTATION));
            portConnections.add(new PortConnection());
            //controlledPort.hubAddress = cursor.getString(cursor.getColumnIndexOrThrow(DEVICE_ADDRESS));
        }
        return portConnections;
    }
}
