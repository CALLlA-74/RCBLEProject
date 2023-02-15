package com.example.rcbleproject;

import static com.example.rcbleproject.Database.DatabaseAdapterForHubs.HUB_ADDRESS;
import static com.example.rcbleproject.Database.DatabaseAdapterForHubs.HUB_NAME;
import static com.example.rcbleproject.Database.DatabaseAdapterForHubs.HUB_TYPE;
import static com.example.rcbleproject.Database.DatabaseAdapterForHubs.ID;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.rcbleproject.Database.DatabaseAdapterForHubs;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class ConnectedDevicesAdapter extends BaseAppCursorAdapter implements IListViewAdapterForHubs {
    private final DatabaseAdapterForHubs dbAdapter;
    private final Map<String, Boolean> availability = Collections.synchronizedMap(new HashMap<>());
    AddingHubsActivity activity;

    public ConnectedDevicesAdapter(AddingHubsActivity context, int resource,
                                   DatabaseAdapterForHubs adapter){
        super(context, resource, adapter.getConnectedHubs_cursor(), adapter.getColumns(),
                new int[]{R.id.et_name, R.id.tv_name, R.id.bt_delete,
                        R.id.bt_cancel, R.id.bt_ok}, 0);
        dbAdapter = adapter;
        activity = context;
        Cursor c = getCursor();
        int columnAddressIndex = c.getColumnIndexOrThrow(HUB_ADDRESS);
        while (c.moveToNext()){
            availability.put(c.getString(columnAddressIndex), Boolean.FALSE);
        }
        c.moveToFirst();
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent){
        View convertView = inflater.inflate(this.layout, parent, false);
        long id = cursor.getLong(cursor.getColumnIndexOrThrow(ID));
        ViewHolder holder = new ViewHolder(convertView);
        holder.id = id;
        convertView.setTag(holder);
        return convertView;
    }

    @Override
    public void bindView(View convertView, Context context, Cursor cursor){
        final ViewHolder holder = (ViewHolder) convertView.getTag();
        holder.id = cursor.getLong(cursor.getColumnIndexOrThrow(ID));
        holder.iv_hub_icon.setVisibility(View.VISIBLE);
        Log.v("APP_TAG22", "device id = " + holder.id);

        holder.et_device_name.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) resetEditingView();
            return false;
        });

        if (Boolean.TRUE.equals(availability.get(cursor.getString(cursor.getColumnIndexOrThrow(HUB_ADDRESS))))){
            holder.tv_device_name.setTextColor(Color.WHITE);
            holder.bt_light_alarm.setVisibility(View.VISIBLE);
        }
        else {
            holder.tv_device_name.setTextColor(activity.getColor(R.color.blue_ncs));
            holder.bt_light_alarm.setVisibility(View.GONE);
        }

        BluetoothHub hub = new BluetoothHub(cursor.getLong(cursor.getColumnIndexOrThrow(ID)), activity);
        if (hub.hubType != BluetoothHub.HubTypes.Unknown)
            holder.iv_hub_icon.setImageResource(hub.getIconId());

        holder.bt_delete_device.setOnClickListener((View v) -> {
            ViewHolder vh = (ViewHolder) ((View)v.getParent()).getTag();
            long id = vh.id;
            Log.v("APP_TAG222", "delete. id = " + id);
            Cursor c = dbAdapter.getHubById_cursor(id);
            c.moveToFirst();
            ConfirmRemoveDialogFragment dialog = new ConfirmRemoveDialogFragment();
            Bundle args = new Bundle();
            args.putLong("object_id", id);
            args.putString("message", activity.getResources().getString(R.string.confirm_msg_device) + " \"" +
                    c.getString(c.getColumnIndexOrThrow(DatabaseAdapterForHubs.HUB_NAME)) + "\" ?");
            dialog.setArguments(args);
            dialog.setCancelable(false);
            dialog.show(activity.getSupportFragmentManager(), activity.getResources().getString(R.string.app_name));
        });

        holder.bt_light_alarm.setOnClickListener(v -> {
            ViewHolder vh = (ViewHolder) ((View)v.getParent()).getTag();
            Cursor c = dbAdapter.getHubById_cursor(vh.id);
            if (!c.moveToFirst()) return;
            String deviceAddress = c.getString(c.getColumnIndexOrThrow(HUB_ADDRESS));
            BluetoothHub.HubTypes type = BluetoothHub.IntToHubTypes(c.getInt(c.getColumnIndexOrThrow(HUB_TYPE)));
            Log.v("APP_TAG222", "id = " + vh.id + "; addr = " + deviceAddress);
            c.close();
            new BluetoothHub(vh.id, activity).alarm(activity);
        });
        holder.bt_cancel.setOnClickListener(v -> cancelEdit());

        holder.bt_ok.setOnClickListener(v -> {
            ViewHolder vh = ((ViewHolder)editingView.getTag());
            activity.hideKeyboard(vh.et_device_name);
            resetEditingView();
        });

        holder.tv_device_name.setText(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseAdapterForHubs.HUB_NAME)));
        Log.v("APP_TAG", "get view. id = " + cursor.getLong(cursor.getColumnIndexOrThrow(ID)));
    }

    @SuppressLint("MissingPermission")
    public boolean addHub(BluetoothHub hub){
        Cursor cursor = dbAdapter.getHubByAddress_cursor(hub.address);
        if (cursor.moveToFirst()) {
            dbAdapter.updateNameAndState(cursor.getLong(cursor.getColumnIndexOrThrow(ID)), hub.getName(), 1);
        }
        else {
            long id = dbAdapter.insert(hub);
            if (BuildConfig.DEBUG) Log.v("APP_TAG", "addProf. id = " + id);
        }
        availability.put(hub.address, true);
        swapCursor(dbAdapter.getConnectedHubs_cursor());
        return true;
    }

    public BluetoothHub removeHub(String hubAddress){
        Cursor c = dbAdapter.getHubByAddress_cursor(hubAddress);
        if (!c.moveToFirst()) return null;
        long hubId = c.getLong(c.getColumnIndexOrThrow(ID));
        String hubName = c.getString(c.getColumnIndexOrThrow(HUB_NAME));
        dbAdapter.updateNameAndState(hubId, hubName,  0);
        c.close();
        swapCursor(dbAdapter.getConnectedHubs_cursor());
        availability.remove(hubAddress);
        notifyDataSetChanged();
        return new BluetoothHub(hubId, activity);
    }

    @SuppressLint("MissingPermission")
    public boolean setAvailability(boolean flag, BluetoothDevice device){
        if (!availability.containsKey(device.getAddress())) return false;
        if (flag){
            if (BluetoothHub.updateHubNameInDB(activity, device.getAddress(), device.getName()))
                swapCursor(dbAdapter.getConnectedHubs_cursor());
        }
        availability.put(device.getAddress(), flag);
        notifyDataSetChanged();
        return true;
    }

    public boolean getAvailability(View v){
        ViewHolder holder = (ViewHolder) v.getTag();
        Cursor cursor = dbAdapter.getHubById_cursor(holder.id);
        if (!cursor.moveToFirst()) return false;
        String hubAddress = cursor.getString(cursor.getColumnIndexOrThrow(HUB_ADDRESS));
        if (!availability.containsKey(hubAddress)) return false;
        return Boolean.TRUE.equals(availability.get(hubAddress));
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
        if (!(new BluetoothHub(vh.id, activity).setName(activity, newName))){
            setMode(Mode.view_mode, vh);
            return;
        }
        vh.tv_device_name.setText(newName);
        dbAdapter.updateNameById(vh.id, newName);
        setMode(Mode.view_mode, vh);
        swapCursor(dbAdapter.getConnectedHubs_cursor());
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
        long id = 0;

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
