package com.scottmckittrick.arduinoserverclientlib.TCPClient;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

import com.scottmckittrick.arduinoserverclientlib.TCPService.ServerService;

/**
 * Class for binding an application to the server service.
 * This class implements all functions to bind to and connect to the service.
 * The handleResponse() function should be overriden so each specific application can handle responses.
 * Created by Scott on 5/11/2017.
 */

public abstract class ServiceClient {

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

    /**
     * Constructor for creating the service client
     * @param ctx The against which services may be called.
     */
    public ServiceClient(Context ctx)
    {
        this.ctx = ctx;
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
                serviceMessenger = new Messenger(iBinder);
            }

            @Override
            public void onServiceDisconnected(ComponentName componentName) {
                serviceMessenger = null;
            }
        };

        if(ctx == null) {
            Log.e(TAG, "Context cannot be null");
            return false;
        }

        if(ctx.bindService(new Intent(ctx, ServerService.class), sConn, Context.BIND_AUTO_CREATE))
        {
            Log.i(TAG,"Successfully bound to service.");
        }
        else
        {
            Log.e(TAG, "Error binding to service");
            ctx.unbindService(sConn);
            return false;
        }

        //Register this object's messenger
        myMessenger = new Messenger(new ServiceClientHandler());
        Message msg = Message.obtain(null, ServerService.MSG_REGISTER_CLIENT);
        msg.replyTo = myMessenger;
        try {
            serviceMessenger.send(msg);
        }catch(RemoteException r) {
            Log.e(TAG, "Error registering client handler");
            return false;
        }

        return true;
    }
    /*
    Function listing:
    implemented:
        bind - done
        unbind
        connect
        disconnect
        setAuthenticationScheme
        sendRequest
    abstract:
        handleResponse - done

     */


    /**
     * Function for handling incoming messages from the service.
     * @param m The message that was received.
     */
    public abstract void handleResponse(Message m);

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
