package com.example.rcbleproject;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.example.rcbleproject.Database.DatabaseAdapterControlledPorts;
import com.example.rcbleproject.Database.DatabaseAdapterForDevices;

import java.util.ArrayList;

public class SettingControlledPortsActivity extends BaseAppBluetoothActivity {
    private DatabaseAdapterControlledPorts dbControlledPorts;
    private long profileID,
                 displayID,
                 elementID;
    private int axisNum;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting_controlled_ports);
        ((TextView)findViewById(R.id.tv_label)).setText(getString(R.string.controlled_ports));
        findViewById(R.id.bt_back).setOnClickListener((View v) -> finish());
        findViewById(R.id.bt_add_device).setOnClickListener((View v) -> {
            Intent intent = new Intent(this, AddingDevicesActivity.class);
            startActivity(intent);
        });

        dbDeviceAdapter = Container.getDbForDevices(this);
        dbControlledPorts = Container.getDbControlledPorts(this);

        profileID = getIntent().getLongExtra("profile_id", -1);
        displayID = getIntent().getLongExtra("display_id", -1);
        elementID = getIntent().getLongExtra("element_id", -1);
        axisNum = getIntent().getIntExtra("axis_num", -1);

        ArrayList<ControlledPort> controlledPorts = dbControlledPorts
                .getControlledPortsByElementIDAndAxisNum(elementID, axisNum);
        ControlledPortsAdapter controlledPortsAdapter = new ControlledPortsAdapter(this,
                controlledPorts, loadDevicesFromDB());
        ListView lv_controlled_ports = findViewById(R.id.lv_controlled_ports);
        lv_controlled_ports.setAdapter(controlledPortsAdapter);
        setLvAdapterConnectedDevices(controlledPortsAdapter.getConnectedDevicesAdapter());

        Button btAddControlledPort = findViewById(R.id.bt_add_controlled_port);
        btAddControlledPort.setOnClickListener((View v) -> {
            controlledPortsAdapter.add(new ControlledPort());
            controlledPortsAdapter.notifyDataSetChanged();
        });
    }

    private ArrayList<BluetoothDeviceApp> loadDevicesFromDB(){
        Cursor cursor = dbDeviceAdapter.getConnectedDevices_cursor();
        ArrayList<BluetoothDeviceApp> connectedDevices = new ArrayList<>();
        while (cursor.moveToNext()){
            String address = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseAdapterForDevices.DEVICE_ADDRESS));
            BluetoothDeviceApp deviceApp = new BluetoothDeviceApp(bluetoothAdapter.getRemoteDevice(address));
            deviceApp.name = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseAdapterForDevices.DEVICE_NAME));
            if (!gatts.containsKey(address))
                connectDevice(address);
            connectedDevices.add(deviceApp);
        }
        return connectedDevices;
    }

    @Override
    protected void onResume(){
        super.onResume();
        checkBluetoothPeripherals();
        startLEScan();
    }

    @Override
    protected void onPause(){
        super.onPause();
        stopLEScan();
    }
}