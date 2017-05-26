package com.scottmckittrick.arduinoserverclientlib.Devices;

import java.util.ArrayList;

/**
 * Objects implementing this interface can receive notifications about changes to one or more devices.
 * Created by Scott on 5/25/2017.
 */

public interface DeviceChangeListener {
    /**
     * Interface for receiving notifications about changes to one or more device.
     * @param changedDevices List of devices that changed.
     */
    void onDeviceChange(ArrayList<BasicDevice> changedDevices);
}
