package com.scottmckittrick.arduinoserverclientlib.TCPClient;

/**
 * Exception thrown when a device isn't found in the device list.
 * Created by Scott on 4/23/2017.
 */

public class DeviceNotFoundException extends Exception {
    public DeviceNotFoundException(String message) {
        super(message);
    }
}
