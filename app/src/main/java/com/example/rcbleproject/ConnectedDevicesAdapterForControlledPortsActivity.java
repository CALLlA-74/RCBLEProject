package com.example.rcbleproject;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.rcbleproject.Database.DatabaseAdapterForDevices;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class ConnectedDevicesAdapterForControlledPortsActivity extends ArrayAdapter<BluetoothDeviceApp>
        implements IListViewAdapterForDevices {
    private final Map<String, Boolean> availability = Collections.synchronizedMap(new HashMap<>());
    private final SettingControlledPortsActivity activity;
    private static final int layout = R.layout.item_device_for_spinner;
    private final LayoutInflater inflater;
    private final ArrayList<BluetoothDeviceApp> deviceApps;

    @SuppressLint("MissingPermission")
    public ConnectedDevicesAdapterForControlledPortsActivity(SettingControlledPortsActivity context,
                                                             ArrayList<BluetoothDeviceApp> devices) {
        super(context, layout, devices);
        deviceApps = devices;
        activity = context;
        inflater = LayoutInflater.from(context);

        for (BluetoothDeviceApp deviceApp : deviceApps) {
            availability.put(deviceApp.getDevice().getAddress(), Boolean.FALSE);
        }
        setDropDownViewResource(android.R.layout.simple_spinner_item);
    }

    @SuppressLint("MissingPermission")
    @Override
    public View getView(int position, View convertView, ViewGroup parent){
        final ViewHolder viewHolder;
        if (convertView == null){
            convertView = inflater.inflate(layout, parent, false);
            viewHolder = new ViewHolder(convertView);
            convertView.setTag(viewHolder);
        }
        else viewHolder = (ViewHolder) convertView.getTag();
        viewHolder.pos = position;

        if (availability.get(deviceApps.get(position).getDevice().getAddress()))
            viewHolder.bt_where_are_you.setVisibility(View.VISIBLE);
        else viewHolder.bt_where_are_you.setVisibility(View.GONE);

        viewHolder.tv_device_name.setText(deviceApps.get(position).name);
        viewHolder.bt_where_are_you.setOnClickListener((View v) -> {
            ViewHolder vh = (ViewHolder) ((View)v.getParent()).getTag();
            String address = deviceApps.get(vh.pos).getDevice().getAddress();
            activity.writeCharacteristic(address, "1");
        });
        Log.v("APP_TAG3", "getting view. pos = " + position);

        return convertView;
    }

    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {
        return getView(position, convertView, parent);
    }

    @Override
    public boolean addDevice(BluetoothDevice device){ return false; }

    @Override
    public boolean removeDevice(BluetoothDevice device) { return false; }

    @Override
    public boolean setAvailability(boolean flag, BluetoothDevice device){
        if (!availability.containsKey(device.getAddress())) return false;
        availability.put(device.getAddress(), flag);
        notifyDataSetChanged();
        return true;
    }

    private class ViewHolder{
        final TextView tv_device_name;
        final ImageView bt_where_are_you;
        int pos = -1;

        ViewHolder(View view){
            tv_device_name = view.findViewById(R.id.tv_device_name);
            bt_where_are_you = view.findViewById(R.id.bt_where_are_you);
        }
    }
}
