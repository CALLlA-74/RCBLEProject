package com.example.rcbleproject.ViewAndPresenter;

import android.bluetooth.BluetoothDevice;

import com.example.rcbleproject.Model.BluetoothHub;


public interface IListViewAdapterForHubs {
    boolean addHub(BluetoothHub hub);
    BluetoothHub removeHub(String address);
    void setAvailability(boolean flag, BluetoothDevice device);
}
