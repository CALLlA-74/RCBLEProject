package com.example.rcbleproject;

import static android.bluetooth.BluetoothDevice.BOND_BONDED;
import static android.bluetooth.BluetoothDevice.BOND_BONDING;
import static android.bluetooth.BluetoothDevice.TRANSPORT_LE;
import static android.bluetooth.BluetoothGatt.GATT_SUCCESS;

import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.os.ParcelUuid;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import com.example.rcbleproject.Database.DatabaseAdapterForDevices;
import com.example.rcbleproject.databinding.ActivityAddDevicesBinding;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class AddDevicesActivity extends BaseAppBluetoothActivity implements Removable {

    private BluetoothLeScanner scanner;
    private UUID ServiceUUID;
    private FoundDevicesAdapter devicesAdapter;
    private DatabaseAdapterForDevices dbDeviceAdapter;
    private ConnectedDevicesAdapter lv_connected_devices_adapter;
    private ActivityAddDevicesBinding binding;

    private final HashMap<String, BluetoothGatt> gatts = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAddDevicesBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(findViewById(R.id.tb_activity_profiles));
        setFullscreenMode(binding.layoutContent);
        ((TextView) findViewById(R.id.tv_label)).setText(getResources().getString(R.string.devices));

        ServiceUUID = UUID.fromString(getResources().getString(R.string.service_uuid));
        devicesAdapter = new FoundDevicesAdapter(this, R.layout.item_found_device, new ArrayList<>());
        ListView lv_found_devices = binding.lvFoundDevices;
        lv_found_devices.setAdapter(devicesAdapter);
        ListView lv_connected_devices = binding.lvConnectedDevices;
        dbDeviceAdapter = new DatabaseAdapterForDevices(this);
        dbDeviceAdapter.open();
        lv_connected_devices_adapter = new ConnectedDevicesAdapter(this,
                                                                          R.layout.app_list_item,
                                                                          dbDeviceAdapter);
        lv_connected_devices.setAdapter(lv_connected_devices_adapter);
        lv_connected_devices.setOnItemLongClickListener(
                (AdapterView<?> parent, View view, int position, long id) -> {
                    lv_connected_devices_adapter.setFocusOnEditText(view);
                    return false;
        });
    }

    @SuppressLint("MissingPermission")
    @Override
    protected void onResume() {
        super.onResume();
        setFullscreenMode(binding.layoutContent);
        if (!checkBluetoothPeripherals()) return;
        if (bluetoothAdapter.isEnabled()) {
            scanner = bluetoothAdapter.getBluetoothLeScanner();
            ScanFilter filter = new ScanFilter.Builder()
                    /*.setServiceUuid(new ParcelUuid(ServiceUUID))*/.build();
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
    @Override
    public void onPause() {
        super.onPause();
        if (scanner == null) return;
        scanner.stopScan(scanCallback);
        //lv_connected_devices_adapter.allDisconnect();
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
        if (!checkBluetoothPeripherals()) return;
        Log.v("APP_TAG22", "start disconnecting");
        new Thread(() -> gatt.disconnect()).start();
    }

    @SuppressLint("MissingPermission")
    public void disconnectDevice(String deviceAddress){
        disconnectDevice(gatts.get(deviceAddress));
    }

    private final ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            super.onBatchScanResults(results);
            if (BuildConfig.DEBUG) Log.v("APP_TAG", "batch");
        }

        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            if (BuildConfig.DEBUG) Log.v("APP_TAG", "onScanResult");
            BluetoothDevice device = result.getDevice();
            if (device != null) {
                if (dbDeviceAdapter.getDeviceStateConnection(device.getAddress()) == 1){
                    connectDevice(device);
                    return;
                }
                devicesAdapter.addDevice(device);
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
                if (BuildConfig.DEBUG) Log.v("APP_TAG", uuids + " " + device.getAddress());
            }
        }
    };

    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @SuppressLint("MissingPermission")
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            Log.v("APP_TAG2", "onConnectionStateChange " + gatt.getDevice().getAddress() +
                    ". status: " + status + "; newState: " + newState + "; bondState: " +
                    gatt.getDevice().getBondState());
            if (status == GATT_SUCCESS){
                Log.v("APP_TAG2", "GATT_SUCCESS");
                if (newState == BluetoothProfile.STATE_CONNECTED){
                    int bondState = gatt.getDevice().getBondState();
                    Log.v("APP_TAG2", "STATE_CONNECTED. bondState: " + bondState);
                    int delay = 0;
                    switch (bondState){
                        case BOND_BONDED:
                            Log.v("APP_TAG2", "is BOND_BONDED");
                            delay = Build.VERSION.SDK_INT <= Build.VERSION_CODES.N? 1000 : 0;
                            break;
                        case BOND_BONDING:
                            Log.v("APP_TAG2", "is Bonding");
                            return;
                    }

                    Log.v("APP_TAG2", "check gatts and db");
                    if (!gatts.containsKey(gatt.getDevice().getAddress()))
                        runOnUiThread(() -> gatts.put(gatt.getDevice().getAddress(), gatt));
                    runOnUiThread(() -> {
                        if (devicesAdapter.removeDevice(gatt.getDevice()))
                            lv_connected_devices_adapter.addDevice(gatt.getDevice());
                        else
                            lv_connected_devices_adapter.setAvailability(true, gatt.getDevice());
                    });
                    Log.v("APP_TAG2", "finish to check");
                    /*new Handler().postDelayed(() -> {
                        if (BuildConfig.DEBUG) Log.v("APP_TAG2", "Discover services");
                        boolean res = gatt.discoverServices();
                        if (!res && BuildConfig.DEBUG)
                            Log.v("APP_TAG2", "Discovering services was failed");
                            }, delay);*/
                }
                else if (newState == BluetoothProfile.STATE_DISCONNECTED){
                    gatt.close();
                    runOnUiThread(() -> {
                        gatts.remove(gatt.getDevice().getAddress());
                        lv_connected_devices_adapter.removeDevice(gatt.getDevice().getAddress());
                        Log.v("APP_TAG22", "gatts size: " + gatts.size());
                    });
                }
            }
            else {
                if (BuildConfig.DEBUG) Log.e("APP_TAG", "Error. status = " + status);
                gatt.close();
                runOnUiThread(() -> {
                    gatts.remove(gatt.getDevice().getAddress());
                    lv_connected_devices_adapter.setAvailability(false, gatt.getDevice());
                });
            }
        }
    };

    @Override
    public void onDestroy(){
        super.onDestroy();
        dbDeviceAdapter.close();
        //lv_connected_devices_adapter.allDisconnect();
    }

    @Override
    public void remove(long id) {
        setFullscreenMode(binding.layoutContent);
        Cursor c = dbDeviceAdapter.getDeviceById_cursor(id);
        if (!c.moveToFirst()) return;
        disconnectDevice(gatts.get(c.getString(c.getColumnIndexOrThrow(
                dbDeviceAdapter.DEVICE_ADDRESS))));
    }

    @Override
    public void cancel(){setFullscreenMode(binding.layoutContent);}

    public void setFullscreenMode(){
        setFullscreenMode(binding.layoutContent);
    }
}