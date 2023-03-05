package com.example.rcbleproject.Database;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Build;
import android.util.Log;

import androidx.annotation.RequiresApi;

import com.example.rcbleproject.BaseAppActivity;
import com.example.rcbleproject.BaseAppBluetoothActivity;
import com.example.rcbleproject.BluetoothHub;
import com.example.rcbleproject.IListViewAdapterForHubs;
import com.example.rcbleproject.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

public class DatabaseAdapterForHubs extends DatabaseAdapter implements IListViewAdapterForHubs {
    public static final String defaultHubAddress = "FF:FF:FF";
    public static final String TABLE_NAME = "hubs";
    public static final String ID = "_id";
    public static final String HUB_NAME = "hub_name";
    public static final String HUB_ADDRESS = "hub_address";
    public static final String HUB_TYPE = "hub_type";
    public static final String HUB_STATE_CONNECTION = "hub_state_connection"; /* статус соединения:
                                                                                           1 - соединено;
                                                                                           0 - соединение разорвано*/
    private static List<BluetoothHub> connectedHubs = null;
    private static Map<String, BluetoothHub> allHubs;

    @RequiresApi(api = Build.VERSION_CODES.O)
    public DatabaseAdapterForHubs(BaseAppActivity context){
        super(context);
        open();
        getConnectedHubs(context);
    }

    public static void createTable(SQLiteDatabase db){
        db.execSQL("CREATE TABLE " + TABLE_NAME
                + " (" + ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + HUB_NAME + " TEXT NOT NULL CHECK (" + HUB_NAME + " != ''), "
                + HUB_ADDRESS + " TEXT NOT NULL UNIQUE CHECK(" + HUB_ADDRESS + " != ''), "
                + HUB_STATE_CONNECTION + " INTEGER NOT NULL CHECK (" + HUB_STATE_CONNECTION
                     + " = 0 OR " + HUB_STATE_CONNECTION + " = 1), "
                + HUB_TYPE + " INTEGER NOT NULL);");

        ContentValues contentValues = new ContentValues();
        contentValues.put(HUB_NAME, "");
        contentValues.put(HUB_ADDRESS, defaultHubAddress);
        contentValues.put(HUB_STATE_CONNECTION, 0);
        contentValues.put(HUB_TYPE, 0);
        db.insert(TABLE_NAME, null, contentValues);
    }

    public String[] getColumns(){
        return new String[]{ID, HUB_NAME, HUB_ADDRESS, HUB_TYPE, HUB_STATE_CONNECTION};
    }

    private Cursor getConnectedHubs_cursor(){
        return database.query(TABLE_NAME, getColumns(), HUB_STATE_CONNECTION + " = 1",
                null, null, null, null);
    }

    private Cursor getAllHubs_cursor() {
        return database.query(TABLE_NAME, getColumns(), null,
                null, null, null, null);
    }

    private Cursor getHubByAddress_cursor(String address){
        return database.query(TABLE_NAME, getColumns(), HUB_ADDRESS + " = '" + address + "'",
                null, null, null, null);
    }

    /**
     * Возвращает список подключенных хабов из БД. Если данные еще не были выгружены из БД, то
     * будет возвращен список нулевой длины.
     * @param activity контекст Activity, в которой отображаются данные списка (для уведомления
     *                 о завершении выгрузки данных). Если уведомление не тредуется, то отправьте null.
     * @return список подключенных хабов.
     */
    @SuppressLint("ResourceType")
    public List<BluetoothHub> getConnectedHubs(BaseAppActivity activity){
        if (connectedHubs != null) return connectedHubs;
        connectedHubs = Collections.synchronizedList(new ArrayList<>(context.getResources().getInteger(R.integer.maxNumOfHubs)));
        if (allHubs == null) allHubs = Collections.synchronizedMap(new TreeMap<>());
        GetConnectedHubsAsync getAsync = new GetConnectedHubsAsync(activity);
        getAsync.execute();
        return connectedHubs;
    }

