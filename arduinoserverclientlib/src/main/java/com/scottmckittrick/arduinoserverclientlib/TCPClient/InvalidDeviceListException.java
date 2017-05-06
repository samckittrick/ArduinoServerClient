package com.scottmckittrick.arduinoserverclientlib.TCPClient;

/**
 * Thrown when the device manager receives an invalid device list from the server.
 * Created by Scott on 4/23/2017.
 */

public class InvalidDeviceListException extends Exception {
    public InvalidDeviceListException(String message){
        super(message);
    }
}
