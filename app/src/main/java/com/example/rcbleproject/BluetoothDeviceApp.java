package com.example.rcbleproject;

import android.bluetooth.BluetoothDevice;

public class BluetoothDeviceApp {
    private final BluetoothDevice device;
    public long lastTimeAdv;
    public volatile boolean isActive = true;
    public String name = "";

    public BluetoothDeviceApp(BluetoothDevice device){
        this.device = device;
    }

    public BluetoothDevice getDevice(){ return device; }
}
