package com.example.rcbleproject;

import android.annotation.SuppressLint;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;

import com.example.rcbleproject.databinding.ActivityAddingDevicesBinding;

import java.util.ArrayList;

public class AddingHubsActivity extends BaseAppBluetoothActivity implements IRemovableHub {
    protected ConnectedDevicesAdapter lv_connected_devices_adapter;
    protected FoundDevicesAdapter devicesAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAddingDevicesBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.tbActivityAddDevices.getRoot());
        setFullscreenMode(binding.layoutContent);
        ((TextView) findViewById(R.id.tv_label)).setText(getResources().getString(R.string.hubs));
        findViewById(R.id.bt_back).setOnClickListener((View v) -> {
            finish();
        });
        findViewById(R.id.bt_add_device).setVisibility(View.GONE);

        dbHubsAdapter = Container.getDbForHubs(this);

        @SuppressLint("ResourceType") int maxNumOfHubs = getResources().getInteger(R.integer.maxNumOfHubs);
        devicesAdapter = new FoundDevicesAdapter(this, new ArrayList<>(maxNumOfHubs));
        ListView lv_found_devices = binding.lvFoundDevices;
        lv_found_devices.setAdapter(devicesAdapter);

        ListView lv_connected_devices = binding.lvConnectedDevices;
        lv_connected_devices_adapter = new ConnectedDevicesAdapter(this, dbHubsAdapter);
        lv_connected_devices.setAdapter(lv_connected_devices_adapter);
        lv_connected_devices.setOnItemLongClickListener(
                (AdapterView<?> parent, View view, int position, long id) -> {
                    if (lv_connected_devices_adapter.getAvailability(view))
                        lv_connected_devices_adapter.setFocusOnEditText(view);
                    return false;
        });
        lv_connected_devices.setOnItemClickListener(
                (AdapterView<?> parent, View view, int position, long id) -> {
                    lv_connected_devices_adapter.cancelEdit();
        });
        setLvAdapterConnectedDevices(lv_connected_devices_adapter);
        setLvAdapterFoundHubs(devicesAdapter);
    }

    public void notifyNoHubConnection(){
        Toast.makeText(this, "Нет соединения с хабом!", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        setFullscreenMode(binding.layoutContent);
    }

    @Override
    public void remove(String address) {
        setFullscreenMode(binding.layoutContent);
        if (BuildConfig.DEBUG) Log.v("APP_TAG22", "try to disconn device. addr = " + address);
        disconnectDevice(gatts.get(address));
    }

    @Override
    public void cancel(){setFullscreenMode(binding.layoutContent);}

    public void setFullscreenMode(){ setFullscreenMode(binding.layoutContent); }
}