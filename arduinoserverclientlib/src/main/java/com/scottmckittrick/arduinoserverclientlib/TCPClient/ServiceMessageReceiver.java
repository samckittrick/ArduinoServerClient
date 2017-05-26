package com.scottmckittrick.arduinoserverclientlib.TCPClient;

import android.os.Message;

/**
 * Receives messages sent from the ServerService
 * Created by Scott on 5/23/2017.
 */

public interface ServiceMessageReceiver {
    /**
     * Called when a message is received from the Service
     * @param m Message being received.
     */
    public void onMessageReceived(Message m);
}
