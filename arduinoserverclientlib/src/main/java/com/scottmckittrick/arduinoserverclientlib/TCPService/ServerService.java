package com.scottmckittrick.arduinoserverclientlib.TCPService;

import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

import com.scottmckittrick.arduinoserverclientlib.AuthenticationScheme.AuthenticationException;
import com.scottmckittrick.arduinoserverclientlib.AuthenticationScheme.AuthenticationScheme;
import com.scottmckittrick.arduinoserverclientlib.AuthenticationScheme.Authenticator;
import com.scottmckittrick.arduinoserverclientlib.AuthenticationScheme.InvalidAuthenticationMessageException;
import com.scottmckittrick.arduinoserverclientlib.InvalidRequestDataException;
import com.scottmckittrick.arduinoserverclientlib.RequestObject;

import java.io.IOException;
import java.net.ConnectException;
import java.util.ArrayList;

/**
 * Object representing a server that can be connected to.
 * Created by Scott on 5/6/2017.
 */

public class ServerService extends Service implements PacketReceiver {
    //Service Message Types
    /** Service Message type indicating a client should be registered for callbacks. */
    public static final int MSG_REGISTER_CLIENT = 1;
    /** Service Message type indicating a client should be unregistered for callbacks. */
    public static final int MSG_UNREGISTER_CLIENT = 2;
    /** Service Message Type indicating that the message contains a selected Authentication scheme. */
    public static final int MSG_AUTHSCHEME_SELECT = 3;
    /** Service Message type indicating the service should connect to the server. */
    public static final int MSG_CONNECT_SERVER = 4;
    /** Service Message type indicating that the service should disconnect from the server. */
    public static final int MSG_DISCONNECT_SERVER = 5;
    /** Service Message type indicating that the connection to the server succeeded. */
    public static final int MSG_CONNECT_SUCCESS = 6;
    /** Service Message type indicating that the connection to the server failed. */
    public static final int MSG_CONNECT_FAILURE = 7;
    /** Service Message type indicating that the authentication to the server succeeded. */
    public static final int MSG_AUTHENTICATION_SUCCEEDED = 8;
    /** Service Message type inidicating that the authentication to the server failed */
    public static final int MSG_AUTHENTICATION_FAILED = 9;
    /** Service Message type indicating that the message contains a request object to be passed to the server. */
    public static final int MSG_REQUEST_OBJECT = 10;
    /** Service Message type indicating that the request object was invalid */
    public static final int MSG_REQUEST_SEND_FAILED = 11;
    /** Service Message type indicating that the server was disconnected. */
    public static final int MSG_SERVER_DISCONNECTED = 12;

    //Bunclde keys
    /** Bundle key name for saving and accessing an AuthenticationScheme stored in a bundle. */
    public static final String KEY_AUTHSCHEME = "authenticationScheme";
    /** Bundle key name for saving and accessing a request object */
    public static final String KEY_REQUEST_OBJECT = "requestObject";
    /** Bundle key name for server ip */
    public static final String KEY_SERVER_IP = "serverIp";
    /** Bundle key name for server port */
    public static final String KEY_SERVER_PORT = "serverPort";
    /** Bundle key name for error message */
    public static final String KEY_ERROR_MESSAGE = "errorMessage";

    /** Logging Tag */
    public static final String TAG = "ArduinoServerService";


    /** Messenger used as a handle to send messeges to the clients of the service.*/
    private ArrayList<Messenger> clientMessenger;
    /** Messenger sent to client for the client to use to communicate with the service */
    private Messenger serviceMessenger;

    /** Connection object for communication with the server */
     private Connection conn;
    /** Authenticator object for authenticating to the server */
    private Authenticator authenticator;
    /** Boolean indicating whether or not a server connectin is active. */
    private boolean isConnected = false;

    /**
     * Create a new server service.
     */
    public ServerService()
    {
        clientMessenger = new ArrayList<>(2);
    }

    @Override
    public IBinder onBind(Intent i) {
        if(serviceMessenger == null)
            serviceMessenger = new Messenger(new ServerServiceHandler());
        return serviceMessenger.getBinder();
    }

    //Do some cleanup
    @Override
    public boolean onUnbind(Intent i)
    {
        try {
            conn.disconnect();
        }catch(IOException e)
        {
            Log.e(TAG, "Error disconnecting socket");
        }
        conn = null;
        isConnected = false;
        return false;
    }

    /*-*************************************************************************
     * Handle the Requests from the client
     *************************************************************************/
    /**
     * Registers a client to receive messages from the server service
     * @param m Messenger to send messages to.
     */
    private void registerClient(Messenger m)
    {
        if(m == null)
        {
            Log.e(TAG, "Client is Null");
            return;
        }

        Log.i(TAG, "Registering Client");
        if(!clientMessenger.contains(m))
            clientMessenger.add(m);
    }

