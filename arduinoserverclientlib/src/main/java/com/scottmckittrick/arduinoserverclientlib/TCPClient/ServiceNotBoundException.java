package com.scottmckittrick.arduinoserverclientlib.TCPClient;

/**
 * Created by Scott on 5/13/2017.
 */

public class ServiceNotBoundException extends Exception {
    public ServiceNotBoundException(String message) {
        super(message);
    }
}
