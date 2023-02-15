package com.example.rcbleproject;

import android.bluetooth.BluetoothDevice;


public interface IListViewAdapterForHubs {
    boolean addHub(BluetoothHub hub);
    BluetoothHub removeHub(String address);
    boolean setAvailability(boolean flag, BluetoothDevice device);
}