    /**
     * Unregister a client so it stops receiving messages from the server service.
     * @param m Client messenger to unregister.
     */
    private void unregisterClient(Messenger m)
    {
        if(m == null)
        {
            Log.e(TAG, "Client is Null");
            return;
        }

        Log.i(TAG, "Unregistering Client");
        if(clientMessenger.contains(m))
            clientMessenger.remove(m);
    }

    /**
     * Set the authentication scheme from a request from the client
     * @param b Bundle containing the requested authentication scheme.
     */
    private void setAuthenticationScheme(Bundle b)
    {
        Log.d(TAG, "Registering Authentication Scheme");
        if(isConnected) {
            Log.w(TAG, "Server is already connected. Not adding auth scheme");
            return;
        }

        AuthenticationScheme auth = b.getParcelable(KEY_AUTHSCHEME);
        if(auth == null) {
            Log.e(TAG, "No Auth scheme provided");
            return;
        }

        authenticator = new Authenticator(auth);
    }

    /**
     * Actually connect to the server.
     * @param data Bundle containing the IP and port of the server.
     */
    private void connectServer(Bundle data)
    {
        Log.i(TAG, "Connecting to server");
        String ip = data.getString(KEY_SERVER_IP);
        int port = data.getInt(KEY_SERVER_PORT);

        //If they didn't send us an IP, we cannot connect to the server
        if(ip == null)
        {
            Log.e(TAG, "Server ip cannot be null");
            Bundle rspBundle = new Bundle();
            rspBundle.putString(KEY_ERROR_MESSAGE, "IP Cannot be null");
            sendMessage(MSG_CONNECT_FAILURE, rspBundle);
            return;
        }

        //If we are already connected, we shouldn't be trying again.
        if(isConnected)
        {
            Log.w(TAG, "Connection is already set up.");
            sendMessage(MSG_CONNECT_SUCCESS, null);
        }
        else {
            //Otherwise lets create the connection
            try {
                conn = new Connection(ip, port);
                conn.setPacketReceiver(this);
                conn.connect();
            }
            catch(ConnectException e)
            {
                Log.e(TAG, "Error connecting socket: " + e.getMessage());
                Bundle rspBundle = new Bundle();
                rspBundle.putString(KEY_ERROR_MESSAGE, "Error connecting to socket");
                sendMessage(MSG_CONNECT_FAILURE, rspBundle);
                return;
            }
        }

        //Once the connection is sent, we need to initiate the auth process
        //But if the authenticator is null, we can't do that.
        if(authenticator == null)
        {
            Log.e(TAG, "Authenticator is null");
            Bundle rspBundle = new Bundle();
            rspBundle.putString(KEY_ERROR_MESSAGE, "Authentication scheme not selected");
            sendMessage(MSG_AUTHENTICATION_FAILED, rspBundle);
            return;
        }

        //If we are already authorized, then we'll return success right away.
        if(authenticator.getAuthenticated())
        {
            Log.i(TAG, "Authentication complete.");
            sendMessage(MSG_AUTHENTICATION_SUCCEEDED, null);
        }
        else {
            //Start to authenticate the connection
            Log.i(TAG, "Starting Authentication Process");
            try {
                authenticator.handleAuthPacket(null);
            }catch(AuthenticationException e) {
                Log.e(TAG, "Authentication failed: " + e.getMessage());
                Bundle rspBundle = new Bundle();
                rspBundle.putString(KEY_ERROR_MESSAGE, "Authentication Failed");
                sendMessage(MSG_AUTHENTICATION_FAILED, rspBundle);
                return;
            }catch(InvalidAuthenticationMessageException e){
                Log.e(TAG, "Server responded with invalid authentication message: " + e.getMessage());
                Bundle rspBundle = new Bundle();
                rspBundle.putString(KEY_ERROR_MESSAGE, "Authentication failed. Server sent an invalid response.");
                sendMessage(MSG_AUTHENTICATION_FAILED, rspBundle);
                return;
            }
        }
    }

    /**
     * Send messages back to the clients.
     * @param messageType The type of message to be sent
     * @param rsp A bundle containing any data that should be sent back as well.
     */
    private void sendMessage(int messageType, Bundle rsp)
    {
        Message msg = Message.obtain(null, messageType);
        if(rsp != null)
                msg.setData(rsp);

        //Loop through all the registered clients and send the message to each.
        for(int i = 0; i <  clientMessenger.size(); i++) {
            try {
                clientMessenger.get(i).send(msg);
            } catch (RemoteException e) {
                Log.e(TAG, "Error sending to client: " + e.getMessage());
            }
        }
    }

    /**
     * Disconnect from the server.
     */
    //ToDo how do I handle the server closing the connection?
    private void disconnectServer()
    {
        try {
            conn.disconnect();
        }catch(ConnectException e) {
            Log.e(TAG, "Error disconnecting from server: " + e.getMessage());
        }

        //Destroy the connection objects
        conn = null;
        isConnected = false;
        sendMessage(MSG_SERVER_DISCONNECTED, null);
    }

