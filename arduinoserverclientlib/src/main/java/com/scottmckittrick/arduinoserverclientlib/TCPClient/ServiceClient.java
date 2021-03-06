package com.scottmckittrick.arduinoserverclientlib.TCPClient;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

import com.scottmckittrick.arduinoserverclientlib.AuthenticationScheme.AuthenticationScheme;
import com.scottmckittrick.arduinoserverclientlib.RequestObject;
import com.scottmckittrick.arduinoserverclientlib.TCPService.ServerService;

/**
 * Class for binding an application to the server service.
 * This class implements all functions to bind to and connect to the service.
 * The handleResponse() function should be overriden so each specific application can handle responses.
 * Created by Scott on 5/11/2017.
 */

public class ServiceClient implements RequestObject.RequestReceiver {

    /** Context used for calling the service */
    private Context ctx;
    /** Object for handling service connection events */
    private ServiceConnection sConn;
    /** Messenger object used to communicate with the service. */
    private Messenger serviceMessenger;
    /** Logging Tag */
    private static final String TAG = "ArduinoServerClient";
    /** Local message handler for receiving messages from the service */
    private Messenger myMessenger;
    /** Indicates whether or not the service is bound. */
    private boolean isBound;

    /** Receiver for messages related to the state of the server connection **/
    private ServiceMessageReceiver messageReceiver;
    /**Receiver for request objects sent from the server via the ServerService **/
    private RequestObject.RequestReceiver requestReceiver;

    /**
     * Constructor for creating the service client
     * @param ctx The against which services may be called.
     */
    public ServiceClient(Context ctx)
    {
        this.ctx = ctx;
        isBound = false;
    }

    /**
     * Bind to the service.
     * @return True of the bind worked. False if the bind failed.
     */
    public boolean  bind()
    {
        //Create the service connection listener.
        sConn = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
                Log.d(TAG, "Service has been connected.");
                serviceMessenger = new Messenger(iBinder);
                registerMessenger();
                isBound = true;
                if(messageReceiver != null)
                    messageReceiver.onMessageReceived(Message.obtain(null, ServerService.MSG_BOUND));
            }

