package com.example.rcbleproject;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.RequiresApi;

import com.example.rcbleproject.Database.DatabaseAdapterForHubs;

import java.util.List;

public class ConnectedDevicesAdapter extends BaseAppArrayAdapter<BluetoothHub> implements IListViewAdapterForHubs {
    private static final int resource = R.layout.app_list_item;

    private final DatabaseAdapterForHubs dbAdapter;
    private final List<BluetoothHub> hubs;
    AddingHubsActivity activity;

    public ConnectedDevicesAdapter(AddingHubsActivity context, DatabaseAdapterForHubs dbHubs){
        super(context, resource, dbHubs.getConnectedHubs(context));
        dbAdapter = dbHubs;
        activity = context;
        hubs = dbAdapter.getConnectedHubs(context);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent){
        final ViewHolder holder;
        BluetoothHub hub = dbAdapter.getConnectedHubs(activity).get(position);

        if (convertView == null){
            convertView = inflater.inflate(this.layout, parent, false);
            holder = new ViewHolder(convertView);
            convertView.setTag(holder);

            holder.iv_hub_icon.setVisibility(View.VISIBLE);

            holder.et_device_name.setOnEditorActionListener((v, actionId, event) -> {
                if (actionId == EditorInfo.IME_ACTION_DONE) resetEditingView();
                return false;
            });
        }
        else holder = (ViewHolder) convertView.getTag();
        holder.position = position;

        if (hub.availability){
            holder.tv_device_name.setTextColor(Color.WHITE);
            holder.bt_light_alarm.setVisibility(View.VISIBLE);
            holder.bt_delete_device.setVisibility(View.VISIBLE);

            holder.bt_delete_device.setOnClickListener((View v) -> {
                //ViewHolder vh = (ViewHolder) ((View)v.getParent()).getTag();
                BluetoothHub bluetoothHub = hubs.get(holder.position);
                ConfirmRemoveDialogFragment dialog = new ConfirmRemoveDialogFragment();
                Bundle args = new Bundle();
                args.putInt("type", ConfirmRemoveDialogFragment.FragmentType.Hub.ordinal());
                args.putString("object_id", bluetoothHub.address);
                args.putString("message", activity.getResources().getString(R.string.confirm_msg_device) + " \"" + bluetoothHub.getName() + "\" ?");
                dialog.setArguments(args);
                dialog.setCancelable(false);
                dialog.show(activity.getSupportFragmentManager(), activity.getResources().getString(R.string.app_name));
            });
        }
        else {
            holder.tv_device_name.setTextColor(activity.getColor(R.color.blue_ncs));
            holder.bt_light_alarm.setVisibility(View.GONE);
            holder.bt_delete_device.setVisibility(View.INVISIBLE);
            holder.bt_delete_device.setOnClickListener(null);
        }

        if (hub.hubType != BluetoothHub.HubTypes.Unknown)
            holder.iv_hub_icon.setImageResource(hub.getIconId());

        holder.tv_device_name.setOnClickListener((View v) -> {
            if (getAvailability((View)(v.getParent())))
                setFocusOnEditText((View)(v.getParent()));
        });

        holder.bt_light_alarm.setOnClickListener(v -> hubs.get(holder.position).alarm(activity));
        holder.bt_cancel.setOnClickListener(v -> cancelEdit());

        holder.bt_ok.setOnClickListener(v -> {
            ViewHolder vh = ((ViewHolder)editingView.getTag());
            activity.hideKeyboard(vh.et_device_name);
            resetEditingView();
        });

        holder.tv_device_name.setText(hub.getName());

        return convertView;
    }