    /**
     * Возвращает список подключенных хабов из БД. Если данные еще не были выгружены из БД, то
     * будет возвращен список нулевой длины.
     * @param activity контекст Activity, в которой отображаются данные списка (для уведомления
     *                 о завершении выгрузки данных). Если уведомление не тредуется, то отправьте null.
     * @return список подключенных хабов.
     */
    @SuppressLint("ResourceType")
    public Map<String, BluetoothHub> getAllHubs(BaseAppActivity activity){
        if (allHubs != null) return allHubs;
        if (connectedHubs == null) connectedHubs = Collections.synchronizedList(new ArrayList<>(context.getResources().getInteger(R.integer.maxNumOfHubs)));
        allHubs = Collections.synchronizedMap(new TreeMap<>());
        GetConnectedHubsAsync getAsync = new GetConnectedHubsAsync(activity);
        getAsync.execute();
        return allHubs;
    }

    /**
     * Возвращает хаб по его адресу.
     * @param hubAddress адрес хаба.
     * @param activity  activity контекст Activity, в которой отображаются данные списка (для уведомления
     *                  о завершении выгрузки данных). Если уведомление не тредуется, то отправьте null.
     * @return хаб.
     */
    public BluetoothHub getHubByAddress(String hubAddress, BaseAppActivity activity){
        if (allHubs != null){
            return allHubs.get(hubAddress);
        }
        getConnectedHubs(activity);
        return null;
    }

    /**
     * Используется для добавления нового хаба в БД.
     * @param hub экземпляр хаба, добавляемого в БД.
     * @param activity контекст Activity, в которой отображаются данные списка (для уведомления
     *                 о завершении выгрузки данных). Если уведомление не тредуется, то отправьте null.
     */
    public boolean insert(BluetoothHub hub, BaseAppActivity activity){
        allHubs.put(hub.address, hub);
        if (connectedHubs.contains(hub)) return false;
        connectedHubs.add(hub);
        InsertOrUpdateAsync insertOrUpdateAsync = new InsertOrUpdateAsync(activity);
        insertOrUpdateAsync.execute(hub);
        return true;
    }