            @Override
            public void onServiceDisconnected(ComponentName componentName) {
                serviceMessenger = null;
                if(messageReceiver != null)
                    messageReceiver.onMessageReceived(Message.obtain(null, ServerService.MSG_UNBOUND));
            }
        };

        if(ctx == null) {
            Log.e(TAG, "Context cannot be null");
            return false;
        }

        //Actually bind the service
        if(ctx.bindService(new Intent(ctx, ServerService.class), sConn, Context.BIND_AUTO_CREATE))
        {
            Log.i(TAG,"Successfully bound to service.");
        }
        else
        {
            Log.e(TAG, "Error binding to service");
            ctx.unbindService(sConn);
            isBound = false;
            return false;
        }

        return true;
    }

    private void registerMessenger()
    {
        //Register this object's messenger
        myMessenger = new Messenger(new ServiceClientHandler());
        Message msg = Message.obtain(null, ServerService.MSG_REGISTER_CLIENT);
        msg.replyTo = myMessenger;
        try {
            serviceMessenger.send(msg);
        }catch(RemoteException r) {
            Log.e(TAG, "Error registering client handler");
        }
    }

    /**
     * Unbind from the service.
     * @return True if successful.
     */
    public boolean unbind()
    {
        Message msg = Message.obtain(null, ServerService.MSG_UNREGISTER_CLIENT);
        msg.replyTo = myMessenger;
        try
        {
            serviceMessenger.send(msg);
        }
        catch(RemoteException e) {
            Log.e(TAG, "Error unregistering client handler");
        }

        ctx.unbindService(sConn);
        isBound = false;
        return true;
    }

    /**
     * Tell the service to connect to a server
     * @param ip The ip of the server
     * @param port the port of the server
     * @return True if the message is sent successfully, false otherwise.
     * @throws ServiceNotBoundException Thrown if the service hasn't been bound to first.
     */
    public boolean sendConnect(String ip, int port) throws ServiceNotBoundException {
        if(!isBound)
            throw new ServiceNotBoundException("Service must be bound first");

        Message msg = Message.obtain(null, ServerService.MSG_CONNECT_SERVER);
        Bundle data = new Bundle();
        data.putString(ServerService.KEY_SERVER_IP, ip);
        data.putInt(ServerService.KEY_SERVER_PORT, port);
        msg.setData(data);
        try {
            serviceMessenger.send(msg);
            return true;
        }catch(RemoteException e) {
            Log.e(TAG, "Error sending message");
            return false;
        }
    }

    /**
     * Send a disconnect command to the server.
     * @throws ServiceNotBoundException Thrown when the client isn't yet bound to the service.
     */
    public boolean sendDisconnect() throws ServiceNotBoundException {
        if(!isBound)
            throw new ServiceNotBoundException("Service must be bound first");

        Message msg = Message.obtain(null, ServerService.MSG_DISCONNECT_SERVER);
        try{
            serviceMessenger.send(msg);
            return true;
        } catch(RemoteException e) {
            Log.e(TAG, "Error Sending message");
            return false;
        }
    }

    /**
     * Set an authentication scheme in the service. This allows the client to choose and set up the authentication scheme before sending it to the service.
     * @param authScheme The AuthenticationScheme to send to the service
     * @return True if the message was sent successfully. False otherwise.
     * @throws ServiceNotBoundException Throws a ServiceNotBoundException when the service is not yet bound.
     */
    public boolean setAuthentciationScheme(AuthenticationScheme authScheme) throws ServiceNotBoundException {
        if(!isBound)
            throw new ServiceNotBoundException("Service must be bound first");

        Message msg = Message.obtain(null, ServerService.MSG_AUTHSCHEME_SELECT);
        Bundle b = new Bundle();
        b.putParcelable(ServerService.KEY_AUTHSCHEME, authScheme);
        msg.setData(b);

        try
        {
            serviceMessenger.send(msg);
            return true;
        } catch(RemoteException e) {
            Log.e(TAG, "Error sending message");
            return false;
        }
    }

    /**
     * Send a request object to the service to be sent to the server.
     * @param r RequestObject to be sent
     * @return True if the message was successful, false otherwise.
     * @throws ServiceNotBoundException Thrown if the service is not already bound.
     */
    @Override
    public void handleRequest(RequestObject r)
    {
        if(!isBound)
            Log.e(TAG, "Service not yet bound");

        Message msg = Message.obtain(null, ServerService.MSG_REQUEST_OBJECT);
        Bundle b = new Bundle();
        b.putParcelable(ServerService.KEY_REQUEST_OBJECT, r);
        msg.setData(b);

        try
        {
            serviceMessenger.send(msg);
            return;
        } catch(RemoteException e) {
            Log.e(TAG, "Error sending message");
            return;
        }
    }

    /**
     * Function for handling incoming messages from the service.
     * If they are request objects, we split them to a request receiver, otherwise pass them on.
     * @param m The message that was received.
     */
    public void handleResponse(Message m) {
        if(m.what == ServerService.MSG_REQUEST_OBJECT) {
            Bundle data = m.getData();
            data.setClassLoader(RequestObject.class.getClassLoader());
            RequestObject r = data.getParcelable(ServerService.KEY_REQUEST_OBJECT);
            if(r == null) {
                Log.e(TAG, "Invalid response from server. Missing request object");
                return;
            }

            if(requestReceiver != null)
                requestReceiver.handleRequest(r);
        }
        else
        {
            if(messageReceiver != null)
                messageReceiver.onMessageReceived(m);
        }
    }

    /**
     * Sets an object to receive request objects from the server
     * @param r Request Receiver to get requests
     */
    public void setRequestReceiver(RequestObject.RequestReceiver r)
    {
        requestReceiver = r;
    }

    /**
     * Sets an object to receive other service messages.
     * @param s Object to receive messages.
     */
    public void setMessageReceiver(ServiceMessageReceiver s)
    {
        messageReceiver = s;
    }

    /**
     * Handler class to capture messages received from the server
     */
    public class ServiceClientHandler extends Handler {
        @Override
        public void handleMessage(Message m) {
            handleResponse(m); //Let the overriding class handle incomming messages.
        }
    }


}
