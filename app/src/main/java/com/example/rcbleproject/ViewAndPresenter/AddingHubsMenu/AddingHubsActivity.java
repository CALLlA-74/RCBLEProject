package com.example.rcbleproject.ViewAndPresenter.AddingHubsMenu;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.rcbleproject.ViewAndPresenter.BluetoothLeService;
import com.example.rcbleproject.BuildConfig;
import com.example.rcbleproject.Container;
import com.example.rcbleproject.R;
import com.example.rcbleproject.databinding.ActivityAddingHubsBinding;

import java.util.ArrayList;

public class AddingHubsActivity extends BluetoothLeService implements IRemovableHub {
    protected ConnectedDevicesAdapter lv_connected_devices_adapter;
    protected FoundDevicesAdapter devicesAdapter;

    private ImageView ivRadarImage;
    private View incEmptyListCnnctdHubsLbl,
                 incEmptyListFndHubsLbl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAddingHubsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.tbActivityAddDevices.getRoot());
        setFullscreenMode(binding.layoutContent);
        ((TextView) findViewById(R.id.tv_label)).setText(getResources().getString(R.string.hubs));
        findViewById(R.id.bt_back).setOnClickListener((View v) -> {
            finish();
        });
        initBtForSearchingHubs();

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

    public void initIncEmptyListFndHubsLbl(){
        incEmptyListFndHubsLbl = findViewById(R.id.inc_empty_list_fnd_hubs);
        incEmptyListFndHubsLbl.setVisibility(View.VISIBLE);
        Button btStartLEScan = incEmptyListFndHubsLbl.findViewById(R.id.bt_empty_list);
        btStartLEScan.setText(R.string.start_searching_for_hubs);
        btStartLEScan.setOnClickListener(v -> startLEScan());
    }

    public void hideIncEmptyListFndHubsLblVisibility(){
        incEmptyListFndHubsLbl = findViewById(R.id.inc_empty_list_fnd_hubs);
        incEmptyListFndHubsLbl.setVisibility(View.GONE);
    }

    public void initIncEmptyListCnnctdHubsLblVisibility(){
        incEmptyListCnnctdHubsLbl = findViewById(R.id.inc_empty_list_cnnctd_hubs);
        incEmptyListCnnctdHubsLbl.setVisibility(View.VISIBLE);

        incEmptyListCnnctdHubsLbl.findViewById(R.id.bt_empty_list).setVisibility(View.GONE);
        TextView tvEmptyList = incEmptyListCnnctdHubsLbl.findViewById(R.id.tv_msg_empty_list);
        tvEmptyList.setText(R.string.empty_hubs_list);
    }

    public void hideIncEmptyListCnnctdHubsLblVisibility(){
        incEmptyListCnnctdHubsLbl = findViewById(R.id.inc_empty_list_cnnctd_hubs);
        incEmptyListCnnctdHubsLbl.setVisibility(View.GONE);
    }

    public void notifyNoHubConnection(){
        Toast.makeText(this, getString(R.string.no_hub_connection), Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        setFullscreenMode(binding.layoutContent);
    }

    private void initBtForSearchingHubs(){
        ImageButton ibAddDevice = findViewById(R.id.bt_add_device);
        ibAddDevice.setImageDrawable(null);
        ibAddDevice.setOnClickListener(v -> {
            if (!isLeScanStarted) startLEScan();
            else stopLEScan();
        });

        ivRadarImage = findViewById(R.id.iv_search_device);
        ivRadarImage.setVisibility(View.VISIBLE);
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    @Override
    public void startLEScan(){
        super.startLEScan();
        if (!isLeScanStarted) return;
        incEmptyListFndHubsLbl.findViewById(R.id.bt_empty_list).setVisibility(View.INVISIBLE);
        TextView tvEmptyListHubs = incEmptyListFndHubsLbl.findViewById(R.id.tv_msg_empty_list);
        tvEmptyListHubs.setText(R.string.searching_for_hubs);
        ivRadarImage.startAnimation(AnimationUtils.loadAnimation(this, R.anim.rotate));
        ivRadarImage.getAnimation().setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {}

            @Override
            public void onAnimationEnd(Animation animation) {
                stopLEScan();
            }

            @Override
            public void onAnimationRepeat(Animation animation) {}
        });
    }

    @Override
    public void stopLEScan(){
        super.stopLEScan();
        incEmptyListFndHubsLbl.findViewById(R.id.bt_empty_list).setVisibility(View.VISIBLE);
        TextView tvEmptyListHubs = incEmptyListFndHubsLbl.findViewById(R.id.tv_msg_empty_list);
        tvEmptyListHubs.setText(R.string.no_found_hubs);
        ivRadarImage.clearAnimation();
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