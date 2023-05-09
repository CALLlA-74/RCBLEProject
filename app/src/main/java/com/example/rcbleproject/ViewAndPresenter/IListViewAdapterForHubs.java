package com.example.rcbleproject.ViewAndPresenter;

import android.bluetooth.BluetoothDevice;

import com.example.rcbleproject.Model.BluetoothHub;


public interface IListViewAdapterForHubs {
    boolean addHub(BluetoothHub hub);
    BluetoothHub removeHub(String address);
    boolean setAvailability(boolean flag, BluetoothDevice device);
}
