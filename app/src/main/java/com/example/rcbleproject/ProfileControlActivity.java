package com.example.rcbleproject;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.example.rcbleproject.Database.DatabaseAdapterDisplays;
import com.example.rcbleproject.Database.DatabaseAdapterElementsControl;
import com.example.rcbleproject.Database.DatabaseAdapterProfilesControl;
import com.example.rcbleproject.databinding.ActivityProfileControlBinding;

public class ProfileControlActivity extends BaseAppBluetoothActivity implements Removable {
    private DatabaseAdapterElementsControl dbAdapterElementsControl;
    private DatabaseAdapterProfilesControl dbProfiles;
    private DatabaseAdapterDisplays dbDisplays;
    private View controlsView;
    private long profileID;
    private GameControllersDrawer gameControllersDrawer;
    private ActivityProfileControlBinding binding;

    @SuppressLint({"ClickableViewAccessibility", "NonConstantResourceId"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        profileID = getIntent().getLongExtra("profile_id", 0);
        Log.v("APP_TAG3", "id: " + profileID);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN
                | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);

        binding = ActivityProfileControlBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        controlsView = binding.fullscreenContentControls;

        dbProfiles = new DatabaseAdapterProfilesControl(this);
        dbProfiles.open();

        dbAdapterElementsControl = new DatabaseAdapterElementsControl(this);
        dbAdapterElementsControl.open();

        dbDisplays = new DatabaseAdapterDisplays(this);
        dbDisplays.open();

        DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getRealMetrics(dm);
        gameControllersDrawer = new GameControllersDrawer(this, dm,
                dbAdapterElementsControl, dbProfiles, dbDisplays, profileID);
        binding.svGameControllers.getHolder().addCallback(gameControllersDrawer);
        binding.svGameControllers.setOnTouchListener((View v, MotionEvent event) -> {
            gameControllersDrawer.onTouch(v, event);
            return true;
        });

        binding.btBack.setOnClickListener((View v) -> finish());
        binding.btAddElementControl.setOnClickListener((View v) -> {
            Intent intent = new Intent(this, AddingElementControlActivity.class);
            intent.putExtra("display_id", gameControllersDrawer.getCurrentDisplayID());
            //intent.putExtra("screen_number", gameControllersDrawer.getCurrentDisplayIndex());
            intent.putExtra("count_of_elements", gameControllersDrawer.getCountOfElements());
            startActivity(intent);
        });

        binding.dlMenuDrawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
        binding.dlMenuDrawer.setScrimColor(getColor(android.R.color.transparent));
        binding.btProfileControlMenu.setOnClickListener((View v) -> {
            binding.nwMenuProfileControl.getMenu().setGroupVisible(R.id.group_profile_control_menu, true);
            binding.nwMenuProfileControl.getMenu().setGroupVisible(R.id.group_element_control_menu_1, false);
            binding.nwMenuProfileControl.getMenu().setGroupVisible(R.id.group_element_control_menu_2, false);
            binding.dlMenuDrawer.openDrawer(binding.nwMenuProfileControl);
        });

        binding.btElementControlMenu.setOnClickListener((View v) -> {
            Menu menu = binding.nwMenuProfileControl.getMenu();
            TextView tvControlledPorts_1 = menu.findItem(R.id.item_controlled_ports_1).getActionView()
                    .findViewById(R.id.tv_axis_name);
            Button btAddControlledPorts_1 = menu.findItem(R.id.item_controlled_ports_1).getActionView()
                    .findViewById(R.id.bt_add_controlled_ports);

            TextView tvControlledPorts_2 = menu.findItem(R.id.item_controlled_ports_2).getActionView()
                    .findViewById(R.id.tv_axis_name);
            Button btAddControlledPorts_2 = menu.findItem(R.id.item_controlled_ports_2).getActionView()
                    .findViewById(R.id.bt_add_controlled_ports);

            String[] axesNames = gameControllersDrawer.getElementAxesNames();
            tvControlledPorts_1.setText(axesNames[0]);
            btAddControlledPorts_1.setOnClickListener((View v1) -> {
                Intent intent = new Intent(this, SettingControlledPortsActivity.class);
                startActivity(intent);
            });
            if (axesNames.length == 1)
                menu.findItem(R.id.item_controlled_ports_2).setVisible(false);
            else {
                menu.findItem(R.id.item_controlled_ports_2).setVisible(true);
                tvControlledPorts_2.setText(axesNames[1]);
                btAddControlledPorts_2.setOnClickListener((View v2) -> {
                    Intent intent = new Intent(this, SettingControlledPortsActivity.class);
                    startActivity(intent);
                });
            }

            sizeChangerInitOrUpdate(true);
            if (gameControllersDrawer.getElementLocking()){
                binding.nwMenuProfileControl.getMenu().findItem(R.id.item_lock_element_control)
                        .setIcon(R.drawable.baseline_lock_20);
            }
            else {
                binding.nwMenuProfileControl.getMenu().findItem(R.id.item_lock_element_control)
                        .setIcon(R.drawable.baseline_lock_open_20);
            }
            binding.nwMenuProfileControl.getMenu().setGroupVisible(R.id.group_profile_control_menu, false);
            binding.nwMenuProfileControl.getMenu().setGroupVisible(R.id.group_element_control_menu_1, true);
            binding.nwMenuProfileControl.getMenu().setGroupVisible(R.id.group_element_control_menu_2, true);
            binding.dlMenuDrawer.openDrawer(binding.nwMenuProfileControl);
        });

        binding.nwMenuProfileControl.setNavigationItemSelectedListener((item) -> {
            switch (item.getItemId()){
                case R.id.item_alignment_to_grid:
                    if (gameControllersDrawer.getGridVisibility()){
                        gameControllersDrawer.setGridVisibility(false);
                        item.setIcon(R.drawable.baseline_grid_off_20);
                    }
                    else{
                        gameControllersDrawer.setGridVisibility(true);
                        item.setIcon(R.drawable.baseline_grid_on_20);
                    }
                    break;
                case R.id.item_add_display:
                    gameControllersDrawer.addDisplay();
                    menuItemsInit();
                    binding.dlMenuDrawer.closeDrawer(GravityCompat.END);
                    break;
                case R.id.item_remove_display:
                    ConfirmRemoveDialogFragment dialog1 = new ConfirmRemoveDialogFragment();
                    Bundle args1 = new Bundle();
                    args1.putLong("object_id", 0);
                    args1.putString("message", getResources().getString(R.string.confirm_msg_display));
                    dialog1.setArguments(args1);
                    dialog1.show(getSupportFragmentManager(), getResources().getString(R.string.app_name));
                    dialog1.setCancelable(false);
                    binding.dlMenuDrawer.closeDrawer(GravityCompat.END);
                    break;


                case R.id.item_lock_element_control:
                    if (gameControllersDrawer.getElementLocking()) {
                        gameControllersDrawer.setElementLocking(false);
                        binding.nwMenuProfileControl.getMenu().findItem(R.id.item_lock_element_control)
                                .setIcon(R.drawable.baseline_lock_open_20);
                    }
                    else {
                        gameControllersDrawer.setElementLocking(true);
                        binding.nwMenuProfileControl.getMenu().findItem(R.id.item_lock_element_control)
                                .setIcon(R.drawable.baseline_lock_20);
                    }
                    break;
                case R.id.item_remove_element_control:
                    ConfirmRemoveDialogFragment dialog2 = new ConfirmRemoveDialogFragment();
                    Bundle args2 = new Bundle();
                    args2.putLong("object_id", 1);
                    args2.putString("message", getResources().getString(R.string.confirm_msg_element_control));
                    dialog2.setArguments(args2);
                    dialog2.show(getSupportFragmentManager(), getResources().getString(R.string.app_name));
                    dialog2.setCancelable(false);
                    binding.dlMenuDrawer.closeDrawer(GravityCompat.END);
                    break;
            }
            Log.v("APP_TAG2", Integer.valueOf(item.getItemId()).toString());
            return false;
        });

        binding.btNextDisplay.setOnClickListener((View v) -> gameControllersDrawer.nextDisplay());
        binding.btLastDisplay.setOnClickListener((View v) -> gameControllersDrawer.prevDisplay());

        SeekBar elementSizeChanger = binding.nwMenuProfileControl.getMenu().findItem(R.id
                .item_element_size).getActionView().findViewById(R.id.sb_element_size_changer);
        elementSizeChanger.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                                          @Override
                                          public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                                              sizeChangerInitOrUpdate(false);
                                          }

