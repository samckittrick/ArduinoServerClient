package com.scottmckittrick.arduinoserverclientlib.AuthenticationScheme;

import java.io.UnsupportedEncodingException;

/**
 * Class for managing authentication to the server.
 *
 * Created by Scott on 4/21/2017.
 */

public class Authenticator {
    /**
     * Client hello identifier. Value is 0x01
     */
    public static final byte AUTHTYPE_CLIENT_HELLO = 0x01;
    /**
     * Server hello identifier. Value is 0x02
     */
    public static final byte AUTHTYPE_SERVER_HELLO = 0x02;
    /**
     * Auth request identifier. Value is 0x03
     */
    public static final byte AUTHTYPE_AUTHREQ = 0x03;
    /**
     * Auth Success response identifier. Value is 0x04
     */
    public static final byte AUTHTYPE_RSPSUCCESS = 0x04;
    /**
     * Request for more info. Value is 0x05
     */
    public static final byte AUTHTYPE_RSPMOREINFO = 0x05;

    private enum AuthState { UNAUTHENTICATED, SCHEME_SELECTED, AUTHENTICATED }

    private AuthenticationScheme authScheme;
    private boolean isAuthenticated;
    private AuthState authState;

    /**
     * Constructor to create an authenticator
     * @param a The chosen Authentication scheme.
     */
    public Authenticator(AuthenticationScheme a)
    {
        authScheme = a;
        isAuthenticated = false;
        authState = AuthState.UNAUTHENTICATED;
    }

    /**
     * Returns whether or not a client is authenticated.
     * @return True if authenticated, false if not.
     */
    public boolean getAuthenticated()
    {
        return isAuthenticated;
    }

    /**
     * Takes an auth packet from the socket and processes it.
     * @param packet The incoming packet or null if this is the first call.
     * @return A response to be sent back to the server. Or null if no response.
     * @throws AuthenticationException Throws AuthenticationException when there is an internal error in authentication
     * @throws InvalidAuthenticationMessageException Throws InvalidAuthenticationMessageException when the auth response from the server is invalid.
     */
    public byte[] handleAuthPacket(byte[] packet) throws AuthenticationException, InvalidAuthenticationMessageException
    {
        //ToDo Reset object after exception.
        byte[] rsp = null;
        //If this is the first call, select a scheme.
        if((authState == AuthState.UNAUTHENTICATED) && (packet == null)) {
            try {
                String schemeName = authScheme.getSchemeName();
                byte[] nameBytes = schemeName.getBytes("US-ASCII");
                rsp = new byte[nameBytes.length + 1];

                //Set the packet type flag
                rsp[0] = AUTHTYPE_CLIENT_HELLO;

                //Add the name of the scheme
                System.arraycopy(nameBytes, 0, rsp, 1, nameBytes.length);
            }catch(UnsupportedEncodingException e)
            {
                throw new AuthenticationException("Unsupported encoding when building client hello");
            }
        //If this is the second call, we should have gotten a response from the server, accepting or rejecting the scheme
        } else if((authState == AuthState.UNAUTHENTICATED) && (packet != null)) {

            //Start by making sure we got a server hello
            if(packet.length == 0)
                throw new InvalidAuthenticationMessageException("Empty response from server at client hello");
            if(packet[0] != AUTHTYPE_SERVER_HELLO)
                throw new InvalidAuthenticationMessageException("Invalid response from server at client hello");

            //If we did, make sure the server didn't reject us.
            if((packet.length == 2) && (packet[1] == 0))
                return null; //ToDo update server to close connection when it rejects the scheme.

            //Check that the scheme matches the one we requested.
            //Note that the first byte in the packet is the type
            try {
                String respScheme = new String(packet, 1, packet.length - 1, "US-ASCII");
                if (respScheme.equals(authScheme.getSchemeName())) {
                    authState = AuthState.SCHEME_SELECTED;
                    //Begin the authentication process. The first request always inserts null data.
                    byte[] response = authScheme.authenticate(null);
                    rsp = new byte[response.length + 1];
                    rsp[0] = AUTHTYPE_AUTHREQ;
                    System.arraycopy(response, 0, rsp, 1, response.length);
                }
            }catch(UnsupportedEncodingException e){
                throw new AuthenticationException("Unsupported encoding when parsing server hello");
            }
        //At this point the client will have sent an auth req and the server should be returning a result.
        }else if((authState == AuthState.SCHEME_SELECTED)) {
            if(packet == null)
                throw new InvalidAuthenticationMessageException("No response received from the server after auth request");

            if((packet[0] != AUTHTYPE_RSPSUCCESS) && packet[0] != AUTHTYPE_RSPMOREINFO)
                throw new InvalidAuthenticationMessageException("Invalid response from server after auth request.");

            //If we receive a success, don't send back anything. Just record the state.
            if(packet[0] == AUTHTYPE_RSPSUCCESS) {
                authState = AuthState.AUTHENTICATED;
                isAuthenticated = true;
                return null;
            }else {
                //If it was more info, extract the response and send it to the auth scheme.
                //Send back any client response to the server
                byte[] serverResponse = new byte[packet.length - 1];
                System.arraycopy(packet, 1, serverResponse, 0, packet.length - 1);
                byte[] response = authScheme.authenticate(serverResponse);
                rsp = new byte[response.length + 1];
                rsp[0] = AUTHTYPE_AUTHREQ;
                System.arraycopy(response, 0, rsp, 1, response.length);
            }
        //An authscheme on the server may potentially timeout the authentication and request more info.
        } else if(authState == AuthState.AUTHENTICATED) {
            if(packet == null)
                throw new InvalidAuthenticationMessageException("No response received from server after reauth request");

            if(packet[0] != AUTHTYPE_RSPMOREINFO)
                throw new InvalidAuthenticationMessageException("Invalid response from server after reauth request");

            //If the server is requesting reauthentication, drop back down to scheme selected
            authState = AuthState.SCHEME_SELECTED;
            isAuthenticated = false;

            //Parse server message and send response.
            byte[] serverResponse = new byte[packet.length - 1];
            System.arraycopy(packet, 1, serverResponse, 0, packet.length - 1);
            byte[] response = authScheme.authenticate(serverResponse);
            rsp = new byte[response.length + 1];
            rsp[0] = AUTHTYPE_AUTHREQ;
            System.arraycopy(response, 0, rsp, 1, response.length);
        }

        return rsp;
    }
}
