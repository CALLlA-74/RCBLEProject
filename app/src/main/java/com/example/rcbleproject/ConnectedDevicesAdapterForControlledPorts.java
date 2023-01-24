package com.example.rcbleproject;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class ConnectedDevicesAdapterForControlledPorts extends ArrayAdapter<BluetoothHub>
        implements IListViewAdapterForHubs {
    private final Map<String, Boolean> availability = Collections.synchronizedMap(new HashMap<>());
    private final SettingControlledPortsActivity activity;
    private static final int layout = R.layout.item_device_for_spinner;
    private final LayoutInflater inflater;
    private final ArrayList<BluetoothHub> hubs;

    @SuppressLint("MissingPermission")
    public ConnectedDevicesAdapterForControlledPorts(SettingControlledPortsActivity context,
                                                     ArrayList<BluetoothHub> devices) {
        super(context, layout, devices);
        hubs = devices;
        activity = context;
        inflater = LayoutInflater.from(context);

        for (BluetoothHub hub : hubs) {
            availability.put(hub.address, Boolean.FALSE);
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

        if (availability.get(hubs.get(position).address))
            viewHolder.bt_where_are_you.setVisibility(View.VISIBLE);
        else viewHolder.bt_where_are_you.setVisibility(View.GONE);

        viewHolder.tv_device_name.setText(hubs.get(position).name);
        viewHolder.bt_where_are_you.setOnClickListener((View v) -> {
            ViewHolder vh = (ViewHolder) ((View)v.getParent()).getTag();
            hubs.get(vh.pos).alarm(activity);
        });
        Log.v("APP_TAG3", "getting view. pos = " + position);

        return convertView;
    }

    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {
        return getView(position, convertView, parent);
    }

    @Override
    public boolean addHub(BluetoothHub hub){ return false; }

    @Override
    public BluetoothHub removeHub(String hubAddress) { return null; }

    @Override
    public boolean setAvailability(boolean flag, BluetoothDevice device){
        String hubAddress = device.getAddress();
        if (!availability.containsKey(hubAddress)) return false;
        availability.put(hubAddress, flag);
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
