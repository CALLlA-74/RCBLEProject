package com.example.rcbleproject;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

public class BaseAppBluetoothActivity extends BaseAppActivity{
    protected boolean bluetoothRequested;
    protected boolean locationRequested;
    protected boolean permissionLocationRequested;
    protected boolean permissionScanRequested;
    protected boolean permissionConnectRequested;
    protected ActivityResultLauncher launcher;


    protected BluetoothAdapter bluetoothAdapter;

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
}
