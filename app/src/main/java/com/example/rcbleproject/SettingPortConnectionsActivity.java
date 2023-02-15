package com.example.rcbleproject;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;

public class SettingPortConnectionsActivity extends BaseAppBluetoothActivity {
    private int currentDisplayIndex, numOfDisplays;
    private ArrayList<PortConnectionsAdapter> portConnectionsByDisplays;
    private ListView lv_controlled_ports;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting_controlled_ports);
        ((TextView)findViewById(R.id.tv_label)).setText(getString(R.string.controlled_ports));
        findViewById(R.id.ll_displays_control).setVisibility(View.VISIBLE);
        findViewById(R.id.bt_back).setOnClickListener((View v) -> finish());
        findViewById(R.id.bt_add_device).setOnClickListener((View v) -> {
            Intent intent = new Intent(this, AddingHubsActivity.class);
            startActivity(intent);
        });
        
        dbHubsAdapter = Container.getDbForHubs(this);

        //long currentDisplayID = getIntent().getLongExtra("display_id", -1);
        currentDisplayIndex = getIntent().getIntExtra("display_index", -1);
        numOfDisplays = getIntent().getIntExtra("number_of_displays", -1);

        initPortConnectionsByDisplays(this);
        lv_controlled_ports = findViewById(R.id.lv_controlled_ports);
        lv_controlled_ports.setAdapter(portConnectionsByDisplays.get(currentDisplayIndex));
        setLvAdapterConnectedDevices(portConnectionsByDisplays.get(currentDisplayIndex).getConnectedDevicesAdapter());

        Button btAddControlledPort = findViewById(R.id.bt_add_controlled_port);
        btAddControlledPort.setOnClickListener((View v) -> {
            PortConnectionsAdapter adapter = (PortConnectionsAdapter) lv_controlled_ports.getAdapter();
            adapter.portConnections.add(new PortConnection());
            adapter.notifyDataSetChanged();
            Log.v("APP_TAG555", "" + adapter.portConnections.size());
        });
        showCurrentDisplayNum(currentDisplayIndex, numOfDisplays);
        findViewById(R.id.bt_last).setOnClickListener((View v) -> prevDisplay());
        findViewById(R.id.bt_next).setOnClickListener((View v) -> nextDisplay());
    }

    private void initPortConnectionsByDisplays(SettingPortConnectionsActivity context){
        ArrayList<Long> displayIDs = GameControllersDrawer.getDisplayIDs();
        portConnectionsByDisplays = new ArrayList<>(numOfDisplays);
        for (int i = 0; i < numOfDisplays; ++i){
            portConnectionsByDisplays.add(new PortConnectionsAdapter(context, displayIDs.get(i), i,
                    numOfDisplays, Container.getDbPortConnections(this).getPortConnectionsByDisplayID(displayIDs.get(i))));
        }
    }

    public void nextDisplay(){
        currentDisplayIndex++;
        if (currentDisplayIndex >= numOfDisplays) currentDisplayIndex = 0;
        showCurrentDisplayNum(currentDisplayIndex, numOfDisplays);
        lv_controlled_ports.setAdapter(portConnectionsByDisplays.get(currentDisplayIndex));
        portConnectionsByDisplays.get(currentDisplayIndex).notifyDataSetChanged();
    }

    public void prevDisplay(){
        currentDisplayIndex--;
        if (currentDisplayIndex < 0) currentDisplayIndex = numOfDisplays - 1;
        showCurrentDisplayNum(currentDisplayIndex, numOfDisplays);
        lv_controlled_ports.setAdapter(portConnectionsByDisplays.get(currentDisplayIndex));
        portConnectionsByDisplays.get(currentDisplayIndex).notifyDataSetChanged();
    }

    public void notifyDataSetChanged(){
        ((PortConnectionsAdapter)lv_controlled_ports.getAdapter()).notifyDataSetChanged();
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