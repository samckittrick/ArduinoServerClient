package com.scottmckittrick.arduinoserverclientlib.AuthenticationScheme;

/**
 * Thrown when the auth message received from the server is invalid.
 * Created by Scott on 4/21/2017.
 */

public class InvalidAuthenticationMessageException extends Exception {
    public InvalidAuthenticationMessageException(String message)
    {
        super(message);
    }
}
