package com.example.rcbleproject;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.TextView;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class FoundDevicesAdapter extends ArrayAdapter<BluetoothDeviceApp> implements IListViewAdapterForDevices {
    public enum Activeness {active, inactive}

    private final LayoutInflater inflater;
    private final int layout;
    private final List<BluetoothDeviceApp> devices;
    private final AddingDevicesActivity activity;

    public FoundDevicesAdapter(AddingDevicesActivity context, int resource, List<BluetoothDeviceApp> devices) {
        super(context, resource, devices);
        this.devices = devices;
        layout = resource;
        inflater = LayoutInflater.from(context);
        activity = context;

        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                long time = System.currentTimeMillis();
                for (BluetoothDeviceApp deviceApp : devices){
                    if (time - deviceApp.lastTimeAdv > 8000)
                        activity.runOnUiThread(() -> {
                            deviceApp.isActive = false;
                            notifyDataSetChanged();
                        });
                }
            }
        }, 0, 1000);
    }

    @SuppressLint("MissingPermission")
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final ViewHolder holder;
        BluetoothDeviceApp deviceApp = devices.get(position);
        if (convertView == null) {
            convertView = inflater.inflate(this.layout, parent, false);
            holder = new ViewHolder(convertView);
            convertView.setTag(holder);
        } else holder = (ViewHolder) convertView.getTag();
        holder.position = position;

        holder.tv_device_name.setText(deviceApp.getDevice().getName() + " (" + deviceApp.getDevice().getAddress() + ")");
        if (deviceApp.isActive) setActiveness(Activeness.active,holder);
        else setActiveness(Activeness.inactive, holder);

        holder.bt_add_device.setOnClickListener((View v) -> {
            ViewHolder vh = (ViewHolder) ((View)v.getParent()).getTag();
            if(BuildConfig.DEBUG) Log.v("APP_TAG2", "connecting to " + devices.get(vh.position).getDevice().getAddress());
            vh.bt_add_device.setVisibility(View.GONE);
            activity.connectDevice(devices.get(vh.position).getDevice());
        });
        return convertView;
    }

    public boolean addDevice(BluetoothDevice device){
        if (device == null) return false;
        int pos = containsDevice(device);
        if (pos >= 0){
            devices.get(pos).lastTimeAdv = System.currentTimeMillis();
            devices.get(pos).isActive = true;
            notifyDataSetChanged();
            return false;
        }
        BluetoothDeviceApp deviceApp = new BluetoothDeviceApp(device);
        deviceApp.lastTimeAdv = System.currentTimeMillis();
        add(deviceApp);
        notifyDataSetChanged();
        return true;
    }

    /**
     * Возврщает true, если device находился в списке найденных устройств
     **/
    public boolean removeDevice(BluetoothDevice device){
        int pos = containsDevice(device);
        if (pos >= 0){
            remove(getItem(pos));
            notifyDataSetChanged();
            return  true;
        }
        return false;
    }

    public boolean setAvailability(boolean flag, BluetoothDevice device){ return true; }

    private int containsDevice(BluetoothDevice device){
        for (int i = 0; i < devices.size(); i++){
            if (devices.get(i).getDevice().getAddress().equals(device.getAddress()))
                return i;
        }
        return -1;
    }

    private void setActiveness(Activeness activeness, ViewHolder vh){
        switch (activeness){
            case active:
                vh.tv_device_name.setTextColor(Color.WHITE);
                vh.bt_add_device.setVisibility(View.VISIBLE);
                break;
            case inactive:
                vh.tv_device_name.setTextColor(activity.getColor(R.color.blue_ncs));
                vh.bt_add_device.setVisibility(View.GONE);
        }
    }

    private class ViewHolder{
        final TextView tv_device_name;
        final ImageButton bt_add_device;
        volatile int position;
        ViewHolder(View view){
            tv_device_name = view.findViewById(R.id.tv_device_name);
            bt_add_device = view.findViewById(R.id.bt_add_device);
        }
    }
}
