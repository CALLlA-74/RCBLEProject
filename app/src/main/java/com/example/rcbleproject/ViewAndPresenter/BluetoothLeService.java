package com.example.rcbleproject.ViewAndPresenter;

import static android.bluetooth.BluetoothDevice.BOND_BONDED;
import static android.bluetooth.BluetoothDevice.BOND_BONDING;
import static android.bluetooth.BluetoothDevice.TRANSPORT_LE;
import static android.bluetooth.BluetoothGatt.GATT_SUCCESS;
import static android.bluetooth.BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelUuid;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.RequiresApi;

import com.example.rcbleproject.BuildConfig;
import com.example.rcbleproject.Container;
import com.example.rcbleproject.Database.DatabaseAdapterForHubs;
import com.example.rcbleproject.Model.BluetoothHub;
import com.example.rcbleproject.R;
import com.example.rcbleproject.databinding.ActivityAddingHubsBinding;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

public class BluetoothLeService extends BaseAppActivity {
    private IListViewAdapterForHubs lvAdapterConnectedDevices = Container.getDbForHubs(this);
    private IListViewAdapterForHubs lvAdapterFoundHubs;

    protected boolean bluetoothRequested;
    protected boolean locationRequested;
    protected boolean permissionLocationRequested;
    protected boolean permissionScanRequested;
    protected boolean permissionConnectRequested;
    protected boolean isLeScanStarted = false;
    protected ActivityResultLauncher launcher;

    protected BluetoothAdapter bluetoothAdapter;
    protected BluetoothLeScanner BLEScanner;
    protected ActivityAddingHubsBinding binding;

    protected DatabaseAdapterForHubs dbHubsAdapter;

    protected static final HashMap<String, BluetoothGatt> gatts = Container.getGatts();
    protected final BluetoothLeService activity = this;

    public void setLvAdapterConnectedDevices(IListViewAdapterForHubs adapter){
        lvAdapterConnectedDevices = adapter;
    }

    protected void setLvAdapterFoundHubs(IListViewAdapterForHubs adapter){
        lvAdapterFoundHubs = adapter;
    }

