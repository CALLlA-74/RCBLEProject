package com.example.rcbleproject;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothGatt;
import android.content.Context;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import com.example.rcbleproject.Database.DatabaseAdapterPortConnections;
import com.example.rcbleproject.Database.DatabaseAdapterDisplays;
import com.example.rcbleproject.Database.DatabaseAdapterElementsControl;
import com.example.rcbleproject.Database.DatabaseAdapterForHubs;
import com.example.rcbleproject.Database.DatabaseAdapterProfilesControl;
import com.example.rcbleproject.Model.BluetoothHub;
import com.example.rcbleproject.ViewAndPresenter.BaseAppActivity;

import java.util.HashMap;
import java.util.UUID;

public class Container {
    public static final String appPrefKey = "app_preferences_key";
    public static final String currDisIdxPrefKey = "current_display_index_";
    public static final String currDisIdPrefKey = "current_display_id_";
    public static final String chosenProfControlPrefKey = "chosen_profile_control";
    public static final String numOfElementsPrefKey = "number_of_elements_";
    public static final String numOfDisplaysPrefKey = "number_of_displays_";

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
    public static DatabaseAdapterPortConnections getDbPortConnections(@NonNull BaseAppActivity context){
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
    public static DatabaseAdapterDisplays getDbDisplays(@NonNull BaseAppActivity context){
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
    public static DatabaseAdapterElementsControl getDbElementsControl(@NonNull BaseAppActivity context){
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
    @RequiresApi(api = Build.VERSION_CODES.O)
    public static DatabaseAdapterForHubs getDbForHubs(@NonNull BaseAppActivity context){
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
    public static DatabaseAdapterProfilesControl getDbProfilesControl(@NonNull BaseAppActivity context){
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
            serviceUUIDs.put(BluetoothHub.HubTypes.PowerFunctionsHub, UUID.fromString(context.getString(R.string.gecko_service_uuid)));
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
            characteristicUUIDs.put(BluetoothHub.HubTypes.PowerFunctionsHub, UUID.fromString(context.getString(R.string.gecko_characteristic_uuid)));
            characteristicUUIDs.put(BluetoothHub.HubTypes.PoweredUpHub, UUID.fromString(context.getString(R.string.pu_characteristic_uuid)));
        }
        return characteristicUUIDs;
    }
}
