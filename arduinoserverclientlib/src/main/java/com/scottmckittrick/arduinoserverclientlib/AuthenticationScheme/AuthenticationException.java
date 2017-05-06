package com.scottmckittrick.arduinoserverclientlib.AuthenticationScheme;

/**
 * Indicates internal issues in the authentication scheme.
 * Created by Scott on 4/21/2017.
 */

public class AuthenticationException extends Exception {
    public AuthenticationException(String message)
    {
        super(message);
    }
}