    @Override
    protected void onCreate(Bundle savedInstance){
        super.onCreate(savedInstance);
        bluetoothRequested = false;
        locationRequested = false;
        permissionLocationRequested = false;
        permissionScanRequested = false;
        permissionConnectRequested = false;

        bluetoothAdapter = ((BluetoothManager) getSystemService(BLUETOOTH_SERVICE)).getAdapter();
        launcher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> checkBluetoothPeripherals());
    }

    @Override
    protected void onResume(){
        super.onResume();
        startLEScan();
        BluetoothDevice device;
        for (HashMap.Entry<String, BluetoothGatt> gatt : gatts.entrySet()){
            device = gatt.getValue().getDevice();
            lvAdapterConnectedDevices.setAvailability(true, device);
        }
    }

    @SuppressLint("MissingPermission")
    @Override
    protected void onPause() {
        super.onPause();
        stopLEScan();
    }

    public boolean checkBluetoothPeripherals(){
        if (bluetoothAdapter == null) {
            Toast.makeText(this, R.string.no_bluetooth_adapter, Toast.LENGTH_SHORT).show();
            return false;
        }
        if (bluetoothAdapter.isEnabled()
                && checkPermissionLocation()
                && checkLocation()
                && checkPermissionBLE_SCAN()
                && checkPermissionBLE_CONNECT()) return true;
        if (!bluetoothAdapter.isEnabled() && !bluetoothRequested) {
            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            launcher.launch(intent);
            bluetoothRequested = true;
            Toast.makeText(this, R.string.bluetooth_not_enabled, Toast.LENGTH_SHORT).show();
            return false;
        }

        if (bluetoothAdapter.isEnabled() && !permissionLocationRequested && !checkPermissionLocation()) {
            permissionLocationRequested = true;
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            Toast.makeText(this, R.string.no_access_to_geo, Toast.LENGTH_SHORT).show();
            return false;
        }

        if (bluetoothAdapter.isEnabled() && !permissionScanRequested && !checkPermissionBLE_SCAN()){
            requestPermissions(new String[]{Manifest.permission.BLUETOOTH_SCAN}, 2);
            permissionScanRequested = true;
            Toast.makeText(this, R.string.no_access_to_geo, Toast.LENGTH_SHORT).show();
            return false;
        }

        if (bluetoothAdapter.isEnabled() && !permissionConnectRequested && !checkPermissionBLE_CONNECT()){
            requestPermissions(new String[]{Manifest.permission.BLUETOOTH_CONNECT}, 3);
            permissionConnectRequested = true;
            Toast.makeText(this, R.string.no_access_to_geo, Toast.LENGTH_SHORT).show();
            return false;
        }

        if (bluetoothAdapter.isEnabled() && !locationRequested && !checkLocation()){
            Intent enableLocationIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            launcher.launch(enableLocationIntent);
            locationRequested = true;
            Toast.makeText(this, R.string.geo_not_enabled, Toast.LENGTH_SHORT).show();
            return false;
        }

        return false;
    }

    protected boolean checkPermissionLocation(){
        return checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    protected boolean checkPermissionBLE_SCAN(){
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true;
        return checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED;
    }

    protected boolean checkPermissionBLE_CONNECT(){
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true;
        return checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED;
    }

    protected boolean checkLocation(){
        try {
            LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            boolean enabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
            return enabled;
        } catch (NullPointerException e) {
            Toast.makeText(this, "Пожалуйста, предоствавьте доступ к геолокации в настройках телефона!", Toast.LENGTH_LONG).show();
        }
        return false;
    }

    @SuppressLint("MissingPermission")
    protected void startLEScan(){
        if (!checkBluetoothPeripherals()) return;
        if (bluetoothAdapter.isEnabled() && !isLeScanStarted) {
            BLEScanner = bluetoothAdapter.getBluetoothLeScanner();
            ScanSettings settings = new ScanSettings.Builder()
                    .setScanMode(ScanSettings.SCAN_MODE_LOW_POWER)
                    .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
                    .setMatchMode(ScanSettings.MATCH_MODE_AGGRESSIVE)
                    .setNumOfMatches(ScanSettings.MATCH_NUM_ONE_ADVERTISEMENT).build();
            ArrayList<ScanFilter> filters = getScanFilters();
            BLEScanner.startScan(filters, settings, scanCallback);
            isLeScanStarted = true;
            if (BuildConfig.DEBUG) Log.v("APP_TAG", "Start scan!");
        }
        Log.v("APP_TAG22", "gatts size: " + gatts.size());
    }

    protected ArrayList<ScanFilter> getScanFilters(){
        ArrayList<ScanFilter> filters = new ArrayList<>();
        for (UUID uuid : Container.getServiceUUIDs(getApplicationContext()).values()){
            filters.add(new ScanFilter.Builder().setServiceUuid(new ParcelUuid(uuid)).build());
        }
        return filters;
    }

    @SuppressLint("MissingPermission")
    protected void stopLEScan(){
        if (BLEScanner == null || !checkBluetoothPeripherals() || !isLeScanStarted) return;
        BLEScanner.stopScan(scanCallback);
        isLeScanStarted = false;
    }

    @SuppressLint("MissingPermission")
    protected void startLEScan(String deviceAddress){
        if (!checkBluetoothPeripherals()) return;
        if (bluetoothAdapter.isEnabled()) {
            BLEScanner = bluetoothAdapter.getBluetoothLeScanner();
            ScanFilter filter = new ScanFilter.Builder().setDeviceAddress(deviceAddress).build();
            ScanSettings settings = new ScanSettings.Builder()
                    .setScanMode(ScanSettings.SCAN_MODE_LOW_POWER).build();
            ArrayList<ScanFilter> filters = new ArrayList<>();
            filters.add(filter);
            BLEScanner.startScan(filters, settings, scanCallback);
            if (BuildConfig.DEBUG) Log.v("APP_TAG", "Start scan!");
        }
        Log.v("APP_TAG22", "gatts size: " + gatts.size());
    }

    @SuppressLint("MissingPermission")
    public void connectDevice(BluetoothDevice device){
        if (BuildConfig.DEBUG) Log.v("APP_TAG22", "try to connect");
        if (!checkBluetoothPeripherals()) return;
        new Thread(() -> {
            device.connectGatt(this, true, gattCallback, TRANSPORT_LE);
        }).start();
    }

    @SuppressLint("MissingPermission")
    public void connectDevice(String deviceAddress){
        connectDevice(bluetoothAdapter.getRemoteDevice(deviceAddress));
    }

    @SuppressLint("MissingPermission")
    public void disconnectDevice(BluetoothGatt gatt){
        if (BuildConfig.DEBUG) Log.v("APP_TAG22", "try to disconnect");
        if (!checkBluetoothPeripherals() || gatt == null) return;
        if (BuildConfig.DEBUG){
            Log.v("APP_TAG22", "start disconnecting, addr = " + gatt.getDevice().getAddress());
            Log.v("APP_TAG22", "gatt = " + gatt);
        }
        new Thread(() -> {
            runOnUiThread(() -> {
                gatts.remove(gatt.getDevice().getAddress());
                if (lvAdapterConnectedDevices != null)
                    lvAdapterConnectedDevices.removeHub(gatt.getDevice().getAddress());
                Log.v("APP_TAG22", "gatts size: " + gatts.size());
            });
            gatt.disconnect();
        }).start();
    }

    protected final ScanCallback scanCallback = new ScanCallback() {
        @RequiresApi(api = Build.VERSION_CODES.O)
        @SuppressLint("MissingPermission")
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            new Thread(() -> {
                BluetoothDevice device = result.getDevice();
                if (device != null) {
                    if (dbHubsAdapter != null && dbHubsAdapter.getHubStateConnection(device.getAddress())){
                        if (!gatts.containsKey(device.getAddress())) {
                            connectDevice(device);
                        }
                        return;
                    }
                    Log.v("APP_TAG6", device.getName());
                    if (lvAdapterFoundHubs != null){
                        runOnUiThread(() -> lvAdapterFoundHubs.addHub(
                                new BluetoothHub(result, activity, false)));
                    }
                }
            }).start();
        }
    };

    protected final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @SuppressLint("MissingPermission")
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            if (BuildConfig.DEBUG) {
                Log.v("APP_TAG22", "state changed: " + gatt.getDevice().getAddress());
                Log.v("APP_TAG22", "status: " + status + "; newState: " + newState);
            }
            if (status == GATT_SUCCESS){
                Log.v("APP_TAG2", "GATT_SUCCESS");
                if (newState == BluetoothProfile.STATE_CONNECTED){
                    int bondState = gatt.getDevice().getBondState();
                    Log.v("APP_TAG2", "STATE_CONNECTED. bondState: " + bondState);
                    int delay = 0;
                    switch (bondState){
                        case BOND_BONDED:
                            delay = Build.VERSION.SDK_INT <= Build.VERSION_CODES.N? 1000 : 0;
                            break;
                        case BOND_BONDING:
                            return;
                    }

                    if (!gatts.containsKey(gatt.getDevice().getAddress()))
                        runOnUiThread(() -> {
                            if (lvAdapterConnectedDevices == null) return;
                            if (lvAdapterFoundHubs == null) return;
                            BluetoothHub hub = lvAdapterFoundHubs.removeHub(gatt.getDevice().getAddress());
                            if (hub != null)
                                lvAdapterConnectedDevices.addHub(hub);
                        });
                    runOnUiThread(() -> {
                        gatts.put(gatt.getDevice().getAddress(), gatt);
                        if (lvAdapterConnectedDevices != null)
                            lvAdapterConnectedDevices.setAvailability(true, gatt.getDevice());
                    });
                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                        if (BuildConfig.DEBUG) Log.v("APP_TAG2", "Discover services");
                        boolean res = gatt.discoverServices();
                        if (!res && BuildConfig.DEBUG)
                            Log.v("APP_TAG2", "Discovering services was failed");
                            }, delay);
                }
                else if (newState == BluetoothProfile.STATE_DISCONNECTED){
                    String addr = gatt.getDevice().getAddress();
                    gatt.close();
                    Log.v("APP_TAG22", "gatt is closed; addr = " + addr);
                    Log.v("APP_TAG22", "gatt = " + gatt + "; Conn state: " + newState);
                }
            }
            else if (status == 8){
                if (BuildConfig.DEBUG) Log.e("APP_TAG", "Error. status = " + status);
                runOnUiThread(() -> {
                    gatts.remove(gatt.getDevice().getAddress());
                    if (lvAdapterConnectedDevices != null){
                        lvAdapterConnectedDevices.setAvailability(false, gatt.getDevice());
                    }
                });
                gatt.close();
            }
            else {
                if (BuildConfig.DEBUG) Log.e("APP_TAG", "Error. status = " + status);
                runOnUiThread(() -> {
                    gatts.remove(gatt.getDevice().getAddress());
                    if (lvAdapterConnectedDevices != null){
                        lvAdapterConnectedDevices.setAvailability(false, gatt.getDevice());
                    }
                });
                gatt.close();
            }
        }
    };

    @SuppressLint("MissingPermission")
    public void writeCharacteristic(BluetoothHub hub, byte[] message){
        Log.v("APP_TAG33", message.toString());
        if (!checkBluetoothPeripherals()) return;
        if (hub == null) return;
        BluetoothGatt bluetoothGatt = gatts.get(hub.address);
        if (bluetoothGatt == null) return;
        BluetoothGattService service = bluetoothGatt.getService(hub.serviceUuid);
        BluetoothGattCharacteristic characteristic = service.getCharacteristic(hub.characteristicUuid);

        byte[] value = characteristic.getValue();
        if (value != null && value.length == message.length){
            boolean isEqual = true;
            for (int i = value.length - 1; i >= 0; --i)
                if (value[i] != message[i]){
                    isEqual = false;
                    break;
                }
            if (isEqual) return;
        }

        /*if((characteristic.getProperties() & PROPERTY_WRITE_NO_RESPONSE) == 0 ) {
            Log.e("APP_TAG22", "proterties " + (characteristic.getProperties() & PROPERTY_WRITE_NO_RESPONSE));
            Log.e("APP_TAG22", "ERROR: Characteristic does not support writeType '" + characteristic.getWriteType() + "'");
            return false;
        }*/
        characteristic.setWriteType(WRITE_TYPE_NO_RESPONSE);
        characteristic.setValue(message);
        while (!bluetoothGatt.writeCharacteristic(characteristic));
    }

    /*public void alarmNoPermissions(){
        Toast.makeText(this, getString(R.string.alarm_no_permissions), Toast.LENGTH_LONG).show();
    }*/
}
