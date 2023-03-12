package com.example.rcbleproject.Database;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import android.widget.Toast;

import com.example.rcbleproject.BaseAppActivity;
import com.example.rcbleproject.BaseControlElement;
import com.example.rcbleproject.BluetoothHub;
import com.example.rcbleproject.BuildConfig;
import com.example.rcbleproject.Container;
import com.example.rcbleproject.GameControllersDrawer;
import com.example.rcbleproject.Port;
import com.example.rcbleproject.PortConnection;
import com.example.rcbleproject.SettingPortConnectionsActivity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.TreeMap;

public class DatabaseAdapterPortConnections extends DatabaseAdapter{
    public static final String TABLE_NAME = "port_connections";
    public static final String ID = "_id";
    public static final String ELEMENT_ID = "element_id";
    public static final String AXIS_NUM = "axis_num";
    public static final String DEVICE_ADDRESS = "device_address";
    public static final String DEVICE_PORT_NUM = "device_port_num";
    public static final String DISPLAY_ID = "display_id";
    public static final String DIRECTION_OF_ROTATION = "direction_of_rotation";

    //private static List<List<PortConnection>> portConnections;
    //private static long profileID = -1;

    public DatabaseAdapterPortConnections(BaseAppActivity context){
        super(context);
        open();
    }

    public static void createTable(SQLiteDatabase db){
        db.execSQL("CREATE TABLE " + TABLE_NAME + " ("
                + ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + ELEMENT_ID + " INTEGER NOT NULL CHECK(" + ELEMENT_ID +  " >= -1),"
                + AXIS_NUM + " INTEGER NOT NULL CHECK(" + AXIS_NUM + " >= -1 AND " + AXIS_NUM + " < 2), "
                + DEVICE_ADDRESS + " TEXT NOT NULL, "
                + DEVICE_PORT_NUM + " INTEGER NOT NULL CHECK(" + DEVICE_PORT_NUM + " >= -1 AND "
                    + DEVICE_PORT_NUM + " < 4), "
                + DISPLAY_ID + " INTEGER NOT NULL, "
                + DIRECTION_OF_ROTATION + " INTEGER NOT NULL CHECK(" + DIRECTION_OF_ROTATION + " = -1"
                    + " OR " + DIRECTION_OF_ROTATION + " = 1),"
                + "FOREIGN KEY (" + ELEMENT_ID + ") REFERENCES " + DatabaseAdapterElementsControl.TABLE_NAME
                    + "(" + DatabaseAdapterElementsControl.ID + ") ON DELETE CASCADE, "
                + "FOREIGN KEY (" + DEVICE_ADDRESS + ") REFERENCES " + DatabaseAdapterForHubs.TABLE_NAME
                    + "(" + DatabaseAdapterForHubs.HUB_ADDRESS + ") ON DELETE CASCADE, "
                + "FOREIGN KEY (" + DISPLAY_ID + ") REFERENCES " + DatabaseAdapterDisplays.TABLE_NAME
                    + "(" + DatabaseAdapterDisplays.ID + ") ON DELETE CASCADE);");
                //+ "PRIMARY KEY(" + DEVICE_ADDRESS + ", " + DISPLAY_ID + ", " + DEVICE_PORT_NUM + "));");
    }

    public String[] getColumns(){
        return new String[] {ID, ELEMENT_ID, AXIS_NUM, DEVICE_ADDRESS, DEVICE_PORT_NUM, DISPLAY_ID,
                DIRECTION_OF_ROTATION};
    }

    /*public void insertPortConnection(PortConnection portConnection, BaseAppActivity activity){
        if (portConnections == null){
            Toast.makeText(activity, "Ошибка добавления соединения", Toast.LENGTH_SHORT).show();
            return;
        }
        if (portConnection.displayIndex < 0 || portConnections.size() <= portConnection.displayIndex) {
            Toast.makeText(activity, "Ошибка добавления соединения", Toast.LENGTH_SHORT).show();
            return;
        }
        portConnections.get(portConnection.displayIndex).add(portConnection);
        new InsertAsync(activity).execute(portConnection);
    }*/

