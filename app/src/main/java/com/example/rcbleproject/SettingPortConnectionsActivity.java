package com.example.rcbleproject;

import static com.example.rcbleproject.Container.currDisIdxKey;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.example.rcbleproject.Database.DatabaseAdapterPortConnections;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

public class SettingPortConnectionsActivity extends BaseAppBluetoothActivity {
    private int currentDisplayIndex, numOfDisplays;
    private ArrayList<PortConnectionsAdapter> portConnectionsByDisplays;
    private ListView lv_controlled_ports;
    private long profileID = -1;
    private BaseAppActivity context;
    private DatabaseAdapterPortConnections dbPortConnections;

    // доступные порты хабов на каждом дисплее
    private static TreeMap<Long, TreeMap<String, List<Port>>> hubPortsByDisplays = new TreeMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting_controlled_ports);
        context = this;
        ((TextView)findViewById(R.id.tv_label)).setText(getString(R.string.controlled_ports));
        findViewById(R.id.ll_displays_control).setVisibility(View.VISIBLE);
        findViewById(R.id.bt_back).setOnClickListener((View v) -> finish());
        findViewById(R.id.bt_add_device).setOnClickListener((View v) -> {
            Intent intent = new Intent(this, AddingHubsActivity.class);
            startActivity(intent);
        });
        
        dbHubsAdapter = Container.getDbForHubs(this);
        dbPortConnections = Container.getDbPortConnections(this);

        //long currentDisplayID = getIntent().getLongExtra("display_id", -1);
        profileID = getIntent().getLongExtra("profile_id", -1);
        numOfDisplays = getIntent().getIntExtra("number_of_displays", -1);

        lv_controlled_ports = findViewById(R.id.lv_controlled_ports);
        //setLvAdapterConnectedDevices(portConnectionsByDisplays.get(currentDisplayIndex).getConnectedDevicesAdapter());

        Button btAddControlledPort = findViewById(R.id.bt_add_controlled_port);
        btAddControlledPort.setOnClickListener((View v) -> {
            PortConnectionsAdapter adapter = (PortConnectionsAdapter) lv_controlled_ports.getAdapter();
            long disId = GameControllersDrawer.getDisplayIDs().get(currentDisplayIndex);
            long id = dbPortConnections.insert(disId);
            adapter.portConnections.add(new PortConnection(id, disId));
            adapter.notifyDataSetChanged();
            //Log.v("APP_TAG555", "" + adapter.portConnections.size());
        });
        findViewById(R.id.bt_last).setOnClickListener((View v) -> prevDisplay());
        findViewById(R.id.bt_next).setOnClickListener((View v) -> nextDisplay());
    }

    //TODO перевести алгоритм метода в фоновой ассинхронный режим!
    private void initPortConnectionsByDisplays(SettingPortConnectionsActivity context){
        ArrayList<Long> displayIDs = GameControllersDrawer.getDisplayIDs();
        portConnectionsByDisplays = new ArrayList<>(numOfDisplays);
        /*List<List<PortConnection>> portConnections = Container.getDbPortConnections(this)
                .getPortConnectionsByProfileID(profileID, context);*/
        for (int i = 0; i < numOfDisplays; ++i){
            TreeMap<String, List<Port>> hubPorts = new TreeMap<>();
            for (BluetoothHub hub : dbHubsAdapter.getAllHubs(this).values())
                hubPorts.put(hub.address, hub.getPorts(this));
            hubPortsByDisplays.put(displayIDs.get(i), hubPorts);

            List<PortConnection> portConnections = Container.getDbPortConnections(this)
                    .getPortConnectionsByDisplayID(displayIDs.get(i), this);
            for (PortConnection portConnection : portConnections){
                if (portConnection.hub == null || portConnection.port == null) continue;
                List<Port> ports = hubPorts.get(portConnection.hub.address);
                if (ports == null) continue;
                for (short idx = (short) (ports.size() - 1); idx >= 0; --idx){
                    if (ports.get(idx).portNum == portConnection.port.portNum){
                        ports.remove(idx);
                    }
                }
                ports.remove(portConnection.port);
            }

            portConnectionsByDisplays.add(new PortConnectionsAdapter(context, displayIDs.get(i), i,
                    numOfDisplays, portConnections));
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

    @Override
    public void notifyDataSetChanged(){
        ((PortConnectionsAdapter)lv_controlled_ports.getAdapter()).notifyDataSetChanged();
    }

    @Override
    protected void onResume(){
        super.onResume();
        SharedPreferences preferences = getSharedPreferences(currDisIdxKey, Context.MODE_PRIVATE);
        currentDisplayIndex = preferences.getInt("current_display_index_"+profileID, 0);
        initPortConnectionsByDisplays(this);
        lv_controlled_ports.setAdapter(portConnectionsByDisplays.get(currentDisplayIndex));
        showCurrentDisplayNum(currentDisplayIndex, numOfDisplays);
        notifyDataSetChanged();
    }

    @SuppressLint("ApplySharedPref")
    @Override
    protected void onPause(){
        super.onPause();
        SharedPreferences preferences = getSharedPreferences(currDisIdxKey, Context.MODE_PRIVATE);
        preferences.edit().putInt("current_display_index_"+profileID, currentDisplayIndex).commit();
        savePortConnections();
    }

    private void savePortConnections(){
        for (PortConnectionsAdapter adapter : portConnectionsByDisplays){
            for (PortConnection portConn : adapter.getPortConnections()){
                Container.getDbPortConnections(context).update(portConn);
            }
        }

    }

    public TreeMap<Long, TreeMap<String, List<Port>>> getHubPortsByDisplays() {
        return hubPortsByDisplays;
    }
}