package com.scottmckittrick.arduinoserverclientlib;

/**
 * Created by Scott on 5/2/2017.
 */

public class InvalidRequestDataException extends Exception {
    public InvalidRequestDataException(String m) {
        super(m);
    }
}
