package com.example.rcbleproject;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothGatt;
import android.content.Context;

import androidx.annotation.NonNull;

import com.example.rcbleproject.Database.DatabaseAdapterPortConnections;
import com.example.rcbleproject.Database.DatabaseAdapterDisplays;
import com.example.rcbleproject.Database.DatabaseAdapterElementsControl;
import com.example.rcbleproject.Database.DatabaseAdapterForHubs;
import com.example.rcbleproject.Database.DatabaseAdapterProfilesControl;

import java.util.HashMap;
import java.util.UUID;

public class Container {
    private static HashMap<String, BluetoothGatt> gatts = null;
    private static HashMap<BluetoothHub.HubTypes, UUID> serviceUUIDs = null,
                                                        characteristicUUIDs = null;

    @SuppressLint("StaticFieldLeak")
    private static DatabaseAdapterPortConnections dbPortConnections = null;
    @SuppressLint("StaticFieldLeak")
    private static DatabaseAdapterDisplays dbDisplays = null;
    @SuppressLint("StaticFieldLeak")
    private static DatabaseAdapterElementsControl dbElementsControl = null;
    @SuppressLint("StaticFieldLeak")
    private static DatabaseAdapterForHubs dbForDevices = null;
    @SuppressLint("StaticFieldLeak")
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
    public static DatabaseAdapterPortConnections getDbPortConnections(@NonNull Context context){
        if (dbPortConnections == null){
            dbPortConnections = new DatabaseAdapterPortConnections(context);
        }
        return dbPortConnections;
    }

    /**
     * Получаем экземпляр с открытым сеансом для работы с таблицей Displays.
     * @param context - используется для инициализации экземпляра БД
     * @return экземпляр таблицы Displays БД.
     */
    public static DatabaseAdapterDisplays getDbDisplays(@NonNull Context context){
        if (dbDisplays == null){
            dbDisplays = new DatabaseAdapterDisplays(context);
        }
        return dbDisplays;
    }

    /**
     * Получаем экземпляр с открытым сеансом для работы с таблицей ElementsControl.
     * @param context - используется для инициализации экземпляра БД
     * @return экземпляр таблицы ElementsControl БД.
     */
    public static DatabaseAdapterElementsControl getDbElementsControl(@NonNull Context context){
        if (dbElementsControl == null){
            dbElementsControl = new DatabaseAdapterElementsControl(context);
        }
        return dbElementsControl;
    }

    /**
     * Получаем экземпляр с открытым сеансом для работы с таблицей Devices.
     * @param context - используется для инициализации экземпляра БД
     * @return экземпляр таблицы Devices БД.
     */
    public static DatabaseAdapterForHubs getDbForHubs(@NonNull Context context){
        if (dbForDevices == null){
            dbForDevices = new DatabaseAdapterForHubs(context);
        }
        return dbForDevices;
    }

    /**
     * Получаем экземпляр с открытым сеансом для работы с таблицей ProfilesControl.
     * @param context - используется для инициализации экземпляра БД
     * @return экземпляр таблицы ProfilesControl БД.
     */
    public static DatabaseAdapterProfilesControl getDbProfilesControl(@NonNull Context context){
        if (dbProfilesControl == null){
            dbProfilesControl = new DatabaseAdapterProfilesControl(context);
        }
        return dbProfilesControl;
    }

    /**
     * Получаем экземпляр коллекции UUID BLE-сервисов.
     * @param context - используется для инициализации экземпляра БД
     * @return экземпляр таблицы ProfilesControl БД.
     */
    public static HashMap<BluetoothHub.HubTypes, UUID> getServiceUUIDs(@NonNull Context context){
        if (serviceUUIDs == null){
            serviceUUIDs = new HashMap<>();
            serviceUUIDs.put(BluetoothHub.HubTypes.GeckoHub, UUID.fromString(context.getString(R.string.gecko_service_uuid)));
            serviceUUIDs.put(BluetoothHub.HubTypes.PoweredUpHub, UUID.fromString(context.getString(R.string.pu_service_uuid)));
        }
        return serviceUUIDs;
    }

    /**
     * Получаем экземпляр коллекции UUID BLE-характеристик.
     * @param context - используется для инициализации экземпляра БД
     * @return экземпляр таблицы ProfilesControl БД.
     */
    public static HashMap<BluetoothHub.HubTypes, UUID> getCharacteristicUUIDs(@NonNull Context context){
        if (characteristicUUIDs == null){
            characteristicUUIDs = new HashMap<>();
            characteristicUUIDs.put(BluetoothHub.HubTypes.GeckoHub, UUID.fromString(context.getString(R.string.gecko_characteristic_uuid)));
            characteristicUUIDs.put(BluetoothHub.HubTypes.PoweredUpHub, UUID.fromString(context.getString(R.string.pu_characteristic_uuid)));
        }
        return characteristicUUIDs;
    }
}
