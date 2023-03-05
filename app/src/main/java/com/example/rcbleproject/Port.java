package com.example.rcbleproject;

import android.content.Context;

public class Port extends BaseParam{
    public final Context context;
    private int direction = 0;
    public final BluetoothHub hub;
    public int portNum = -1;

    public Port(Context context, BluetoothHub hub){
        this.context = context;
        this.hub = hub;
    }

    public Port(Context context, BluetoothHub hub, int portNum, int direction){
        this.context = context;
        this.hub = hub;
        this.portNum = portNum;
        this.direction = direction;
    }

    public int getDirection() {return direction; }

    public void setDirection(int newDirection){
        direction = Integer.compare(newDirection, 0);
    }

    @Override
    public String getName() {
        switch (portNum){
            case 0:
                if (direction == 1) return context.getString(R.string.port_a);
                else return context.getString(R.string.port_a_inv);
            case 1:
                if (direction == 1) return context.getString(R.string.port_b);
                else return context.getString(R.string.port_b_inv);
            case 2:
                if (direction == 1) return context.getString(R.string.port_c);
                else return context.getString(R.string.port_c_inv);
            case 3:
                if (direction == 1) return context.getString(R.string.port_d);
                else return context.getString(R.string.port_d_inv);
        }
        return ".?.";
    }

    @Override
    public int getIconId(){
        switch (portNum){
            case 0:
                return R.drawable.letter_a;
            case 1:
                return R.drawable.letter_b;
            case 2:
                return R.drawable.letter_c;
            case 3:
                return R.drawable.letter_d;
        }

        return R.drawable.unknown_param;
    }

    @Override
    public int getMenuIconId(){
        if (direction < 0) return R.drawable.baseline_rotate_left_24;
        return R.drawable.baseline_rotate_right_24;
    }

    @Override
    public void act(Object obj){
        hub.setOutputPortCommand((BaseAppBluetoothActivity) obj, portNum, direction);
    }

    @Override
    public boolean getAvailabilityForAct() {return hub.availability; }

    @Override
    public boolean equals(Object obj){
        return (((Port)obj).portNum == portNum) && (((Port)obj).getDirection() == getDirection());
    }
}
