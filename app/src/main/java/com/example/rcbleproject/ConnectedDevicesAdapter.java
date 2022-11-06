package com.example.rcbleproject;

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
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.rcbleproject.Database.DatabaseAdapterForDevices;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class ConnectedDevicesAdapter extends BaseAppCursorAdapter implements IListViewAdapterForDevices {
    private final DatabaseAdapterForDevices dbAdapter;
    private final Map<String, Boolean> availability = Collections.synchronizedMap(new HashMap<>());
    AddingDevicesActivity activity;

    public ConnectedDevicesAdapter(AddingDevicesActivity context, int resource,
                                   DatabaseAdapterForDevices adapter){
        super(context, resource, adapter.getConnectedDevices_cursor(), adapter.getColumns(),
                new int[]{R.id.et_name, R.id.tv_name, R.id.bt_delete,
                        R.id.bt_cancel, R.id.bt_ok}, 0);
        dbAdapter = adapter;
        activity = context;
        Cursor c = getCursor();
        while (c.moveToNext()){
            availability.put(c.getString(c.getColumnIndexOrThrow(dbAdapter.DEVICE_ADDRESS)),
                    Boolean.FALSE);
            activity.connectDevice(c.getString(c.getColumnIndexOrThrow(dbAdapter.DEVICE_ADDRESS)));
        }
        c.moveToFirst();
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent){
        View convertView = inflater.inflate(this.layout, parent, false);
        ViewHolder holder = new ViewHolder(convertView);
        convertView.setTag(holder);
        return convertView;
    }

    @Override
    public void bindView(View convertView, Context context, Cursor cursor){
        final ViewHolder holder = (ViewHolder) convertView.getTag();
        holder.id = cursor.getLong(cursor.getColumnIndexOrThrow(dbAdapter.ID));
        Log.v("APP_TAG22", "device id = " + holder.id);

        holder.et_device_name.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) resetEditingView();
            return false;
        });

        if (availability.get(cursor.getString(cursor.getColumnIndexOrThrow(dbAdapter.DEVICE_ADDRESS)))){
            holder.tv_device_name.setTextColor(Color.WHITE);
            holder.bt_light_alarm.setVisibility(View.VISIBLE);
        }
        else {
            holder.tv_device_name.setTextColor(activity.getColor(R.color.blue_ncs));
            holder.bt_light_alarm.setVisibility(View.GONE);
        }

        holder.bt_delete_device.setOnClickListener((View v) -> {
            long id = ((ViewHolder)((View)v.getParent()).getTag()).id;
            Log.v("APP_TAG", "delete. id = " + id);
            Cursor c = dbAdapter.getDeviceById_cursor(id);
            c.moveToFirst();
            ConfirmRemoveDialogFragment dialog = new ConfirmRemoveDialogFragment();
            Bundle args = new Bundle();
            args.putLong("object_id", id);
            args.putString("message", activity.getResources().getString(R.string.confirm_msg_device) + " \"" +
                    c.getString(c.getColumnIndexOrThrow(dbAdapter.DEVICE_NAME)) + "\" ?");
            dialog.setArguments(args);
            dialog.setCancelable(false);
            dialog.show(activity.getSupportFragmentManager(), activity.getResources().getString(R.string.app_name));
        });

        holder.bt_light_alarm.setOnClickListener(v -> {
            String address = cursor.getString(cursor.getColumnIndexOrThrow(dbAdapter.DEVICE_ADDRESS));
            activity.writeCharacteristic(address, "1");
        });

        holder.bt_cancel.setOnClickListener(v -> cancelEdit());

        holder.bt_ok.setOnClickListener(v -> {
            ViewHolder vh = ((ViewHolder)editingView.getTag());
            activity.hideKeyboard(vh.et_device_name);
            resetEditingView();
        });

        holder.tv_device_name.setText(cursor.getString(cursor.getColumnIndexOrThrow(dbAdapter.DEVICE_NAME)));
        Log.v("APP_TAG", "get view. id = " + cursor.getLong(cursor.getColumnIndexOrThrow(dbAdapter.ID)));
    }

    @SuppressLint("MissingPermission")
    public boolean addDevice(BluetoothDevice device){
        Cursor c = dbAdapter.getDeviceByAddress_cursor(device.getAddress());
        if (c.getCount() > 0) {
            c.moveToFirst();
            dbAdapter.updateState(c.getLong(c.getColumnIndexOrThrow(dbAdapter.ID)), 1);
        }
        else {
            long id = dbAdapter.insert(device.getName(), device.getAddress(), 1);
            if (BuildConfig.DEBUG) Log.v("APP_TAG", "addProf. id = " + id);
        }
        availability.put(device.getAddress(), true);
        swapCursor(dbAdapter.getConnectedDevices_cursor());
        return true;
    }

    public boolean removeDevice(BluetoothDevice device){
        String deviceAddress = device.getAddress();
        Cursor c = dbAdapter.getDeviceByAddress_cursor(deviceAddress);
        c.moveToFirst();
        dbAdapter.updateState(c.getLong(c.getColumnIndexOrThrow(dbAdapter.ID)), 0);
        swapCursor(dbAdapter.getConnectedDevices_cursor());
        availability.remove(deviceAddress);
        notifyDataSetChanged();
        return true;
    }

    public boolean setAvailability(boolean flag, BluetoothDevice device){
        if (!availability.containsKey(device.getAddress())) return false;
        availability.put(device.getAddress(), flag);
        notifyDataSetChanged();
        return true;
    }

    public void resetEditingView(){
        if (editingView == null) return;
        ViewHolder holder = (ViewHolder) editingView.getTag();
        if (holder == null) return;
        saveChanges(holder);
        editingView = null;
    }

    public void cancelEdit(){
        if (editingView == null) return;
        ViewHolder vh = ((ViewHolder)editingView.getTag());
        setMode(Mode.view_mode, vh);
        activity.hideKeyboard(vh.et_device_name);
        editingView = null;
    }

    protected void saveChanges(ViewHolder vh){
        String newName = vh.et_device_name.getText().toString();
        if (newName.length() <= 0){
            setMode(Mode.view_mode, vh);
            return;
        }
        vh.tv_device_name.setText(newName);
        dbAdapter.updateName(vh.id, newName);
        setMode(Mode.view_mode, vh);
        swapCursor(dbAdapter.getConnectedDevices_cursor());
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
    }

    /*public void allDisconnect(){
        Cursor c = getCursor();
        if (!c.moveToFirst()) return;
        do{
            String deviceAddress = c.getString(c.getColumnIndexOrThrow(dbAdapter.DEVICE_ADDRESS));
            if (availability.get(deviceAddress))
                activity.disconnectDevice(deviceAddress);
        } while (c.moveToNext());
    }*/

    private class ViewHolder{
        final TextView tv_device_name;
        final EditText et_device_name;
        final ImageView bt_delete_device;
        final ImageView bt_ok;
        final ImageView bt_cancel;
        final ImageView bt_light_alarm;
        long id = 0;

        ViewHolder(View view){
            tv_device_name = view.findViewById(R.id.tv_name);
            et_device_name = view.findViewById(R.id.et_name);
            bt_delete_device = view.findViewById(R.id.bt_delete);
            bt_ok = view.findViewById(R.id.bt_ok);
            bt_cancel = view.findViewById(R.id.bt_cancel);
            bt_light_alarm = view.findViewById(R.id.bt_alarm_light);
        }
    }
}
