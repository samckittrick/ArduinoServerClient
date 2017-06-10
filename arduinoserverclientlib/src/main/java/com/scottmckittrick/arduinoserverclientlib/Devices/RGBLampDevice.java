package com.scottmckittrick.arduinoserverclientlib.Devices;

import android.util.Log;

import com.scottmckittrick.arduinoserverclientlib.RequestObject;

import java.util.ArrayList;

/**
 * Object representing a RGB Lamp Type Device
 * Created by Scott on 4/23/2017.
 */

public class RGBLampDevice extends BasicDevice {
    /** Device Type */
    public static final int DEVICE_TYPE = DeviceManager.DEVICE_TYPE_RGB_LAMP;

    /** Lamp off command. Value: 0x0010 */
    public static final short CMD_LAMP_OFF = 0x0010;
    /** Response to lamp off command. Value: 0x1000 */
    public static final short CMD_LAMP_OFF_RSP = 0x1000;
    /** Command to set the lamp to a solid color */
    public static final short CMD_LAMP_SOLID = 0x0011;
    /** Response to solid color command */
    public static final short CMD_LAMP_SOLID_RSP = 0x1100;
    /** Command to set the lamp to fade through all colors */
    public static final short CMD_LAMP_FADE = 0x0013;
    /** Response to the fade command */
    public static final short CMD_LAMP_FADE_RSP = 0x1300;
    /** Command to set the lamp to cycle through all colors */
    public static final short CMD_LAMP_CYCLE = 0x0014;
    /** Response to the cycle command */
    public static final short CMD_LAMP_CYCLE_RSP = 0x1400;
    /** Command to retrieve the status of the lamp */
    public static final short CMD_LAMP_GET_STATUS = 0x0015;
    /** Response to the get status */
    public static final short CMD_LAMP_GET_STATUS_RSP = 0x1500;

    public static final String TAG = "RGBLampDevice";

    /** The current state of the lamp */
    private int currentState;
    /** The current color of the lamp in RGB colors. { Red, Green, Blue } */
    private byte[] colors;
    /** The interval between steps in the fade transition or the interval between steps in the solid cycle in miliseconds */
    private int interval;
    /** The object wanting to receive notifications that this device's status has changed. */
    private DeviceChangeListener changeListener;

    /**
     * Construct RGBLampDevice
     * @param id Id of the device.
     * @param name Name of the device.
     */
    public RGBLampDevice(int id, String name, int deviceAddr)
    {
        super(id, name, deviceAddr);
        colors = new byte[3];
    }

    /**
     * Return the id of the device
     * @return DeviceId
     */
    public int getDeviceType()
    {
        return DEVICE_TYPE;
    }

    /**
     * Handle an incomming request.
     * @param r The request to be handled.
     */
    public void handleRequest(RequestObject r)
    {
        Log.d(TAG, "Handling Request Object");
        if(r.getDeviceAddress() != getDeviceAddr())
            return;

        Log.d(TAG, "Commaand: " + r.getCommand());

        byte[] d = r.getData();
        switch(r.getCommand()) {
            case CMD_LAMP_CYCLE_RSP:
                currentState = CMD_LAMP_CYCLE;
                interval = (d[0] << 8) | (d[1] & 0xFF);
                notifyChanges();
                break;
            case CMD_LAMP_FADE_RSP:
                currentState = CMD_LAMP_FADE;
                interval = (d[0] << 8) | (d[1] & 0xFF);
                notifyChanges();
                break;
            case CMD_LAMP_SOLID_RSP:
                currentState = CMD_LAMP_SOLID;
                colors[0] = d[0];
                colors[1] = d[1];
                colors[2] = d[2];
                notifyChanges();
                break;
            case CMD_LAMP_GET_STATUS_RSP:
                parseStatus(d);
                break;
            case CMD_LAMP_OFF_RSP:
                currentState = CMD_LAMP_OFF;
                notifyChanges();
                break;
            default:
                Log.e(TAG,"Unknown command received");
        }
    }

