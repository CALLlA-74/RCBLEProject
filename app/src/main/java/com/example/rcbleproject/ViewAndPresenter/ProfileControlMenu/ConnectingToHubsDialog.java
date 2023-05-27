package com.example.rcbleproject.ViewAndPresenter.ProfileControlMenu;

import static android.view.View.TEXT_ALIGNMENT_TEXT_START;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.rcbleproject.Model.BluetoothHub;
import com.example.rcbleproject.Model.Image;
import com.example.rcbleproject.R;
import com.example.rcbleproject.ViewAndPresenter.IListViewAdapterForHubs;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeSet;

public class ConnectingToHubsDialog extends Dialog {

    private final List<BluetoothHub> hubs;
    private final ProfileControlActivity activity;

    private final Dialog dialogContext;

    private Timer checkAvailabilitiesTimer;
    private String strConnecting;
    private TextView tvHeader;

    RecyclerView rvHubs;

    public ConnectingToHubsDialog(ProfileControlActivity activity,
                                  TreeSet<BluetoothHub> hubs){
        super(activity);
        this.activity = activity;
        dialogContext = this;
        this.hubs = new ArrayList<>(hubs);
    }

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.connecting_to_hubs_dialog);
        rvHubs = findViewById(R.id.rv_hubs);
        HubsListAdapter adapter = new HubsListAdapter(getContext(), hubs);
        rvHubs.setAdapter(adapter);
        activity.setLvAdapterConnectedDevices(adapter);
        dialogContext.setCancelable(false);

        Button btBack = findViewById(R.id.bt_back);
        btBack.setOnClickListener(v -> {
            activity.finish();
            dismiss();
        });

        Button btEditor = findViewById(R.id.bt_edit);
        btEditor.setOnClickListener(v -> {
            activity.setMode(ProfileControlActivity.MODE_TYPE.EDIT_MODE);
            dismiss();
        });

        tvHeader = findViewById(R.id.tv_header);

        if (!activity.checkProfileValid()){
            rvHubs.setVisibility(View.GONE);
            TextView tvHeader = findViewById(R.id.tv_header);
            TextView tvHint = findViewById(R.id.tv_hint);
            ImageView ivIcon = findViewById(R.id.iv_icon);
            tvHeader.setText(R.string.profile_not_ready);
            tvHint.setText(R.string.no_port_connections);
            //tvHint.setTextAlignment(TEXT_ALIGNMENT_TEXT_START);
            ivIcon.setBackground(null);
            ivIcon.setImageResource(android.R.drawable.ic_dialog_alert);
        }
        else {
            strConnecting = activity.getString(R.string.connecting);
            tvHeader.setText(strConnecting+".");

            checkAvailabilitiesTimer = new Timer();
            checkAvailabilitiesTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    if (checkAllAvailabilities())
                        dismiss();
                    activity.runOnUiThread(() -> updateTextView());
                }
            }, 1, 500);
        }
    }

    public void dismiss(){
        super.dismiss();
        if (activity.checkProfileValid() && checkAvailabilitiesTimer != null)
            checkAvailabilitiesTimer.cancel();
    }

    @SuppressLint("SetTextI18n")
    private void updateTextView(){
        int cnt = tvHeader.getText().length() - strConnecting.length();
        cnt = cnt >= 3? 1 : cnt+1;
        StringBuilder str = new StringBuilder();
        for (;cnt > 0; --cnt) str.append(".");
        tvHeader.setText(strConnecting + str);
    }

    private boolean checkAllAvailabilities(){
        boolean isAllAvailable = true;
        for (BluetoothHub hub : hubs)
            if (!hub.availability){
                isAllAvailable = false;
                break;
            }
        return isAllAvailable;
    }

    private class HubsListAdapter extends RecyclerView.Adapter<HubsListAdapter.ViewHolder>
                                                                implements IListViewAdapterForHubs {
        List<BluetoothHub> hubs;
        LayoutInflater inflater;
        final int itemId = R.layout.item_for_params_list;

        HubsListAdapter(Context context, List<BluetoothHub> hubs){
            this.hubs = hubs;
            inflater = LayoutInflater.from(context);

            if (checkAllAvailabilities())
                dialogContext.dismiss();
        }

        @Override
        public boolean addHub(BluetoothHub hub){ return false; }

        @Override
        public BluetoothHub removeHub(String address){ return null; }

        @SuppressLint("NotifyDataSetChanged")
        @Override
        public void setAvailability(boolean availability, BluetoothDevice device){
            BluetoothHub hub;
            for (short idx = 0; idx < hubs.size(); ++idx){
                hub = hubs.get(idx);
                if (hub.address.equals(device.getAddress())){
                    hub.availability = availability;
                    notifyDataSetChanged();
                    break;
                }
            }
            if (checkAllAvailabilities())
                dialogContext.dismiss();
        }

        @Override
        public int getItemCount(){ return hubs.size(); }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType){
            View convertView = inflater.inflate(itemId, parent, false);
            ViewHolder holder = new ViewHolder(convertView);
            holder.iv_menu_icon.setVisibility(View.VISIBLE);
            return holder;
        }

        @SuppressLint("UseCompatLoadingForDrawables")
        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position){
            if (hubs == null) return;
            BluetoothHub hub = hubs.get(position);

            holder.tv_name.setText(hub.getName());
            holder.iv_icon.setImageResource(hub.getIconId());
            int ivMenuIconId;
            if (hub.availability) ivMenuIconId = R.drawable.done;
            else ivMenuIconId = R.drawable.wait;
            holder.iv_menu_icon.setImageResource(ivMenuIconId);
        }

        class ViewHolder extends RecyclerView.ViewHolder{
            final View parentView;
            final TextView tv_name;
            final ImageView iv_menu_icon;
            final ImageView iv_icon;

            ViewHolder(View view){
                super(view);
                parentView = view;
                tv_name = view.findViewById(R.id.tv_name);
                iv_menu_icon = view.findViewById(R.id.iv_menu_icon);
                iv_icon = view.findViewById(R.id.iv_icon);
            }
        }
    }
}