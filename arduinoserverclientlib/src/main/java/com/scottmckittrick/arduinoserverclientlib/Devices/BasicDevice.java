package com.scottmckittrick.arduinoserverclientlib.Devices;

import com.scottmckittrick.arduinoserverclientlib.RequestObject;

/**
 * Generic representation of a device to be controlled by the client.
 * This interface should be overridden by specific device types.
 * Note that each specific class is responsible for knowing its type.
 * Created by Scott on 4/23/2017.
 */

public abstract class BasicDevice implements RequestObject.RequestReceiver {
    private int deviceId;
    private int deviceType;
    private String deviceName;
    private int deviceAddr;
    private RequestObject.RequestReceiver receiver;

    /**
     * Constructor to create new device.
     * @param deviceId Id of the device.
     * @param deviceName The name of the device.
     * @param deviceAddr The address of the device
     */
    public BasicDevice(int deviceId, String deviceName, int deviceAddr) {
        this.deviceId = deviceId;
        this.deviceName = deviceName;
        this.deviceAddr = deviceAddr;
    }

    /**
     * Get the id of the device.
     * @return The device id.
     */
    public int getDeviceId(){
        return deviceId;
    }

    /**
     * Get the name of the device.
     * @return The device name.
     */
    public String getDeviceName(){
        return deviceName;
    }

    /**
     * Get the address of the device
     * @return The device address
     */
    public int getDeviceAddr() { return deviceAddr; }

    /**
     * Get the type of the device
     * @return Integer representing the device type.
     */
    public abstract int getDeviceType();

    /**
     * Handle incoming requests.
     * @param r The request to be handled.
     */
    @Override
    public abstract void handleRequest(RequestObject r);

    /**
     * Set the request receiver that this object will send to.
     * @param r Receiver to send to.
     */
    public void setRequestReceiver(RequestObject.RequestReceiver r)
    {
        receiver = r;
    }

    /**
     * Get the request receiver this object should send requests to.
     * @return The request receiver for this object.
     */
    protected RequestObject.RequestReceiver getRequestReceiver() { return receiver; }
}