    /**
     * Handle incoming requests.
     * @param data A bundle holding the request object from the client.
     */
    private void handleRequest(Bundle data) {
        RequestObject req = data.getParcelable(KEY_REQUEST_OBJECT);
        if(req == null)
        {
            Log.e(TAG, "Request Object is null");
            Bundle b = new Bundle();
            b.putString(KEY_ERROR_MESSAGE, "Request object cannot be null");
            sendMessage(MSG_REQUEST_SEND_FAILED, b);
            return;
        }else if(!isConnected) { //If we arne't connected then we can't send requests.
            Log.e(TAG, "Server is not yet connected.");
            Bundle b = new Bundle();
            b.putString(KEY_ERROR_MESSAGE, "Not connected to server yet.");
            sendMessage(MSG_REQUEST_SEND_FAILED, b);
            return;
        } else { //If we are ready to send a request, send it.
            try {
                conn.writePacket(new PacketConstants.Packet(PacketConstants.PACKET_TYPE_DATA, RequestObject.serializeRequestObject(req)));
            } catch(IOException e){
                Log.e(TAG, "Errror serializing request: " + e.getMessage());
                Bundle b = new Bundle();
                b.putString(KEY_ERROR_MESSAGE, "Error sending request");
                sendMessage(MSG_REQUEST_SEND_FAILED, b);
            }
        }
    }


    /**
     * Class representing the message handler for this service
     */
    public class ServerServiceHandler extends Handler {
        @Override
        public void handleMessage(Message m)
        {
            int messageType = m.what;
            switch(messageType)
            {
                case MSG_REGISTER_CLIENT:
                    registerClient(m.replyTo);
                    break;
                case MSG_UNREGISTER_CLIENT:
                    unregisterClient(m.replyTo);
                    break;
                case MSG_AUTHSCHEME_SELECT:
                    setAuthenticationScheme(m.getData());
                    break;
                case MSG_CONNECT_SERVER:
                    connectServer(m.getData());
                    break;
                case MSG_DISCONNECT_SERVER:
                    disconnectServer();
                    break;
                case MSG_REQUEST_OBJECT:
                    handleRequest(m.getData());
                    break;
                //The following messages should be sent from service to client, not the other way around.
                //Ignore them if they come from the client.
                case MSG_AUTHENTICATION_FAILED:
                case MSG_AUTHENTICATION_SUCCEEDED:
                case MSG_CONNECT_FAILURE:
                case MSG_CONNECT_SUCCESS:
                    break;
                default:
                    super.handleMessage(m);
            }
        }
    }


    /*-********************************************
    * Handle packets received from the server
     **********************************************/
    @Override
    public void onPacketReceived(PacketConstants.Packet p)
    {
        if(p.getType() == PacketConstants.PACKET_TYPE_AUTH) {
            try {
                //Handle the authentication packets.
                byte[] response = authenticator.handleAuthPacket(p.getData());
                if(authenticator.getAuthenticated())
                {
                    sendMessage(MSG_AUTHENTICATION_SUCCEEDED, null);
                }

                //If there is a response to send back
                if(response != null)
                {
                    try {
                        conn.writePacket(new PacketConstants.Packet(PacketConstants.PACKET_TYPE_AUTH, response));
                    }catch(IOException e) {
                        Log.e(TAG, "Error sending authentication messages: " + e.getMessage());
                        Bundle b = new Bundle();
                        b.putString(KEY_ERROR_MESSAGE, "Error sending authentication messages");
                        sendMessage(MSG_REQUEST_SEND_FAILED, b);
                        return;
                    }
                }
            }
            catch(AuthenticationException e){
                Log.e(TAG, "Error sending authentication messages: " + e.getMessage());
                Bundle b = new Bundle();
                b.putString(KEY_ERROR_MESSAGE, "Error sending authentication messages");
                sendMessage(MSG_REQUEST_SEND_FAILED, b);
                return;
            } catch(InvalidAuthenticationMessageException e) {
                Log.e(TAG, "Invalid server response: " + e.getMessage());
                Bundle b = new Bundle();
                b.putString(KEY_ERROR_MESSAGE, "Server sent invalid response.");
                sendMessage(MSG_REQUEST_SEND_FAILED, b);
                return;
            }
        }
        else if(p.getType() == PacketConstants.PACKET_TYPE_DATA) {
            //Handle data packets
            try {
                RequestObject r = RequestObject.deserializeRequestObject(p.getData());
                Bundle b = new Bundle();
                b.putParcelable(KEY_REQUEST_OBJECT, r);
                sendMessage(MSG_REQUEST_OBJECT, b);
            }
            catch(InvalidRequestDataException e) {
                Log.e(TAG, "Invalid server response: " + e.getMessage());
                Bundle b = new Bundle();
                b.putString(KEY_ERROR_MESSAGE, "Server sent invalid response.");
                sendMessage(MSG_REQUEST_SEND_FAILED, b);
                return;
            }
        }
    }
}
