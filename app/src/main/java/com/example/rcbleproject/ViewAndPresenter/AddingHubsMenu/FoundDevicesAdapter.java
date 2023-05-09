package com.example.rcbleproject.ViewAndPresenter.AddingHubsMenu;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.rcbleproject.BuildConfig;
import com.example.rcbleproject.ViewAndPresenter.IListViewAdapterForHubs;
import com.example.rcbleproject.Model.BluetoothHub;
import com.example.rcbleproject.R;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class FoundDevicesAdapter extends ArrayAdapter<BluetoothHub> implements IListViewAdapterForHubs {
    public enum Activeness {active, inactive}
    private static final int resource = R.layout.item_found_device;

    private final LayoutInflater inflater;
    private final int layout;
    private final List<BluetoothHub> hubs;
    private final AddingHubsActivity activity;

    public FoundDevicesAdapter(AddingHubsActivity context, List<BluetoothHub> hubs) {
        super(context, resource, hubs);
        this.hubs = hubs;
        layout = resource;
        inflater = LayoutInflater.from(context);
        activity = context;

        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                long time = System.currentTimeMillis();
                for (BluetoothHub hub : hubs){
                    if (time - hub.lastTimeAdv > 2000)
                        activity.runOnUiThread(() -> {
                            hub.isActive = false;
                            notifyDataSetChanged();
                        });
                }
            }
        }, 0, 1000);

        if (hubs.size() <= 0) activity.initIncEmptyListFndHubsLbl();
    }

    @SuppressLint("MissingPermission")
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final ViewHolder holder;
        BluetoothHub hub = hubs.get(position);
        if (convertView == null) {
            convertView = inflater.inflate(this.layout, parent, false);
            holder = new ViewHolder(convertView);
            convertView.setTag(holder);
        } else holder = (ViewHolder) convertView.getTag();
        holder.position = position;

        holder.tv_device_name.setText(hub.getName()/* + " (" + hub.address + ")"*/);
        if (hub.isActive) {
            setActiveness(Activeness.active,holder);
            convertView.setOnClickListener((View v) -> {
                ViewHolder vh = (ViewHolder) v.getTag();
                if(BuildConfig.DEBUG)
                    Log.v("APP_TAG6666", "connecting to " + hubs.get(vh.position).address);
                vh.iv_add_device.setVisibility(View.GONE);
                activity.connectDevice(hubs.get(vh.position).address);
            });
        }
        else {
            setActiveness(Activeness.inactive, holder);
            convertView.setOnClickListener(null);
        }

        if (hub.hubType != BluetoothHub.HubTypes.Unknown)
            holder.iv_hub_icon.setImageResource(hub.getIconId());
        return convertView;
    }

    public boolean addHub(BluetoothHub hub){
        if (hub == null) return false;
        int pos = containsDevice(hub.address);
        if (pos >= 0){
            hubs.get(pos).lastTimeAdv = System.currentTimeMillis();
            hubs.get(pos).isActive = true;
            notifyDataSetChanged();
            return false;
        }
        hub.lastTimeAdv = System.currentTimeMillis();
        add(hub);
        notifyDataSetChanged();
        activity.hideIncEmptyListFndHubsLblVisibility();
        return true;
    }

    /**
     * Возврщает true, если device находился в списке найденных устройств
     **/
    public BluetoothHub removeHub(String hubAddress){
        int pos = containsDevice(hubAddress);
        if (pos >= 0){
            BluetoothHub removableHub = getItem(pos);
            remove(removableHub);
            notifyDataSetChanged();
            if (hubs.size() <= 0) activity.initIncEmptyListFndHubsLbl();
            return  removableHub;
        }
        return null;
    }

    public boolean setAvailability(boolean flag, BluetoothDevice device){ return true; }

    private int containsDevice(String address){
        for (int i = 0; i < hubs.size(); i++){
            if (hubs.get(i).address.equals(address))
                return i;
        }
        return -1;
    }

    private void setActiveness(Activeness activeness, ViewHolder vh){
        switch (activeness){
            case active:
                vh.tv_device_name.setTextColor(Color.WHITE);
                vh.iv_add_device.setVisibility(View.VISIBLE);
                break;
            case inactive:
                vh.tv_device_name.setTextColor(activity.getColor(R.color.blue_ncs));
                vh.iv_add_device.setVisibility(View.GONE);
        }
    }

    private class ViewHolder{
        final TextView tv_device_name;
        final ImageView iv_add_device;
        final ImageView iv_hub_icon;
        volatile int position;
        ViewHolder(View view){
            tv_device_name = view.findViewById(R.id.tv_device_name);
            iv_add_device = view.findViewById(R.id.iv_add_device);
            iv_hub_icon = view.findViewById(R.id.iv_hub_icon);
        }
    }
}
