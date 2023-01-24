package com.example.rcbleproject;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.example.rcbleproject.Database.DatabaseAdapterControlledPorts;
import com.example.rcbleproject.Database.DatabaseAdapterForHubs;

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
            Intent intent = new Intent(this, AddingHubsActivity.class);
            startActivity(intent);
        });
        
        dbHubsAdapter = Container.getDbForHubs(this);
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

    private ArrayList<BluetoothHub> loadDevicesFromDB(){
        Cursor cursor = dbHubsAdapter.getConnectedHubs_cursor();
        ArrayList<BluetoothHub> connectedDevices = new ArrayList<>();
        if (cursor == null || !cursor.moveToFirst()) return connectedDevices;
        while (cursor.moveToNext()){
            String address = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseAdapterForHubs.HUB_ADDRESS));
            long id = cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseAdapterForHubs.ID));
            BluetoothHub hub = new BluetoothHub(id, this);
            if (!gatts.containsKey(address))
                connectDevice(address);
            connectedDevices.add(hub);
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