package com.scottmckittrick.arduinoserverclientlib.TCPClient;

import com.scottmckittrick.arduinoserverclientlib.RequestObject;

/**
 * Created by Scott on 4/23/2017.
 */

public class RGBLampDevice extends BasicDevice {
    public static final int DEVICE_TYPE = DeviceManager.DEVICE_TYPE_RGB_LAMP;

    /**
     * Construct RGBLampDevice
     * @param id Id of the device.
     * @param name Name of the device.
     */
    public RGBLampDevice(int id, String name)
    {
        super(id, name);
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
        //ToDo Implement request handler.
    }

    //ToDo Implement lamp commands.
}
