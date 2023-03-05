package com.example.rcbleproject;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class PortConnectionParamsDialog extends Dialog {
    public enum ParamTypes {HUB, PORT, CONTROLLER_AXIS}

    private final List<BaseParam> params;
    private final ParamTypes typeOfParam;
    private final SettingPortConnectionsActivity activity;
    private final PortConnection portConnection;

    private final Dialog dialogContext;

    RecyclerView lrVarsList;

    public PortConnectionParamsDialog(SettingPortConnectionsActivity activity, ParamTypes typeOfParam,
                                      PortConnection port, int currentDisplayIndex, int numOfDisplays,
                                      List<BaseParam> params){
        super(activity);
        this.activity = activity;
        dialogContext = this;
        this.params = params;
        this.typeOfParam = typeOfParam;
        portConnection = port;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.controlled_port_params_dialog);
        lrVarsList = findViewById(R.id.lr_list);
        ParamsListAdapter adapter = new ParamsListAdapter(getContext(), params);
        lrVarsList.setAdapter(adapter);
        activity.setLvAdapterConnectedDevices(adapter);
    }

    @Override
    public void dismiss(){
        super.dismiss();
        if (portConnection.port == null) return;
        if (typeOfParam == ParamTypes.PORT){
            List<Port> ports = (List) params;
            for (short idx = (short) (ports.size() - 1); idx >= 0; --idx){
                if (ports.get(idx).portNum == portConnection.port.portNum){
                    ports.remove(idx);
                }
            }
        }
    }

    private class ParamsListAdapter extends RecyclerView.Adapter<ParamsListAdapter.ViewHolder>
                                                                implements IListViewAdapterForHubs{
        List<BaseParam> paramsList;
        LayoutInflater inflater;
        final int itemId = R.layout.item_for_params_list;

        ParamsListAdapter(Context context, List<BaseParam> paramsList){
            this.paramsList = paramsList;
            inflater = LayoutInflater.from(context);
        }

        @Override
        public boolean addHub(BluetoothHub hub){ return false; }

        @Override
        public BluetoothHub removeHub(String address){ return null; }

        @SuppressLint("NotifyDataSetChanged")
        @Override
        public boolean setAvailability(boolean availability, BluetoothDevice device){
            if (typeOfParam == ParamTypes.HUB){
                BluetoothHub hub;
                for (short idx = 0; idx < paramsList.size(); ++idx){
                    hub = (BluetoothHub) paramsList.get(idx);
                    if (hub.address.equals(device.getAddress())){
                        hub.availability = availability;
                        notifyDataSetChanged();
                        return true;
                    }
                }
                return true;
            }
            return false;
        }

        @Override
        public int getItemCount(){ return paramsList.size(); }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType){
            View convertView = inflater.inflate(itemId, parent, false);
            ViewHolder holder = new ViewHolder(convertView);

            switch (typeOfParam){
                case HUB:
                case PORT:
                    holder.iv_menu_icon.setVisibility(View.VISIBLE);
                    break;
                case CONTROLLER_AXIS:
                    holder.iv_menu_icon.setVisibility(View.GONE);
            }

            return holder;
        }

        @SuppressLint("UseCompatLoadingForDrawables")
        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position){
            if (paramsList == null) return;
            BaseParam param = paramsList.get(position);

            switch (typeOfParam){
                case HUB:
                case PORT:
                    if (param.getAvailabilityForAct()){
                        holder.iv_menu_icon.setVisibility(View.VISIBLE);
                        holder.parentView.setOnLongClickListener((View v) -> {
                            switch (typeOfParam){
                                case HUB: param.act(activity);
                                case PORT: param.act(activity);
                            }
                            return true;
                        });
                    }
                    else {
                        holder.iv_menu_icon.setVisibility(View.GONE);
                        holder.parentView.setOnLongClickListener(null);
                    }
                    holder.parentView.setBackground(activity.getDrawable(R.drawable.rect_white_border));
                    holder.tv_name.setTextColor(activity.getColor(R.color.white));
                    holder.iv_menu_icon.setImageResource(param.getMenuIconId());
                    holder.iv_menu_icon.setOnClickListener((View v) -> param.act(activity));
            }

            holder.tv_name.setText(param.getName());
            holder.iv_icon.setImageResource(param.getIconId());

            holder.parentView.setOnClickListener((View v) -> {
                switch (typeOfParam){
                    case HUB:
                        if (!((BluetoothHub)param).equals(portConnection.hub))
                            portConnection.port = null;
                        portConnection.hub = (BluetoothHub) param;
                        break;
                    case PORT:
                        portConnection.port = (Port) param;
                        break;
                    case CONTROLLER_AXIS:
                        portConnection.controllerAxis = (BaseControlElement.ControllerAxis) param;
                }
                Log.v("APP_TAG5555", portConnection.toString());
                activity.notifyDataSetChanged();
                dialogContext.dismiss();
            });
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