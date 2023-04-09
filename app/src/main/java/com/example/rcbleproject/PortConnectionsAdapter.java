package com.example.rcbleproject;

import static com.example.rcbleproject.PortConnectionParamsDialog.ParamType.CONTROLLER_AXIS;
import static com.example.rcbleproject.PortConnectionParamsDialog.ParamType.HUB;
import static com.example.rcbleproject.PortConnectionParamsDialog.ParamType.PORT;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.rcbleproject.Database.DatabaseAdapterPortConnections;

import java.util.ArrayList;
import java.util.List;

public class PortConnectionsAdapter extends ArrayAdapter<PortConnection> {
    private final SettingPortConnectionsActivity activity;
    private static final int layout = R.layout.item_port_connection;
    private final ArrayList<Long> displayIds;
    public final List<PortConnection> portConnections;
    public final List<BluetoothHub> hubs;
    private final LayoutInflater inflater;
    private final int currentDisplayIndex, numOfDisplays;
    private final DatabaseAdapterPortConnections dbPortConn;

    boolean isPortsActive = true;

    private ArrayList<ArrayList<BaseControlElement.ControllerAxis>> controllersAxes;

    public PortConnectionsAdapter(SettingPortConnectionsActivity context, long currentDisplayID,
                                  int currentDisplayIndex, int numOfDisplays, List<PortConnection> portConnections){
        super(context, layout, portConnections);

        activity = context;
        this.portConnections = portConnections;
        inflater = LayoutInflater.from(context);
        this.hubs = Container.getDbForHubs(activity).getConnectedHubs(activity);
        this.currentDisplayIndex = currentDisplayIndex;
        this.numOfDisplays = numOfDisplays;
        this.dbPortConn = Container.getDbPortConnections(activity);
        initControllersAxes();
        displayIds = GameControllersDrawer.getDisplayIDs();
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
            holder.tv_port_name.setTextColor(activity.getColor(R.color.white));
            holder.v_port.setOnClickListener((View v) -> {
                ViewHolder vh = (ViewHolder) ((View)v.getParent()).getTag();
                PortConnection portConn = portConnections.get(vh.position);
                if (portConn.hub == null) {
                    Toast.makeText(activity, "Сперва выберете хаб", Toast.LENGTH_SHORT).show();
                    return;
                }

                List<Port> ports = activity.getHubPortsByDisplays().get(displayIds.get(currentDisplayIndex))
                        .get(portConnections.get(vh.position).hub.address);
                if (portConn.port != null){
                    Port p1 = new Port(activity, portConn.hub, portConn.port.portNum, 1);
                    Port p2 = new Port(activity, portConn.hub, portConn.port.portNum, -1);
                    ports.add(p1);
                    ports.add(p2);
                    portConn.port = p1.equals(portConn.port)? p1 : p2;
                    ports.sort((o1, o2) -> {
                        if (o1.portNum == o2.portNum)
                            return o2.getDirection() - o1.getDirection();
                        return o1.portNum - o2.portNum;
                    });
                }
                new PortConnectionParamsDialog(activity, PORT, portConnections.get(vh.position),
                        (List) ports).show();
            });
        }
        else {
            holder.v_port.setBackground(activity.getDrawable(R.drawable.rect_blue_ncs_border));
            holder.tv_port_name.setTextColor(activity.getColor(R.color.maximum_blue));
            holder.v_port.setOnClickListener(null);
            holder.v_port.setClickable(false);
        }
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
            viewHolder.position = position;

            viewHolder.v_hub.setOnClickListener((View v) ->
                    new PortConnectionParamsDialog(activity, HUB, portConnections.get(viewHolder.position),
                            (List) hubs).show());

            setPortsActiveness(viewHolder, isPortsActive);

            viewHolder.v_controller_axis.setOnClickListener((View v) -> {
                ViewHolder holder = (ViewHolder) ((View)v.getParent()).getTag();
                new PortConnectionParamsDialog(activity, CONTROLLER_AXIS,
                        portConnections.get(holder.position),
                        (List) controllersAxes.get(currentDisplayIndex)).show();
            });
            viewHolder.bt_delete_port_conn.setOnClickListener((View v) -> {
                int pos = ((ViewHolder)((View)v.getParent()).getTag()).position;
                dbPortConn.delete(activity, portConnections.remove(pos));
                //ViewHolder holder = (ViewHolder) ((View)v.getParent()).getTag();
                //Container.getDbPortConnections(activity).deletePortConnection(portConnections.get(holder.position));
                notifyDataSetChanged();
            });
        }
        else viewHolder = (ViewHolder) convertView.getTag();
        viewHolder.position = position;
        setConnectionPortInfo(viewHolder, portConnection);

        return convertView;
    }

    private void setConnectionPortInfo(ViewHolder holder, PortConnection portConnection){
        if (portConnection.hub != null){
            holder.tv_hub_name.setText(portConnection.hub.getName());
            holder.iv_hub_icon.setImageResource(portConnection.hub.getIconId());
        }
        else {
            holder.tv_hub_name.setText(activity.getString(R.string.unknown_hub_name));
            holder.iv_hub_icon.setImageResource(R.drawable.unknown_param);
        }

        if (portConnection.port != null){
            holder.tv_port_name.setText(portConnection.port.getName());
            holder.iv_port_icon.setImageResource(portConnection.port.getIconId());
        }
        else {
            holder.tv_port_name.setText(activity.getString(R.string.unknown_port_name));
            holder.iv_port_icon.setImageResource(R.drawable.unknown_param);
        }

        if (portConnection.controllerAxis != null){
            holder.tv_controller_axis_name.setText(portConnection.controllerAxis.getName());
            holder.iv_controller_axis_icon.setImageResource(portConnection.controllerAxis.getIconId());
        }
        else {
            holder.tv_controller_axis_name.setText(activity.getString(R.string.unknown_controller_axis_name));
            holder.iv_controller_axis_icon.setImageResource(R.drawable.unknown_param);
        }
    }

    public List<PortConnection> getPortConnections() { return portConnections; }

    private class ViewHolder{
        final View v_hub;
        final TextView tv_hub_name;
        final ImageView iv_hub_icon;

        final View v_port;
        final TextView tv_port_name;
        final ImageView iv_port_icon;

        final View v_controller_axis;
        final TextView tv_controller_axis_name;
        final ImageView iv_controller_axis_icon;

        final ImageButton bt_delete_port_conn;

        int position = -1;

        ViewHolder(View view){
            v_hub = view.findViewById(R.id.v_hub);
            tv_hub_name = v_hub.findViewById(R.id.tv_hub_name);
            iv_hub_icon = v_hub.findViewById(R.id.iv_hub_icon);

            v_port = view.findViewById(R.id.v_port);
            tv_port_name = v_port.findViewById(R.id.tv_port_name);
            iv_port_icon = v_port.findViewById(R.id.iv_port_icon);

            v_controller_axis = view.findViewById(R.id.v_controller_axis);
            tv_controller_axis_name = v_controller_axis.findViewById(R.id.tv_controller_axis_name);
            iv_controller_axis_icon = v_controller_axis.findViewById(R.id.iv_controller_axis_icon);

            bt_delete_port_conn = view.findViewById(R.id.bt_delete_port_connection);
        }
    }
}