    public long insert(long displayID){
        /*if (portConnection.port == null || portConnection.hub == null
                || portConnection.controllerAxis == null) return;*/
        long id = Container.getDbElementsControl(context).getUnknownElementIdByDisplayId(displayID);
        ContentValues contentValues = new ContentValues();
        contentValues.put(DISPLAY_ID, displayID);
        contentValues.put(ELEMENT_ID, id);
        contentValues.put(AXIS_NUM, -1);
        contentValues.put(DEVICE_ADDRESS, DatabaseAdapterForHubs.defaultHubAddress);
        contentValues.put(DEVICE_PORT_NUM, -1);
        contentValues.put(DIRECTION_OF_ROTATION, -1);
        return database.insert(TABLE_NAME, null, contentValues);
    }

    public void update(PortConnection portConnection){
        ContentValues contentValues = new ContentValues();
        contentValues.put(DISPLAY_ID, portConnection.getDisplayID());

        if (portConnection.controllerAxis == null) {
            contentValues.put(ELEMENT_ID, -1);
            contentValues.put(AXIS_NUM, -1);
        }
        else {
            contentValues.put(ELEMENT_ID, portConnection.controllerAxis.parent.elementID);
            contentValues.put(AXIS_NUM, portConnection.controllerAxis.axisNum);
        }

        if (portConnection.hub == null){
            contentValues.put(DEVICE_ADDRESS, DatabaseAdapterForHubs.defaultHubAddress);
            contentValues.put(DEVICE_PORT_NUM, -1);
            contentValues.put(DIRECTION_OF_ROTATION, -1);
        }
        else {
            contentValues.put(DEVICE_ADDRESS, portConnection.hub.address);
            if (portConnection.port == null){
                contentValues.put(DEVICE_PORT_NUM, -1);
                contentValues.put(DIRECTION_OF_ROTATION, -1);
            }
            else {
                contentValues.put(DEVICE_PORT_NUM, portConnection.port.portNum);
                contentValues.put(DIRECTION_OF_ROTATION, portConnection.port.getDirection());
            }
        }
        database.update(TABLE_NAME, contentValues, ID + " = " + portConnection.getId(),
                null);
    }

    public void delete(SettingPortConnectionsActivity activity, PortConnection portConnection) {
        //new DeleteAsync(null).execute(portConnection);
        if (portConnection.port != null && portConnection.hub != null){
            Port p1 = new Port(portConnection.port.context, portConnection.hub, portConnection.port.portNum, 1);
            Port p2 = new Port(portConnection.port.context, portConnection.hub, portConnection.port.portNum, -1);
            long disId = portConnection.getDisplayID();
            TreeMap<String, List<Port>> portsByHubs = activity.getHubPortsByDisplays().get(disId);
            List<Port> ports = portsByHubs.get(portConnection.hub.address);
            ports.add(p1);
            ports.add(p2);
            ports.sort((o1, o2) -> {
                if (o1.portNum == o2.portNum)
                    return o2.getDirection() - o1.getDirection();
                return o1.portNum - o2.portNum;
            });
        }
        database.delete(TABLE_NAME, ID + " = " + portConnection.getId(), null);
    }