    /**
     * Command the lamp to turn off.
     */
    public void turnLampOff() {
        RequestObject r = new RequestObject(getDeviceAddr(), CMD_LAMP_OFF, null);
        getRequestReceiver().handleRequest(r);
    }

    /**
     * Set the lamp to a solid color
     * @param r Red color value 0-255
     * @param g Green color value 0-255
     * @param b Blue color value 0-255
     */
    public void setLampSolid(int r, int g, int b) {
        byte[] colors = new byte[3];
        colors[0] = (byte)(r & 0xFF);
        colors[1] = (byte)(g & 0xFF);
        colors[2] = (byte)(b & 0xFF);
        RequestObject req = new RequestObject(getDeviceAddr(), CMD_LAMP_SOLID, colors);
        getRequestReceiver().handleRequest(req);
    }

    /**
     * Set the lamp to fade through different colors.
     * @param interval The interval between steps in milliseconds during the fade.
     */
    public void setLampFade(int interval) {
        byte[] intervalArr = new byte[2];
        intervalArr[0] = (byte)((interval >> 8) & 0xFF);
        intervalArr[1] = (byte)(interval & 0xFF);
        RequestObject r = new RequestObject(getDeviceAddr(), CMD_LAMP_FADE, intervalArr);
        getRequestReceiver().handleRequest(r);
    }

    /**
     * Set the lamp to cycle through different colors.
     * @param interval The interval between colors in milliseconds during the cycle.
     */
    public void setLampCycle(int interval) {
        byte[] intervalArr = new byte[2];
        intervalArr[0] = (byte)((interval >> 8) & 0xFF);
        intervalArr[1] = (byte)(interval & 0xFF);
        RequestObject r = new RequestObject(getDeviceAddr(), CMD_LAMP_CYCLE, intervalArr);
        getRequestReceiver().handleRequest(r);
    }

    /**
     * Send a status request to this lamp.
     */
    public void sendGetStatus() {
        Log.d(TAG, "Getting Status");
        RequestObject r = new RequestObject(getDeviceAddr(), CMD_LAMP_GET_STATUS, null);
        if(getRequestReceiver() != null)
            getRequestReceiver().handleRequest(r);
        else
            Log.w(TAG, "RequestReceiver is null.");
    }

    /**
     * Parse incoming status messages, update the object and notify the client of changes.
     * @param data The status message to be parsed.
     */
    private void parseStatus(byte[] data) {
        int pointer = 0;
        short command = (short)((data[pointer++] << 8) | (data[pointer++] & 0xFF));
        currentState = command;
        if((command == CMD_LAMP_CYCLE) | (command == CMD_LAMP_FADE))
        {
            if(data == null) {
                Log.e(TAG, "Missing interval data in status response");
                return;
            }
            interval = (data[pointer++] << 8) | (data[pointer++] & 0xFF);
            notifyChanges();
        }
        else if(command == CMD_LAMP_SOLID)
        {
            if(data == null) {
                Log.e(TAG, "Invalid color data in status response");
                return;
            }
            colors[0] = data[pointer++];
            colors[1] = data[pointer++];
            colors[2] = data[pointer++];
            notifyChanges();
        }
        else if(command == CMD_LAMP_OFF) {
            notifyChanges();
        }
        else
        {
            Log.e(TAG, "Invalid status received");
            //ToDo send errors to client.
        }
    }

    /**
     * Register the client that will be listening for changes for this object.
     * @param d The object listening for changes.
     */
    public void setDeviceChangeListener(DeviceChangeListener d)
    {
        changeListener = d;
    }

    /**
     * Notify the client of any changes that have been made.
     */
    private void notifyChanges()
    {
        ArrayList<BasicDevice> arr = new ArrayList<>(1);
        arr.add(this);
        if(changeListener != null)
            changeListener.onDeviceChange(arr);
    }

    /**
     * Get the current state of the lamp.
     * @return Integer representing the current state of the lamp.
     */
    public int getCurrentState()
    {
        return currentState;
    }


}
