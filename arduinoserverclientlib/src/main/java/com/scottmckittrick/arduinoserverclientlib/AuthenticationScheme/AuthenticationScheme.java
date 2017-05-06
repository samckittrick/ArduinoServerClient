package com.scottmckittrick.arduinoserverclientlib.AuthenticationScheme;

import android.os.Parcelable;

/**
 * Interface representing Authentication Schemes
 * It is implemented as an abstract class due to the access limitations of interfaces.
 * You cannot have an interface with package private methods
 * Created by Scott on 4/21/2017.
 */

public abstract class AuthenticationScheme implements Parcelable{
    /**
     * Generate authentication byte string to be sent to server
     * @param challengeMessage Returned byte string from server. Null if first request.
     * @return Byte string to be sent to the server
     * @throws InvalidAuthenticationMessageException Invalid message is received from the server
     * @throws AuthenticationException Internal errors in authentication process.
     */
     protected abstract byte[] authenticate(byte[] challengeMessage) throws InvalidAuthenticationMessageException, AuthenticationException;

    /**
     * Returns the name of the scheme. Used when negotiating a scheme with the server
     * @return Name of the Authentication Scheme
     */
    public abstract String getSchemeName();
}