    public boolean addHub(BluetoothHub hub){
        hub.availability = true;
        hub.stateConnection = true;
        updateHub(hub, null);
        return true;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public BluetoothHub removeHub(String hubAddress){
        BluetoothHub bluetoothHub = findConnectedHubByAddress(hubAddress);
        if (bluetoothHub != null){
            bluetoothHub.stateConnection = false;
            updateHub(bluetoothHub, null);
        }
        return bluetoothHub;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @SuppressLint("MissingPermission")
    public boolean setAvailability(boolean flag, BluetoothDevice device){
        BluetoothHub hub = findConnectedHubByAddress(device.getAddress());
        if (hub == null) return false;
        if (flag && !hub.availability){
            hub.updateHubNameInDB(device.getName());
        }
        hub.availability = flag;
        return true;
    }

    public void updateHub(BluetoothHub hub, BaseAppActivity activity){
        if (hub == null) return;
        if (!connectedHubs.contains(hub)) {
            insert(hub, activity);
            return;
        }
        if (!hub.stateConnection) connectedHubs.remove(hub);
        UpdateAsync updateAsync = new UpdateAsync(activity);
        updateAsync.execute(hub);
    }

    /**
     * Ищет хаб по mac-адресу среди подключенных хабов
     * @param hubAddress mac-адрес хаба
     * @return указатель на найденный хаб, иначе - null
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    public BluetoothHub findConnectedHubByAddress(String hubAddress){
        if (connectedHubs == null) {
            getConnectedHubs(context);
            return null;
        }
        for (int pos = connectedHubs.size() - 1; pos >= 0; --pos){
            if (connectedHubs.get(pos).address.equals(hubAddress)){
                return connectedHubs.get(pos);
            }
        }
        return null;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public boolean getHubStateConnection(String hubAddress){
        BluetoothHub hub = findConnectedHubByAddress(hubAddress);
        return (hub != null) && hub.stateConnection;
    }

    public void loadHubName(BluetoothHub hub, BaseAppBluetoothActivity activity){
        new LoadHubNameAsync(activity).execute(hub);
    }

    @SuppressLint("StaticFieldLeak")
    private class GetConnectedHubsAsync extends DoingInBackAsync<Void, Void, Void>{
        public GetConnectedHubsAsync(BaseAppActivity activity){
            super(activity);
        }

        @Override
        protected Void doInBackground(Void... voids){
            synchronized (connectedHubs){
                //Cursor cursor = getConnectedHubs_cursor();
                Cursor cursor = getAllHubs_cursor();
                int hubNameIndex = cursor.getColumnIndexOrThrow(HUB_NAME);
                int hubAddressIndex = cursor.getColumnIndexOrThrow(HUB_ADDRESS);
                int hubTypeIndex = cursor.getColumnIndexOrThrow(HUB_TYPE);
                int stateConnIndex = cursor.getColumnIndexOrThrow(HUB_STATE_CONNECTION);
                while (cursor.moveToNext()){
                    String name = cursor.getString(hubNameIndex);
                    String address = cursor.getString(hubAddressIndex);
                    int hubType = cursor.getInt(hubTypeIndex);
                    int stateConn = cursor.getInt(stateConnIndex);
                    BluetoothHub hub = new BluetoothHub(name, address, hubType, context);
                    allHubs.put(address, hub);
                    if (stateConn == 1) connectedHubs.add(hub);
                }
                cursor.close();
            }
            return null;
        }
    }

    @SuppressLint("StaticFieldLeak")
    private class InsertOrUpdateAsync extends DoingInBackAsync<BluetoothHub, Void, Void> {
        public InsertOrUpdateAsync(BaseAppActivity activity) {
            super(activity);
        }

        @Override
        protected Void doInBackground(BluetoothHub... hubs){
            for (BluetoothHub hub : hubs){
                ContentValues contentValues = new ContentValues();
                contentValues.put(HUB_NAME, hub.getName());
                contentValues.put(HUB_ADDRESS, hub.address);
                contentValues.put(HUB_STATE_CONNECTION, 1);
                contentValues.put(HUB_TYPE, hub.hubType.ordinal());
                Cursor c = getHubByAddress_cursor(hub.address);
                if (!c.moveToFirst()){
                    long id = database.insert(TABLE_NAME, null, contentValues);
                    Log.v("APP_TAG55", "hub id = " + id);
                }
                else
                    database.update(TABLE_NAME, contentValues, HUB_ADDRESS + " = '" + hub.address + "'", null);
            }
            return null;
        }
    }

    @SuppressLint("StaticFieldLeak")
    private class UpdateAsync extends DoingInBackAsync<BluetoothHub, Void, Void>{
        public UpdateAsync(BaseAppActivity activity){
            super(activity);
        }

        @Override
        protected Void doInBackground(BluetoothHub... hubs){
            for (BluetoothHub hub : hubs){
                ContentValues contentValues = new ContentValues();
                contentValues.put(HUB_NAME, hub.getName());
                contentValues.put(HUB_ADDRESS, hub.address);
                contentValues.put(HUB_STATE_CONNECTION, hub.stateConnection);
                contentValues.put(HUB_TYPE, hub.hubType.ordinal());
                database.update(TABLE_NAME, contentValues, HUB_ADDRESS + " = '" + hub.address + "'", null);
            }
            return null;
        }
    }

    @SuppressLint("StaticFieldLeak")
    private class LoadHubNameAsync extends DoingInBackAsync<BluetoothHub, Void, Void>{
        public LoadHubNameAsync(BaseAppActivity activity){
            super(activity);
        }

        @Override
        protected Void doInBackground(BluetoothHub... hubs){
            for (BluetoothHub hub : hubs){
                Cursor c = database.query(TABLE_NAME, getColumns(), HUB_ADDRESS + " = '" + hub.address + "'",
                        null, null, null, null);
                if (c.moveToFirst()){
                    hub.rename(c.getString(c.getColumnIndexOrThrow(HUB_NAME)), (BaseAppBluetoothActivity) activity);
                }
                else hub.rename(BluetoothHub.getDefaultHubName(context), (BaseAppBluetoothActivity) activity);
            }
            return null;
        }
    }
}
