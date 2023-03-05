package com.example.rcbleproject;

/**
 * Класс PortConnection содержит поля и методы для управления подключением к порту хаба.
 */
public class PortConnection{
    public BluetoothHub hub = null;
    public Port port = null;
    public BaseControlElement.ControllerAxis controllerAxis = null;

    private long id, displayID;

    public PortConnection(long id, long displayID) {
        this.id = id;
        this.displayID = displayID;
    }

    public PortConnection(long id, long displayID, BluetoothHub hub, Port port,
                          BaseControlElement.ControllerAxis controllerAxis){
        this.id = id;
        this.displayID = displayID;
        this.hub = hub;
        this.port = port;
        this.controllerAxis = controllerAxis;
    }

    public long getId() { return id; }

    public long getDisplayID() { return displayID; }
}
