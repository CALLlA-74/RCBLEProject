package com.example.rcbleproject;

import static com.example.rcbleproject.Database.DatabaseAdapterForHubs.HUB_ADDRESS;
import static com.example.rcbleproject.Database.DatabaseAdapterForHubs.HUB_NAME;
import static com.example.rcbleproject.Database.DatabaseAdapterForHubs.HUB_TYPE;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.database.Cursor;
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

    public class Port extends BaseParam{
        private final Context context;
        private int direction = 0;
        public final BluetoothHub hub;
        public int portNum = -1;

        public Port(Context context, BluetoothHub hub){
            this.context = context;
            this.hub = hub;
        }

        public Port(Context context, BluetoothHub hub, int portNum, int direction){
            this.context = context;
            this.hub = hub;
            this.portNum = portNum;
            this.direction = direction;
        }

        public int getDirection() {return direction; }

        public void setDirection(int newDirection){
            direction = Integer.compare(newDirection, 0);
        }

        @Override
        public String getName() {
            switch (portNum){
                case 0:
                    if (direction == 1) return context.getString(R.string.port_a);
                    else return context.getString(R.string.port_a_inv);
                case 1:
                    if (direction == 1) return context.getString(R.string.port_b);
                    else return context.getString(R.string.port_b_inv);
                case 2:
                    if (direction == 1) return context.getString(R.string.port_c);
                    else return context.getString(R.string.port_c_inv);
                case 3:
                    if (direction == 1) return context.getString(R.string.port_d);
                    else return context.getString(R.string.port_d_inv);
            }
            return ".?.";
        }

        @Override
        public int getIconId(){
            switch (portNum){
                case 0:
                    return R.drawable.letter_a;
                case 1:
                    return R.drawable.letter_b;
                case 2:
                    return R.drawable.letter_c;
                case 3:
                    return R.drawable.letter_d;
            }

            return R.drawable.unknown_param;
        }

        @Override
        public int getMenuIconId(){
            if (direction < 0) return R.drawable.baseline_rotate_left_24;
            return R.drawable.baseline_rotate_right_24;
        }

        @Override
        public void act(Object obj){
            hub.setOutputPortCommand((BaseAppBluetoothActivity) obj, portNum, direction);
        }
    }

    private String name;
    public final String address;
    public final HubTypes hubType;
    public final UUID serviceUuid;
    public final UUID characteristicUuid;
    public long lastTimeAdv;
    public volatile boolean isActive = true;

    private ArrayList<Port> ports;

    @SuppressLint("MissingPermission")
    public BluetoothHub(@NonNull ScanResult result, @NonNull Context context) {
        BluetoothDevice device = result.getDevice();
        address = device.getAddress();
        serviceUuid = getServiceUuid(result, context);
        hubType = getHubType(context);
        name = getName(device, context);
        characteristicUuid = Container.getCharacteristicUUIDs(context).get(hubType);
        initPorts(context);
    }

    public BluetoothHub(String name, String address, int type, Context context){
        this.name = name;
        this.address = address;
        this.hubType = IntToHubTypes(type);
        serviceUuid = Container.getServiceUUIDs(context).get(hubType);
        characteristicUuid = Container.getCharacteristicUUIDs(context).get(hubType);
        initPorts(context);
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
        initPorts(context);
    }

    private void initPorts(Context context){
        ports = new ArrayList<>();
        ports.add(new Port(context, this, 0, 1));
        ports.add(new Port(context, this, 0, -1));

        ports.add(new Port(context, this, 1, 1));
        ports.add(new Port(context, this, 1, -1));

        ports.add(new Port(context, this, 2, 1));
        ports.add(new Port(context, this, 2, -1));

        ports.add(new Port(context, this, 3, 1));
        ports.add(new Port(context, this, 3, -1));
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

    public void setOutputPortCommand(BaseAppBluetoothActivity activity, int portNum, int d){
        if (portNum < 0 || portNum > 7) return;
        final int direction = Integer.compare(d, 0);
        switch (hubType){
            case GeckoHub:
                new Thread(() -> {
                    byte[] gecko_message = {'0', '0', '0', '1', '0',
                            '2', '0', '3', '0',
                            '4', '0', '5', '0',
                            '6', '0', '7', '0'};
                    gecko_message[4*portNum + (direction < 0? 2 : 0) + 2] = '7';
                    activity.writeCharacteristic(this, gecko_message);
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) { e.printStackTrace(); }
                    gecko_message[4*portNum + (direction < 0? 2 : 0) + 2] = '0';
                    activity.writeCharacteristic(this, gecko_message);
                }).start();
                break;
            case PoweredUpHub:
                new Thread(() -> {
                    byte[] pu_message = {0x05, 0x00, (byte) 0x81, (byte) portNum, 0x10, 0x01, (byte) (100*direction)};
                    activity.writeCharacteristic(this, pu_message);
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) { e.printStackTrace(); }
                    pu_message[6] = 0;
                    activity.writeCharacteristic(this, pu_message);
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
    public boolean setName(BaseAppBluetoothActivity activity, String newName){
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
        name = newName;
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

    public ArrayList<Port> getPorts() { return ports; }
}
