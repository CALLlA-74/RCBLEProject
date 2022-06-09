package com.example.rcbleproject;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import java.util.ArrayList;

public class ControlledPortsAdapter extends ArrayAdapter<ControlledPort> {
    private final SettingControlledPortsActivity activity;
    private static final int layout = R.layout.item_controlled_port;
    private final ArrayList<ControlledPort> controlledPorts;
    private final ArrayList<BluetoothDeviceApp> devices;
    private final LayoutInflater inflater;
    private final ConnectedDevicesAdapterForControlledPorts connectedDevicesAdapter;

    public ControlledPortsAdapter(SettingControlledPortsActivity context,
                                  ArrayList<ControlledPort> controlledPorts,
                                  ArrayList<BluetoothDeviceApp> devices){
        super(context, layout, controlledPorts);
        activity = context;
        this.controlledPorts = controlledPorts;
        inflater = LayoutInflater.from(context);
        this.devices = devices;
        connectedDevicesAdapter = new ConnectedDevicesAdapterForControlledPorts(activity,
                devices);
    }

    public ConnectedDevicesAdapterForControlledPorts getConnectedDevicesAdapter(){
        return connectedDevicesAdapter;
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

        viewHolder.sp_devices.setAdapter(connectedDevicesAdapter);
        return convertView;
    }

    private class ViewHolder{
        final Spinner sp_devices;
        ViewHolder(View view){
            sp_devices = view.findViewById(R.id.sp_devices);
        }
    }
}