                                          @Override
                                          public void onStartTrackingTouch(SeekBar seekBar) {}

                                          @Override
                                          public void onStopTrackingTouch(SeekBar seekBar) {}
                                      });

    }

    @SuppressLint("SetTextI18n")
    public void showCurrentDisplayNum(int currentDisplayNum, int countOfDisplays){
        binding.tvNumDisplay.setText((currentDisplayNum + 1) + " / " + countOfDisplays);
    }

    private void menuItemsInit(){
        MenuItem item = binding.nwMenuProfileControl.getMenu().findItem(R.id.item_alignment_to_grid);
        if (gameControllersDrawer.getGridVisibility()) item.setIcon(R.drawable.baseline_grid_on_20);
        else item.setIcon(R.drawable.baseline_grid_off_20);

        item = binding.nwMenuProfileControl.getMenu().findItem(R.id.item_add_display);
        if (gameControllersDrawer.getCountOfDisplays() >= 5) item.setEnabled(false);
        else item.setEnabled(true);

        item = binding.nwMenuProfileControl.getMenu().findItem(R.id.item_remove_display);
        if (gameControllersDrawer.getCountOfDisplays() < 2) item.setEnabled(false);
        else item.setEnabled(true);

        Log.v("APP_TAG3", "" + gameControllersDrawer.getCountOfDisplays());
    }

    @SuppressLint({"UseCompatLoadingForDrawables", "SetTextI18n"})
    private void sizeChangerInitOrUpdate(boolean isInit){
        TextView tvValSize = binding.nwMenuProfileControl.getMenu().findItem(R.id.item_element_size)
                .getActionView().findViewById(R.id.tv_val_size);
        SeekBar itemSizeChanger = binding.nwMenuProfileControl.getMenu().findItem(R.id.item_element_size)
                .getActionView().findViewById(R.id.sb_element_size_changer);

        if (isInit) itemSizeChanger.setProgress(gameControllersDrawer.getElementSize());
        else gameControllersDrawer.setElementSize(itemSizeChanger.getProgress());

        tvValSize.setText(Integer.toString(itemSizeChanger.getProgress()));
        if (itemSizeChanger.getProgress() > 0)
            itemSizeChanger.setThumb(getDrawable(R.drawable.thumb_for_element_size_changer_1));
        else itemSizeChanger.setThumb(getDrawable(R.drawable.thumb_for_element_size_changer_2));
        itemSizeChanger.setThumbOffset(0);

        if (itemSizeChanger.getProgress() > 18)
            itemSizeChanger.setProgressDrawable(getDrawable(R.drawable.progress_for_element_size_changer_1));
        else itemSizeChanger.setProgressDrawable(getDrawable(R.drawable.progress_for_element_size_changer_2));
    }

    @Override
    protected void onResume(){
        super.onResume();
        setFullscreenMode(binding.dlMenuDrawer);
        if (!checkBluetoothPeripherals()) return;
        gameControllersDrawer.updateElementsControl();
        menuItemsInit();
    }

    @Override
    protected void onPause(){
        super.onPause();
        gameControllersDrawer.saveElementsParams();
    }

    @Override
    protected void onDestroy(){
        super.onDestroy();
        dbAdapterElementsControl.close();
        dbProfiles.close();
        dbDisplays.close();
    }

    @Override
    public void remove(long id){
        if (id == 0) gameControllersDrawer.removeDisplay();
        else gameControllersDrawer.removeElementControl();
        setFullscreenMode(binding.dlMenuDrawer);
        menuItemsInit();
    }

    @Override
    public void cancel(){ setFullscreenMode(binding.dlMenuDrawer); }
}