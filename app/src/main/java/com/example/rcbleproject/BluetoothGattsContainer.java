package com.example.rcbleproject;

import android.bluetooth.BluetoothGatt;

import java.util.HashMap;

public class BluetoothGattsContainer {
    private static HashMap<String, BluetoothGatt> gatts = null;

    public static HashMap<String, BluetoothGatt> getGatts(){
        if (gatts == null) gatts = new HashMap<>();
        return gatts;
    }
}
