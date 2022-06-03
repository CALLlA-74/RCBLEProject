package com.example.rcbleproject;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

public class SettingControlledPortsActivity extends BaseAppBluetoothActivity {

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
    }

    @Override
    protected void onResume(){
        super.onResume();
        if (!checkBluetoothPeripherals()) return;
    }
}