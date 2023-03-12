package com.example.rcbleproject;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.os.ParcelUuid;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.example.rcbleproject.Database.DatabaseAdapterForHubs;
import com.google.android.gms.common.util.ArrayUtils;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.UUID;


public class BluetoothHub extends BaseParam {
    public enum HubTypes {GeckoHub, PoweredUpHub, Unknown}

    private String name;
    public final String address;
    public final HubTypes hubType;
    public final UUID serviceUuid;
    public final UUID characteristicUuid;
    public long lastTimeAdv;
    public volatile boolean isActive = true;
    public volatile boolean availability = false;
    public volatile boolean stateConnection = true;

    @SuppressLint("MissingPermission")
    public BluetoothHub(@NonNull ScanResult result, @NonNull BaseAppBluetoothActivity context) {
        BluetoothDevice device = result.getDevice();
        address = device.getAddress();
        serviceUuid = getServiceUuid(result, context);
        hubType = getHubType(context);
        name = loadName(device, context);
        characteristicUuid = Container.getCharacteristicUUIDs(context).get(hubType);
    }

    public BluetoothHub(String name, String address, int type, Context context){
        this.name = name;
        this.address = address;
        this.hubType = IntToHubTypes(type);
        serviceUuid = Container.getServiceUUIDs(context).get(hubType);
        characteristicUuid = Container.getCharacteristicUUIDs(context).get(hubType);
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
    private String loadName(BluetoothDevice device, BaseAppBluetoothActivity context) {
        switch (hubType){
            case GeckoHub:
                DatabaseAdapterForHubs dbHubs = Container.getDbForHubs(context);
                dbHubs.loadHubName(this, context);
                return "";
            case PoweredUpHub:
                //dbHubs.updateNameByAddress(device.getAddress(), device.getName());
                return device.getName();
        }

        return "";
    }

    @Override
    public boolean equals(Object obj){
        if (obj == null) return false;
        try {
            BluetoothHub hub = (BluetoothHub) obj;
            return address.equals(hub.address);
        }
        catch (Exception e){
            return false;
        }
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

    public void setOutputPortCommand(BaseAppBluetoothActivity activity, int portNum, int direction){
        if (portNum < 0 || portNum > 4) return;
        final int d = Integer.compare(direction, 0);
        switch (hubType){
            case GeckoHub:
                new Thread(() -> {
                    byte[] gecko_message = {'0', '0', '0', '1', '0',
                            '2', '0', '3', '0',
                            '4', '0', '5', '0',
                            '6', '0', '7', '0'};
                    gecko_message[4*portNum + (d < 0? 2 : 0) + 2] = '7';
                    activity.writeCharacteristic(this, gecko_message);
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) { e.printStackTrace(); }
                    gecko_message[4*portNum + (d < 0? 2 : 0) + 2] = '0';
                    activity.writeCharacteristic(this, gecko_message);
                }).start();
                break;
            case PoweredUpHub:
                new Thread(() -> {
                    byte[] pu_message = {0x05, 0x00, (byte) 0x81, (byte) portNum, 0x10, 0x01, (byte) (100*d)};
                    activity.writeCharacteristic(this, pu_message);
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) { e.printStackTrace(); }
                    pu_message[6] = 0;
                    activity.writeCharacteristic(this, pu_message);
                }).start();
        }
    }

    public void setOutputPortCommand(BaseAppBluetoothActivity activity, int portNum, int direction,
                                     int speed){
        if (portNum < 0 || portNum > 4) return;
        switch (hubType){
            case GeckoHub:
                byte[] gecko_message = {'0', '0', '0', '1', '0',
                        '2', '0', '3', '0',
                        '4', '0', '5', '0',
                        '6', '0', '7', '0'};
                gecko_message[4*portNum + (direction < 0? 2 : 0) + 2] = '7';
                activity.writeCharacteristic(this, gecko_message);
                break;
            case PoweredUpHub:
                byte[] pu_message = {0x05, 0x00, (byte) 0x81, (byte) portNum, 0x10, 0x01,
                        (byte) (speed*direction)};
                Log.v("APP_TAG778", "hub: " + address+" " + portNum+" "+speed);
                activity.writeCharacteristic(this, pu_message);
        }
    }

    public boolean updateHubNameInDB(String newHubName) {
        if (hubType == HubTypes.PoweredUpHub){
            name = newHubName;
            return true;
        }
        return false;
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    public boolean rename(String newName, BaseAppBluetoothActivity activity){
        name = newName;
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

    @Override
    public String getName(){
        return name;
    }

    @Override
    public int getIconId(){
        switch (hubType){
            case PoweredUpHub:
                return R.drawable.pu_hub;
            case GeckoHub:
                return R.drawable.gecko_hub;
            case Unknown:
                return R.drawable.unknown_param;
        }
        return -1;
    }

    @Override
    public int getMenuIconId(){
        return R.drawable.alarm_icon;
    }

    @Override
    public void act(Object obj){
        alarm((BaseAppBluetoothActivity) obj);
    }

    public ArrayList<Port> getPorts(Context context) {
        ArrayList<Port> ports = new ArrayList<>();
        ports.add(new Port(context, this, 0, 1));
        ports.add(new Port(context, this, 0, -1));

        ports.add(new Port(context, this, 1, 1));
        ports.add(new Port(context, this, 1, -1));

        ports.add(new Port(context, this, 2, 1));
        ports.add(new Port(context, this, 2, -1));

        ports.add(new Port(context, this, 3, 1));
        ports.add(new Port(context, this, 3, -1));
        return ports;
    }

    public static String getDefaultHubName(Context context) {
        return context.getString(R.string.default_hub_name);
    }

    @Override
    public boolean getAvailabilityForAct(){
        return availability;
    }
}
