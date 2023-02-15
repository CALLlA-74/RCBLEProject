package com.example.rcbleproject;

/**
 * Класс PortConnection содержит поля и методы для управления подключением к порту хаба.
 */
public class PortConnection{
    public BluetoothHub hub = null;
    public BluetoothHub.Port port = null;
    public BaseControlElement.ControllerAxis controllerAxis = null;

    public PortConnection() {}

    public PortConnection(BluetoothHub hub, BluetoothHub.Port port,
                          BaseControlElement.ControllerAxis controllerAxis){
        this.hub = hub;
        this.port = port;
        this.controllerAxis = controllerAxis;
    }
}
