package com.scottmckittrick.arduinoserverclientlib.TCPClient;

/**
 * Thrown when an unknown device type is requested.
 * Created by Scott on 4/23/2017.
 */

public class UnknownDeviceTypeException extends Exception {
    public UnknownDeviceTypeException(String message) {
        super(message);
    }
}