    public List<PortConnection> getPortConnectionsByDisplayID(long displayID, BaseAppActivity activity){
        Cursor cursor =  database.query(TABLE_NAME, getColumns(), DISPLAY_ID + " = "
                + displayID, null, null, null, null);
        ArrayList<PortConnection> portConnections = new ArrayList<>();
        long elementID, id;
        int portNum, direction, axisNum;
        String deviceAddress;
        int axisNumIndex = cursor.getColumnIndexOrThrow(AXIS_NUM),
            elementIDIndex = cursor.getColumnIndexOrThrow(ELEMENT_ID),
            devicePortNumIndex = cursor.getColumnIndexOrThrow(DEVICE_PORT_NUM),
            directionIndex = cursor.getColumnIndexOrThrow(DIRECTION_OF_ROTATION),
            deviceAddressIndex = cursor.getColumnIndexOrThrow(DEVICE_ADDRESS),
            idIndex = cursor.getColumnIndexOrThrow(ID);
        while (cursor.moveToNext()){
            id = cursor.getLong(idIndex);
            axisNum = cursor.getInt(axisNumIndex);
            elementID = cursor.getInt(elementIDIndex);   //TODO реализовать механизм удаления соединения при удалении элемента управления, входящего в соединение
            portNum = cursor.getInt(devicePortNumIndex);
            direction = cursor.getInt(directionIndex);
            deviceAddress = cursor.getString(deviceAddressIndex);
            BluetoothHub hub = null;
            if (!deviceAddress.equals(DatabaseAdapterForHubs.defaultHubAddress)){
                while(hub == null){
                    hub = Container.getDbForHubs(context).getHubByAddress(deviceAddress, null);
                    if (BuildConfig.DEBUG) Log.v("APP_TAG777", "inf");
                }
            }

            Port port = null;
            if (hub != null && portNum > -1) {
                port = new Port(activity, hub, portNum, direction);
            }

            BaseControlElement.ControllerAxis axis = null;
            if (elementID != -1 && axisNum > -1){
                BaseControlElement element = GameControllersDrawer.getElementByID(elementID);
                axis = element == null? null : element.getControllerAxes().get(axisNum);
            }
            portConnections.add(new PortConnection(id, displayID, hub, port, axis));
            //controlledPort.hubAddress = cursor.getString(cursor.getColumnIndexOrThrow(DEVICE_ADDRESS));
        }
        return portConnections;
    }

    /*public List<List<PortConnection>> getPortConnectionsByProfileID(long profileID, BaseAppActivity activity){
        if (portConnections != null && DatabaseAdapterPortConnections.profileID == profileID && profileID >= 0)
            return portConnections;

        DatabaseAdapterPortConnections.profileID = profileID;
        portConnections = Collections.synchronizedList(new ArrayList<>());
        new LoadPortConnectionsAsync(activity).execute();
        return portConnections;
    }*/

    @SuppressLint("StaticFieldLeak")
    private class InsertAsync extends DoingInBackAsync<PortConnection, Void, Void>{
        public InsertAsync(BaseAppActivity activity){
            super(activity);
        }

        @Override
        protected Void doInBackground(PortConnection... portConnections){
            for (PortConnection portConn : portConnections){
                if (portConn.port == null || portConn.hub == null || portConn.controllerAxis == null)
                    break;
                ContentValues contentValues = new ContentValues();
                contentValues.put(ELEMENT_ID, portConn.controllerAxis.parent.elementID);
                contentValues.put(AXIS_NUM, portConn.controllerAxis.axisNum);
                contentValues.put(DEVICE_ADDRESS, portConn.hub.address);
                contentValues.put(DEVICE_PORT_NUM, portConn.port.portNum);
                contentValues.put(DISPLAY_ID, portConn.controllerAxis.parent.displayID);
                contentValues.put(DIRECTION_OF_ROTATION, portConn.port.getDirection());
                database.insert(TABLE_NAME, null, contentValues);
            }
            return null;
        }
    }

    @SuppressLint("StaticFieldLeak")
    private class DeleteAsync extends DoingInBackAsync<PortConnection, Void, Void>{
        public DeleteAsync(BaseAppActivity activity){
            super(activity);
        }

        @Override
        protected Void doInBackground(PortConnection... portConnections){
            for (PortConnection portConn : portConnections){
                database.delete(TABLE_NAME,
                        DISPLAY_ID + " = " + portConn.controllerAxis.parent.displayID
                        + " AND " + DEVICE_ADDRESS + " = '" + portConn.hub.address + "'"
                        + " AND " + DEVICE_PORT_NUM + " = " + portConn.port.portNum, null);
            }
            return null;
        }
    }
}
