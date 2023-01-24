package com.example.rcbleproject;

import static com.example.rcbleproject.Database.DatabaseAdapterForHubs.HUB_ADDRESS;
import static com.example.rcbleproject.Database.DatabaseAdapterForHubs.HUB_NAME;
import static com.example.rcbleproject.Database.DatabaseAdapterForHubs.HUB_TYPE;
import static com.example.rcbleproject.R.id.tv_num_display;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.database.Cursor;
import android.os.ParcelUuid;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.example.rcbleproject.Database.DatabaseAdapterForHubs;
import com.google.android.gms.common.util.ArrayUtils;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;


public class BluetoothHub {
    public enum HubTypes {GeckoHub, PoweredUpHub, Unknown}

    public final String name;
    public final String address;
    public final HubTypes hubType;
    public final UUID serviceUuid;
    public final UUID characteristicUuid;
    public long lastTimeAdv;
    public volatile boolean isActive = true;

    @SuppressLint("MissingPermission")
    public BluetoothHub(@NonNull ScanResult result, @NonNull Context context) {
        BluetoothDevice device = result.getDevice();
        address = device.getAddress();
        serviceUuid = getServiceUuid(result, context);
        hubType = getHubType(context);
        name = getName(device, context);
        characteristicUuid = Container.getCharacteristicUUIDs(context).get(hubType);
    }

    public BluetoothHub(long hubId, @NonNull Context context){
        DatabaseAdapterForHubs dbHubs = Container.getDbForHubs(context);
        Cursor cursor = dbHubs.getHubById_cursor(hubId);
        if (!cursor.moveToFirst()){
            name = "";
            address = "";
            serviceUuid = null;
            characteristicUuid = null;
            hubType = HubTypes.Unknown;
        }
        else{
            name = cursor.getString(cursor.getColumnIndexOrThrow(HUB_NAME));
            address = cursor.getString(cursor.getColumnIndexOrThrow(HUB_ADDRESS));
            hubType = IntToHubTypes(cursor.getInt(cursor.getColumnIndexOrThrow(HUB_TYPE)));
            characteristicUuid = Container.getCharacteristicUUIDs(context).get(hubType);
            serviceUuid = Container.getServiceUUIDs(context).get(hubType);
        }
        cursor.close();
    }

    @SuppressLint("MissingPermission")
    private UUID getServiceUuid(ScanResult scanResult, Context context){
        UUID uuid = null;
        for (ParcelUuid parcelUuid : scanResult.getScanRecord().getServiceUuids()){
            if (Container.getServiceUUIDs(context).containsValue(parcelUuid.getUuid()))
                uuid = parcelUuid.getUuid();
        }
        return uuid;
    }

    private HubTypes getHubType(Context context){
        if (serviceUuid == null) return HubTypes.Unknown;
        HubTypes type = HubTypes.Unknown;
        Set<Map.Entry<HubTypes, UUID>> uuidsSet = Container.getServiceUUIDs(context).entrySet();
        for (Map.Entry<HubTypes, UUID> pair : uuidsSet){
            if (serviceUuid.equals(pair.getValue())) type = pair.getKey();
        }
        return type;
    }

    @SuppressLint("MissingPermission")
    private String getName(BluetoothDevice device, Context context) {
        DatabaseAdapterForHubs dbHubs = Container.getDbForHubs(context);
        switch (hubType){
            case GeckoHub:
                Cursor cursor = dbHubs.getHubByAddress_cursor(device.getAddress());
                if (!cursor.moveToFirst()) {
                    cursor.close();
                    return "Gecko Hub";
                }
                String name = cursor.getString(cursor.getColumnIndexOrThrow(HUB_NAME));
                cursor.close();
                return name;
            case PoweredUpHub:
                dbHubs.updateNameByAddress(device.getAddress(), device.getName());
                return device.getName();
        }

        return "";
    }

    public static HubTypes IntToHubTypes(int type){
        switch (type){
            case 0: return HubTypes.GeckoHub;
            case 1: return HubTypes.PoweredUpHub;
            default: return HubTypes.Unknown;
        }
    }
    
    public void alarm(BaseAppBluetoothActivity activity){
        switch (hubType){
            case GeckoHub:
                activity.writeCharacteristic(this, ("1").getBytes());
            case PoweredUpHub:
                new Thread(() -> {
                    byte[] message = {0x01, 0x00, 0x02, 0x05};
                    activity.writeCharacteristic(this, message);
                    try {
                        Thread.sleep(300);
                    } catch (InterruptedException e) { e.printStackTrace(); }
                    message[3] = 0x06;
                    activity.writeCharacteristic(this, message);
                }).start();
        }
    }

    public static boolean updateHubNameInDB(Context context, String hubAddress, String newHubName) {
        DatabaseAdapterForHubs adapter = Container.getDbForHubs(context);
        Cursor cursor = adapter.getHubByAddress_cursor(hubAddress);
        if (!cursor.moveToFirst()){
            cursor.close();
            return false;
        }
        HubTypes type = IntToHubTypes(cursor.getInt(cursor.getColumnIndexOrThrow(HUB_TYPE)));
        cursor.close();
        if (type == HubTypes.PoweredUpHub){
            adapter.updateNameByAddress(hubAddress, newHubName);
            return true;
        }
        return false;
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    public boolean updateHubNameOnRemoteDev(BaseAppBluetoothActivity activity, String newName){
        if (hubType == HubTypes.PoweredUpHub) {
            byte[] name = newName.getBytes(StandardCharsets.UTF_8);
            if (name.length > 14) {
                String toastText = activity.getResources().getString(R.string.too_long_name);
                Toast.makeText(activity, toastText, Toast.LENGTH_SHORT).show();
                return false;
            }
            Log.v("APP_TAG222", "type: " + hubType);
            Log.v("APP_TAG222", newName);
            Log.v("APP_TAG222", Integer.toString(newName.length()));
            new Thread(() -> {
                byte[] header = {0x02, 0x00, 0x01, 0x01, 0x01};
                header[0] += name.length;
                byte[] message = ArrayUtils.concatByteArrays(header, name);
                Log.v("APP_TAG", "len: " + message.length + " msg:");
                for (byte b : message) Log.v("APP_TAG", Byte.toString(b));
                activity.writeCharacteristic(this, message);
            }).start();
        }
        return true;
    }
}
