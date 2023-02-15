package com.example.rcbleproject;

import static com.example.rcbleproject.ConnectionPortParamsDialog.ParamTypes.CONTROLLER_AXIS;
import static com.example.rcbleproject.ConnectionPortParamsDialog.ParamTypes.HUB;
import static com.example.rcbleproject.ConnectionPortParamsDialog.ParamTypes.PORT;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

public class PortConnectionsAdapter extends ArrayAdapter<PortConnection> {
    private final SettingPortConnectionsActivity activity;
    private static final int layout = R.layout.item_port_connection;
    public final ArrayList<PortConnection> portConnections;
    public final ArrayList<BluetoothHub> hubs;
    private final LayoutInflater inflater;
    private final ConnectedDevicesAdapterForControlledPorts connectedDevicesAdapter;
    private final int currentDisplayIndex, numOfDisplays;

    boolean isPortsActive = true;

    private ArrayList<ArrayList<BaseControlElement.ControllerAxis>> controllersAxes;

    public PortConnectionsAdapter(SettingPortConnectionsActivity context, long currentDisplayID,
                                  int currentDisplayIndex, int numOfDisplays, ArrayList<PortConnection> portConnections){
        super(context, layout, portConnections);

        activity = context;
        this.portConnections = portConnections;
        inflater = LayoutInflater.from(context);
        this.hubs = Container.getDbForHubs(activity).getConnectedHubs(activity);
        this.currentDisplayIndex = currentDisplayIndex;
        this.numOfDisplays = numOfDisplays;
        connectedDevicesAdapter = new ConnectedDevicesAdapterForControlledPorts(activity, hubs);
        initControllersAxes();
    }

    private void initControllersAxes(){
        controllersAxes = new ArrayList<>(numOfDisplays);
        ArrayList<BaseControlElement.ControllerAxis> axesOnDisplay;
        ArrayList<ArrayList<BaseControlElement>> elements = GameControllersDrawer.getElementsControl();
        for (int idx = 0; idx < numOfDisplays; ++idx){
            axesOnDisplay = new ArrayList<>(elements.get(idx).size()*2);
            for (BaseControlElement element : elements.get(idx)){
                axesOnDisplay.addAll(element.getControllerAxes());
            }
            controllersAxes.add(axesOnDisplay);
        }
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    public void setPortsActiveness(ViewHolder holder, boolean activeness){
        isPortsActive = activeness;
        if (isPortsActive){
            holder.v_port.setBackground(activity.getDrawable(R.drawable.rect_white_border));
            ((TextView)holder.v_port.findViewById(R.id.tv_port_name)).setTextColor(activity.getColor(R.color.white));
            holder.v_port.setOnClickListener((View v) -> {
                if (portConnections.get(holder.position).hub == null) return;
                new ConnectionPortParamsDialog(activity, PORT, portConnections.get(holder.position),
                        currentDisplayIndex, numOfDisplays,
                        (ArrayList) portConnections.get(holder.position).hub.getPorts()).show();
            });
        }
        else {
            holder.v_port.setBackground(activity.getDrawable(R.drawable.rect_blue_ncs_border));
            ((TextView)holder.v_port.findViewById(R.id.tv_port_name)).setTextColor(activity.getColor(R.color.maximum_blue));
            holder.v_port.setOnClickListener(null);
            holder.v_port.setClickable(false);
        }
    }

    public ConnectedDevicesAdapterForControlledPorts getConnectedDevicesAdapter(){
        return connectedDevicesAdapter;
    }

    @SuppressLint("MissingPermission")
    @Override
    public View getView(int position, View convertView, ViewGroup parent){
        final ViewHolder viewHolder;
        PortConnection portConnection = portConnections.get(position);
        if (convertView == null){
            convertView = inflater.inflate(layout, parent, false);
            viewHolder = new ViewHolder(convertView);
            convertView.setTag(viewHolder);

            viewHolder.v_hub.setOnClickListener((View v) ->
                    new ConnectionPortParamsDialog(activity, HUB, portConnections.get(viewHolder.position),
                            currentDisplayIndex, numOfDisplays, (ArrayList) hubs).show());

            setPortsActiveness(viewHolder, isPortsActive);

            viewHolder.v_controller_axis.setOnClickListener((View v) ->
                    new ConnectionPortParamsDialog(activity, CONTROLLER_AXIS,
                            portConnections.get(viewHolder.position), currentDisplayIndex, numOfDisplays,
                            (ArrayList) controllersAxes.get(currentDisplayIndex)).show());
        }
        else viewHolder = (ViewHolder) convertView.getTag();
        viewHolder.position = position;
        setConnectionPortInfo(viewHolder, portConnection);

        return convertView;
    }

    private void setConnectionPortInfo(ViewHolder holder, PortConnection portConnection){
        if (portConnection.hub != null){
            ((TextView)holder.v_hub.findViewById(R.id.tv_hub_name)).setText(portConnection.hub.getName());
            ((ImageView)holder.v_hub.findViewById(R.id.iv_hub_icon)).setImageResource(portConnection.hub.getIconId());
        }
        else {
            ((TextView)holder.v_hub.findViewById(R.id.tv_hub_name)).setText(activity.getString(R.string.unknown_hub_name));
            ((ImageView)holder.v_hub.findViewById(R.id.iv_hub_icon)).setImageResource(R.drawable.unknown_param);
        }

        if (portConnection.port != null){
            ((TextView)holder.v_port.findViewById(R.id.tv_port_name)).setText(portConnection.port.getName());
            ((ImageView)holder.v_port.findViewById(R.id.iv_port_icon)).setImageResource(portConnection.port.getIconId());
        }
        else {
            ((TextView)holder.v_port.findViewById(R.id.tv_port_name)).setText(activity.getString(R.string.unknown_port_name));
            ((ImageView)holder.v_port.findViewById(R.id.iv_port_icon)).setImageResource(R.drawable.unknown_param);
        }

        if (portConnection.controllerAxis != null){
            ((TextView)holder.v_controller_axis.findViewById(R.id.tv_controller_axis_name)).setText(portConnection.controllerAxis.getName());
            ((ImageView)holder.v_controller_axis.findViewById(R.id.iv_controller_axis_icon)).setImageResource(portConnection.controllerAxis.getIconId());
        }
        else {
            ((TextView)holder.v_controller_axis.findViewById(R.id.tv_controller_axis_name)).setText(activity.getString(R.string.unknown_controller_axis_name));
            ((ImageView)holder.v_controller_axis.findViewById(R.id.iv_controller_axis_icon)).setImageResource(R.drawable.unknown_param);
        }
    }

    private class ViewHolder{
        final View v_hub;
        final View v_port;
        final View v_controller_axis;
        int position = -1;
        ViewHolder(View view){
            v_hub = view.findViewById(R.id.v_hub);
            v_port = view.findViewById(R.id.v_port);
            v_controller_axis = view.findViewById(R.id.v_controller_axis);
        }
    }
}
