package com.example.rcbleproject;

import android.bluetooth.BluetoothGatt;
import android.content.Context;

import com.example.rcbleproject.Database.DatabaseAdapterControlledPorts;
import com.example.rcbleproject.Database.DatabaseAdapterDisplays;
import com.example.rcbleproject.Database.DatabaseAdapterElementsControl;
import com.example.rcbleproject.Database.DatabaseAdapterForDevices;
import com.example.rcbleproject.Database.DatabaseAdapterProfilesControl;

import java.util.HashMap;

public class Container {
    private static HashMap<String, BluetoothGatt> gatts = null;
    private static DatabaseAdapterControlledPorts dbControlledPorts = null;
    private static DatabaseAdapterDisplays dbDisplays = null;
    private static DatabaseAdapterElementsControl dbElementsControl = null;
    private static DatabaseAdapterForDevices dbForDevices = null;
    private static DatabaseAdapterProfilesControl dbProfilesControl = null;

    /**
     * Получаем список профилей BLE, сопоставленных с их mac-адресами.
     * @return HashMap профилей BLE.
     */
    public static HashMap<String, BluetoothGatt> getGatts(){
        if (gatts == null) gatts = new HashMap<>();
        return gatts;
    }

    /**
     * Получаем экземпляр с открытым сеансом для работы с таблицей ControlledPorts.
     * @param context - используется для инициализации экземпляра БД.
     * @return экземпляр таблицы ControlledPorts БД.
     */
    public static DatabaseAdapterControlledPorts getDbControlledPorts(Context context){
        if (dbControlledPorts == null){
            dbControlledPorts = new DatabaseAdapterControlledPorts(context);
            dbControlledPorts.open();
        }
        return dbControlledPorts;
    }

    /**
     * Получаем экземпляр с открытым сеансом для работы с таблицей Displays.
     * @param context - используется для инициализации экземпляра БД
     * @return экземпляр таблицы Displays БД.
     */
    public static DatabaseAdapterDisplays getDbDisplays(Context context){
        if (dbDisplays == null){
            dbDisplays = new DatabaseAdapterDisplays(context);
            dbDisplays.open();
        }
        return dbDisplays;
    }

    /**
     * Получаем экземпляр с открытым сеансом для работы с таблицей ElementsControl.
     * @param context - используется для инициализации экземпляра БД
     * @return экземпляр таблицы ElementsControl БД.
     */
    public static DatabaseAdapterElementsControl getDbElementsControl(Context context){
        if (dbElementsControl == null){
            dbElementsControl = new DatabaseAdapterElementsControl(context);
            dbElementsControl.open();
        }
        return dbElementsControl;
    }

    /**
     * Получаем экземпляр с открытым сеансом для работы с таблицей Devices.
     * @param context - используется для инициализации экземпляра БД
     * @return экземпляр таблицы Devices БД.
     */
    public static DatabaseAdapterForDevices getDbForDevices(Context context){
        if (dbForDevices == null){
            dbForDevices = new DatabaseAdapterForDevices(context);
            dbForDevices.open();
        }
        return dbForDevices;
    }

    /**
     * Получаем экземпляр с открытым сеансом для работы с таблицей ProfilesControl.
     * @param context - используется для инициализации экземпляра БД
     * @return экземпляр таблицы ProfilesControl БД.
     */
    public static DatabaseAdapterProfilesControl getDbProfilesControl(Context context){
        if (dbProfilesControl == null){
            dbProfilesControl = new DatabaseAdapterProfilesControl(context);
            dbProfilesControl.open();
        }
        return dbProfilesControl;
    }
}