    @SuppressLint("MissingPermission")
    public boolean addHub(BluetoothHub hub){
        hub.availability = true;
        hub.stateConnection = true;
        dbAdapter.updateHub(hub, activity);
        notifyDataSetChanged();
        return true;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public BluetoothHub removeHub(String hubAddress){
        BluetoothHub bluetoothHub = dbAdapter.findConnectedHubByAddress(hubAddress);
        if (bluetoothHub != null){
            bluetoothHub.stateConnection = false;
            dbAdapter.updateHub(bluetoothHub, activity);
            notifyDataSetChanged();
        }
        return bluetoothHub;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @SuppressLint("MissingPermission")
    public boolean setAvailability(boolean flag, BluetoothDevice device){
        BluetoothHub hub = dbAdapter.findConnectedHubByAddress(device.getAddress());
        if (hub == null) return false;
        if (flag && !hub.availability){
            Log.v("APP_TAG33333333333333", device.getName() + " " + hub.getName());
            hub.updateHubNameInDB(device.getName());
        }
        hub.availability = flag;
        notifyDataSetChanged();
        return true;
    }

    public boolean getAvailability(View v){
        try {
            ViewHolder holder = (ViewHolder) v.getTag();
            BluetoothHub hub = hubs.get(holder.position);
            return hub.availability;
        }
        catch (Exception e) { return false; }
    }

    public void resetEditingView(){
        if (editingView == null) return;
        ViewHolder holder = (ViewHolder) editingView.getTag();
        if (holder == null) return;
        saveChanges(holder);
        editingView = null;
        holder.iv_hub_icon.setVisibility(View.VISIBLE);
    }

    public void cancelEdit(){
        if (editingView == null) return;
        ViewHolder vh = ((ViewHolder)editingView.getTag());
        setMode(Mode.view_mode, vh);
        activity.hideKeyboard(vh.et_device_name);
        editingView = null;
        vh.iv_hub_icon.setVisibility(View.VISIBLE);
    }

    protected void saveChanges(ViewHolder vh){
        String newName = vh.et_device_name.getText().toString();
        if (newName.length() <= 0){
            setMode(Mode.view_mode, vh);
            return;
        }
        if (!(hubs.get(vh.position).rename(newName, activity))){
            setMode(Mode.view_mode, vh);
            return;
        }
        vh.tv_device_name.setText(newName);
        dbAdapter.updateHub(hubs.get(vh.position), activity);
        setMode(Mode.view_mode, vh);
        notifyDataSetChanged();
    }

    public void setMode(Mode mode, ViewHolder vh){
        switch (mode){
            case view_mode:
                vh.et_device_name.setVisibility(View.GONE);
                vh.et_device_name.setText("");
                vh.bt_ok.setVisibility(View.GONE);
                vh.bt_cancel.setVisibility(View.GONE);
                vh.tv_device_name.setVisibility(View.VISIBLE);
                vh.bt_delete_device.setVisibility(View.VISIBLE);
                vh.bt_light_alarm.setVisibility(View.VISIBLE);
                activity.setFullscreenMode();
                break;
            case edit_mode:
                vh.tv_device_name.setVisibility(View.GONE);
                vh.bt_delete_device.setVisibility(View.GONE);
                vh.bt_ok.setVisibility(View.VISIBLE);
                vh.bt_cancel.setVisibility(View.VISIBLE);
                vh.et_device_name.setVisibility(View.VISIBLE);
                vh.bt_light_alarm.setVisibility(View.GONE);
        }
    }

    public void setFocusOnEditText(View view){
        if (view == null) return;
        ViewHolder vh = (ViewHolder) view.getTag();
        if (vh == null) return;
        resetEditingView();
        setEditingView(view);
        setMode(Mode.edit_mode, vh);
        vh.et_device_name.setText(vh.tv_device_name.getText());
        vh.et_device_name.setSelectAllOnFocus(true);
        vh.et_device_name.requestFocus();
        InputMethodManager imm = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.showSoftInput(vh.et_device_name, InputMethodManager.SHOW_IMPLICIT);
    }

    public void setEditingView(View editingView) {
        this.editingView = editingView;
        ViewHolder holder = (ViewHolder) editingView.getTag();
        if (holder == null) return;
        holder.iv_hub_icon.setVisibility(View.GONE);
    }

    private class ViewHolder{
        final TextView tv_device_name;
        final EditText et_device_name;
        final ImageView bt_delete_device;
        final ImageView bt_ok;
        final ImageView bt_cancel;
        final ImageView bt_light_alarm;
        final ImageView iv_hub_icon;
        int position = -1;

        ViewHolder(View view){
            tv_device_name = view.findViewById(R.id.tv_name);
            et_device_name = view.findViewById(R.id.et_name);
            bt_delete_device = view.findViewById(R.id.bt_delete);
            bt_ok = view.findViewById(R.id.bt_ok);
            bt_cancel = view.findViewById(R.id.bt_cancel);
            bt_light_alarm = view.findViewById(R.id.bt_alarm_light);
            iv_hub_icon = view.findViewById(R.id.iv_connected_hub_icon);
        }
    }
}
