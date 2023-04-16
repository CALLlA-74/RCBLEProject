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

        if (hubs.size() <= 0) activity.initIncEmptyListCnnctdHubsLblVisibility();
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

            holder.et_hub_name.setOnEditorActionListener((v, actionId, event) -> {
                if (actionId == EditorInfo.IME_ACTION_DONE) resetEditingView();
                return false;
            });
        }
        else holder = (ViewHolder) convertView.getTag();
        holder.position = position;
        holder.availability = hub.availability;

        if (hub.availability){
            holder.tv_hub_name.setTextColor(Color.WHITE);
            holder.bt_light_alarm.setVisibility(View.VISIBLE);
            holder.bt_context_menu.setVisibility(View.VISIBLE);
            //holder.bt_delete_hub.setVisibility(View.VISIBLE);

            holder.bt_delete_hub.setOnClickListener((View v) -> {
                //ViewHolder vh = (ViewHolder) ((View)v.getParent()).getTag();
                BluetoothHub bluetoothHub = hubs.get(holder.position);
                ConfirmRemoveDialogFragment dialog = new ConfirmRemoveDialogFragment();
                Bundle args = new Bundle();
                args.putInt("type", ConfirmRemoveDialogFragment.FragmentType.Hub.ordinal());
                args.putString("object_id", bluetoothHub.address);
                args.putString("message", activity.getResources().getString(R.string.confirm_msg_hub) + " \"" + bluetoothHub.getName() + "\" ?");
                dialog.setArguments(args);
                dialog.setCancelable(false);
                dialog.show(activity.getSupportFragmentManager(), activity.getResources().getString(R.string.app_name));
            });
        }
        else {
            holder.tv_hub_name.setTextColor(activity.getColor(R.color.blue_ncs));
            holder.bt_light_alarm.setVisibility(View.GONE);
            holder.bt_context_menu.setVisibility(View.GONE);
            holder.bt_delete_hub.setVisibility(View.GONE);
            holder.bt_edit_name.setVisibility(View.GONE);
        }

        if (hub.hubType != BluetoothHub.HubTypes.Unknown)
            holder.iv_hub_icon.setImageResource(hub.getIconId());

        holder.tv_hub_name.setOnClickListener((View v) -> {
            if (getAvailability((View)(v.getParent())))
                setFocusOnEditText((View)(v.getParent()));
        });

        holder.bt_light_alarm.setOnClickListener(v -> hubs.get(holder.position).alarm(activity));
        holder.bt_cancel.setOnClickListener(v -> cancelEdit());

        holder.bt_ok.setOnClickListener(v -> {
            if (!hub.availability) cancelEdit();
            ViewHolder vh = ((ViewHolder)editingView.getTag());
            activity.hideKeyboard(vh.et_hub_name);
            resetEditingView();
        });

        holder.bt_edit_name.setOnClickListener(v -> {
            if (!hub.availability) {
                activity.notifyNoHubConnection();
                return;
            }
            setFocusOnEditText((View)v.getParent());
        });

        holder.bt_context_menu.setOnClickListener(v -> {
            if (!hub.availability) {
                activity.notifyNoHubConnection();
                return;
            }
            resetViewInConMenu();
            viewInConMenu = (View)v.getParent();
            setMode(Mode.context_menu_mode, (ViewHolder)(viewInConMenu.getTag()));
        });

        holder.tv_hub_name.setText(hub.getName());

        return convertView;
    }

    @SuppressLint("MissingPermission")
    public boolean addHub(BluetoothHub hub){
        hub.availability = true;
        hub.stateConnection = true;
        dbAdapter.updateHub(hub, activity);
        activity.hideIncEmptyListCnnctdHubsLblVisibility();
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

            if (hubs.size() <= 0) activity.initIncEmptyListCnnctdHubsLblVisibility();
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

    public boolean cancelEdit(){
        boolean isViewNull = resetViewInConMenu();
        if (editingView == null) return isViewNull;
        ViewHolder vh = ((ViewHolder)editingView.getTag());
        setMode(Mode.view_mode, vh);
        activity.hideKeyboard(vh.et_hub_name);
        editingView = null;
        vh.iv_hub_icon.setVisibility(View.VISIBLE);
        return true;
    }

    public boolean resetViewInConMenu(){
        if (viewInConMenu != null){
            setMode(Mode.view_mode, (ViewHolder) (viewInConMenu.getTag()));
            viewInConMenu = null;
            return true;
        }
        return false;
    }

    protected void saveChanges(ViewHolder vh){
        String newName = vh.et_hub_name.getText().toString();
        if (newName.length() <= 0){
            setMode(Mode.view_mode, vh);
            return;
        }
        if (!(hubs.get(vh.position).rename(newName, activity))){
            setMode(Mode.view_mode, vh);
            return;
        }
        vh.tv_hub_name.setText(newName);
        dbAdapter.updateHub(hubs.get(vh.position), activity);
        setMode(Mode.view_mode, vh);
        notifyDataSetChanged();
    }

    public void setMode(Mode mode, ViewHolder vh){
        switch (mode){
            case view_mode:
                vh.et_hub_name.setVisibility(View.GONE);
                vh.et_hub_name.setText("");
                vh.bt_ok.setVisibility(View.GONE);
                vh.bt_cancel.setVisibility(View.GONE);
                vh.tv_hub_name.setVisibility(View.VISIBLE);
                vh.bt_delete_hub.setVisibility(View.GONE);
                vh.bt_light_alarm.setVisibility(View.VISIBLE);
                vh.bt_edit_name.setVisibility(View.GONE);
                vh.bt_context_menu.setVisibility(View.VISIBLE);
                activity.setFullscreenMode();
                break;
            case context_menu_mode:
                vh.et_hub_name.setVisibility(View.GONE);
                vh.bt_ok.setVisibility(View.GONE);
                vh.bt_cancel.setVisibility(View.GONE);
                vh.tv_hub_name.setVisibility(View.VISIBLE);
                vh.bt_delete_hub.setVisibility(View.VISIBLE);
                vh.bt_light_alarm.setVisibility(View.GONE);
                vh.bt_edit_name.setVisibility(View.VISIBLE);
                vh.bt_context_menu.setVisibility(View.GONE);
                activity.setFullscreenMode();
                break;
            case edit_mode:
                vh.tv_hub_name.setVisibility(View.GONE);
                vh.bt_delete_hub.setVisibility(View.GONE);
                vh.bt_ok.setVisibility(View.VISIBLE);
                vh.bt_cancel.setVisibility(View.VISIBLE);
                vh.et_hub_name.setVisibility(View.VISIBLE);
                vh.bt_light_alarm.setVisibility(View.GONE);
                vh.bt_edit_name.setVisibility(View.GONE);
                vh.bt_context_menu.setVisibility(View.GONE);
        }
    }

    public void setFocusOnEditText(View view){
        if (view == null) return;
        ViewHolder vh = (ViewHolder) view.getTag();
        if (vh == null) return;
        resetViewInConMenu();
        resetEditingView();
        setEditingView(view);
        setMode(Mode.edit_mode, vh);
        vh.et_hub_name.setText(vh.tv_hub_name.getText());
        vh.et_hub_name.setSelectAllOnFocus(true);
        vh.et_hub_name.requestFocus();
        InputMethodManager imm = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.showSoftInput(vh.et_hub_name, InputMethodManager.SHOW_IMPLICIT);
    }

    public void setEditingView(View editingView) {
        this.editingView = editingView;
        ViewHolder holder = (ViewHolder) editingView.getTag();
        if (holder == null) return;
        holder.iv_hub_icon.setVisibility(View.GONE);
    }

    private class ViewHolder{
        final TextView tv_hub_name;
        final EditText et_hub_name;
        final ImageView bt_delete_hub;
        final ImageView bt_ok;
        final ImageView bt_cancel;
        final ImageView bt_light_alarm;
        final ImageView iv_hub_icon;
        final ImageView bt_edit_name;
        final ImageView bt_context_menu;
        int position = -1;
        boolean availability = false;

        ViewHolder(View view){
            tv_hub_name = view.findViewById(R.id.tv_name);
            et_hub_name = view.findViewById(R.id.et_name);
            bt_delete_hub = view.findViewById(R.id.bt_delete);
            bt_ok = view.findViewById(R.id.bt_ok);
            bt_cancel = view.findViewById(R.id.bt_cancel);
            bt_light_alarm = view.findViewById(R.id.bt_alarm_light);
            iv_hub_icon = view.findViewById(R.id.iv_connected_hub_icon);
            bt_edit_name = view.findViewById(R.id.bt_edit_name);
            bt_context_menu = view.findViewById(R.id.bt_context_menu);
        }
    }
}
