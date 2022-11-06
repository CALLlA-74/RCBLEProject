package com.example.rcbleproject;

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
import androidx.core.app.ActivityCompat;

import com.example.rcbleproject.Database.DatabaseAdapterForDevices;
import com.example.rcbleproject.databinding.ActivityAddingDevicesBinding;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class BaseAppBluetoothActivity extends BaseAppActivity{
    private IListViewAdapterForDevices lvAdapterConnectedDevices;
    private IListViewAdapterForDevices lvAdapterFoundDevices;

    protected boolean bluetoothRequested;
    protected boolean locationRequested;
    protected boolean permissionLocationRequested;
    protected boolean permissionScanRequested;
    protected boolean permissionConnectRequested;
    protected ActivityResultLauncher launcher;

    protected BluetoothAdapter bluetoothAdapter;
    protected BluetoothLeScanner scanner;
    protected UUID serviceUUID;
    protected UUID characteristicUUID;
    protected ActivityAddingDevicesBinding binding;

    protected DatabaseAdapterForDevices dbDeviceAdapter;

    protected final HashMap<String, BluetoothGatt> gatts = BluetoothGattsContainer.getGatts();

    protected void setLvAdapterConnectedDevices(IListViewAdapterForDevices adapter){
        lvAdapterConnectedDevices = adapter;
    }

    protected void setLvAdapterFoundDevices(IListViewAdapterForDevices adapter){
        lvAdapterFoundDevices = adapter;
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
                result -> {checkBluetoothPeripherals();});

        serviceUUID = UUID.fromString(getResources().getString(R.string.service_uuid));
        characteristicUUID = UUID.fromString(getString(R.string.characteristic_uuid));
    }

    public boolean checkBluetoothPeripherals(){
        if (bluetoothAdapter == null) return false;
        if (bluetoothAdapter.isEnabled()
                && checkPermissionLocation()
                && checkLocation()
                && checkPermissionBLE_SCAN()
                && checkPermissionBLE_CONNECT()) return true;
        if (!bluetoothAdapter.isEnabled() && !bluetoothRequested) {
            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            launcher.launch(intent);
            bluetoothRequested = true;
            return false;
        }

        if (bluetoothAdapter.isEnabled() && !permissionLocationRequested && !checkPermissionLocation()) {
            permissionLocationRequested = true;
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            return false;
        }

        if (bluetoothAdapter.isEnabled() && !permissionScanRequested && !checkPermissionBLE_SCAN()){
            requestPermissions(new String[]{Manifest.permission.BLUETOOTH_SCAN}, 2);
            permissionScanRequested = true;
            return false;
        }

        if (bluetoothAdapter.isEnabled() && !permissionConnectRequested && !checkPermissionBLE_CONNECT()){
            requestPermissions(new String[]{Manifest.permission.BLUETOOTH_CONNECT}, 3);
            permissionConnectRequested = true;
            return false;
        }

        if (bluetoothAdapter.isEnabled() && !locationRequested && !checkLocation()){
            Intent enableLocationIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            launcher.launch(enableLocationIntent);
            locationRequested = true;
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
        if (bluetoothAdapter.isEnabled()) {
            scanner = bluetoothAdapter.getBluetoothLeScanner();
            ScanFilter filter = new ScanFilter.Builder()
                    .setServiceUuid(new ParcelUuid(serviceUUID)).build();
            ScanSettings settings = new ScanSettings.Builder()
                    .setScanMode(ScanSettings.SCAN_MODE_LOW_POWER).build();
            ArrayList<ScanFilter> filters = new ArrayList<>();
            filters.add(filter);
            scanner.startScan(filters, settings, scanCallback);
            if (BuildConfig.DEBUG) Log.v("APP_TAG", "Start scan!");
        }
        Log.v("APP_TAG22", "gatts size: " + gatts.size());
    }

    @SuppressLint("MissingPermission")
    protected void stopLEScan(){
        if (scanner == null) return;
        scanner.stopScan(scanCallback);
    }

    @SuppressLint("MissingPermission")
    protected void startLEScan(String deviceAddress){
        if (!checkBluetoothPeripherals()) return;
        if (bluetoothAdapter.isEnabled()) {
            scanner = bluetoothAdapter.getBluetoothLeScanner();
            ScanFilter filter = new ScanFilter.Builder().setDeviceAddress(deviceAddress).build();
            ScanSettings settings = new ScanSettings.Builder()
                    .setScanMode(ScanSettings.SCAN_MODE_LOW_POWER).build();
            ArrayList<ScanFilter> filters = new ArrayList<>();
            filters.add(filter);
            scanner.startScan(filters, settings, scanCallback);
            if (BuildConfig.DEBUG) Log.v("APP_TAG", "Start scan!");
        }
        Log.v("APP_TAG22", "gatts size: " + gatts.size());
    }

    @SuppressLint("MissingPermission")
    public void connectDevice(BluetoothDevice device){
        if (!checkBluetoothPeripherals()) return;
        new Thread(() -> {
            device.connectGatt(this, false, gattCallback, TRANSPORT_LE);
        }).start();
    }

    @SuppressLint("MissingPermission")
    public void connectDevice(String deviceAddress){
        connectDevice(bluetoothAdapter.getRemoteDevice(deviceAddress));
    }

    @SuppressLint("MissingPermission")
    public void disconnectDevice(BluetoothGatt gatt){
        if (!checkBluetoothPeripherals() || gatt == null) return;
        if (BuildConfig.DEBUG){
            Log.v("APP_TAG22", "start disconnecting, addr = " + gatt.getDevice().getAddress());
            Log.v("APP_TAG22", "gatt = " + gatt);
        }
        new Thread(() -> gatt.disconnect()).start();
        runOnUiThread(() -> {
            gatts.remove(gatt.getDevice().getAddress());
            if (lvAdapterConnectedDevices != null)
                lvAdapterConnectedDevices.removeDevice(gatt.getDevice());
            Log.v("APP_TAG22", "gatts size: " + gatts.size());
        });
    }

    @SuppressLint("MissingPermission")
    public void disconnectDevice(String deviceAddress){
        disconnectDevice(gatts.get(deviceAddress));
    }

    protected final ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            super.onBatchScanResults(results);
            if (BuildConfig.DEBUG) Log.v("APP_TAG", "batch");
        }

        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            //if (BuildConfig.DEBUG) Log.v("APP_TAG", "onScanResult");
            BluetoothDevice device = result.getDevice();
            if (device != null) {
                if (dbDeviceAdapter != null &&
                        dbDeviceAdapter.getDeviceStateConnection(device.getAddress()) == 1){
                    //connectDevice(device);
                    return;
                }
                if (lvAdapterFoundDevices != null)
                    lvAdapterFoundDevices.addDevice(device);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
                String uuids = "";
                device.fetchUuidsWithSdp();
                try {
                    device.createRfcommSocketToServiceRecord(UUID.randomUUID());
                } catch (IOException e) {
                    e.printStackTrace();
                }
                List<ParcelUuid> uuids2 = result.getScanRecord().getServiceUuids();
                if (uuids2 != null)
                    for (ParcelUuid uuid : uuids2) uuids += uuid + " ";
                //if (BuildConfig.DEBUG) Log.v("APP_TAG", uuids + " " + device.getAddress());
            }
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
                        runOnUiThread(() -> gatts.put(gatt.getDevice().getAddress(), gatt));
                    runOnUiThread(() -> {
                        if (lvAdapterConnectedDevices == null) return;
                        if (lvAdapterFoundDevices == null) return;
                        if (lvAdapterFoundDevices.removeDevice(gatt.getDevice()))
                            lvAdapterConnectedDevices.addDevice(gatt.getDevice());
                        else
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
                    gatt.close();
                    Log.v("APP_TAG22", "gatt is closed");
                    Log.v("APP_TAG22", "gatt = " + gatt);
                }
            }
            else if (status == 8){
                if (BuildConfig.DEBUG) Log.e("APP_TAG", "Error. status = " + status);
                runOnUiThread(() -> {
                    //gatts.remove(gatt.getDevice().getAddress());
                    if (lvAdapterConnectedDevices != null)
                        lvAdapterConnectedDevices.setAvailability(false, gatt.getDevice());
                });
                //gatt.close();
            }
            else {
                if (BuildConfig.DEBUG) Log.e("APP_TAG", "Error. status = " + status);
                runOnUiThread(() -> {
                    gatts.remove(gatt.getDevice().getAddress());
                    if (lvAdapterConnectedDevices != null)
                        lvAdapterConnectedDevices.setAvailability(false, gatt.getDevice());
                });
                gatt.close();
            }
        }
    };

    @SuppressLint("MissingPermission")
    public boolean writeCharacteristic(String deviceAddress, String message){
        BluetoothGatt bluetoothGatt = gatts.get(deviceAddress);
        if (bluetoothGatt == null) return false;
        BluetoothGattService service = bluetoothGatt.getService(serviceUUID);
        BluetoothGattCharacteristic characteristic = service.getCharacteristic(characteristicUUID);

        /*if((characteristic.getProperties() & PROPERTY_WRITE_NO_RESPONSE) == 0 ) {
            Log.e("APP_TAG22", "proterties " + (characteristic.getProperties() & PROPERTY_WRITE_NO_RESPONSE));
            Log.e("APP_TAG22", "ERROR: Characteristic does not support writeType '" + characteristic.getWriteType() + "'");
            return false;
        }*/
        characteristic.setWriteType(WRITE_TYPE_NO_RESPONSE);
        characteristic.setValue(message);
        if (!bluetoothGatt.writeCharacteristic(characteristic)) return false;
        return true;
    }

    @Override
    protected void onDestroy(){
        super.onDestroy();
        //lv_connected_devices_adapter.allDisconnect();
    }
}
