package com.example.rcbleproject;

import android.bluetooth.BluetoothDevice;

public interface IListViewAdapterForDevices {
    boolean addDevice(BluetoothDevice device);
    boolean removeDevice(BluetoothDevice device);
    boolean setAvailability(boolean flag, BluetoothDevice device);
}
