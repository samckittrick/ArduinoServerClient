package com.scottmckittrick.arduinoserverclientlib.TCPService;

import android.os.Handler;
import android.util.Log;

import java.io.IOException;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Provides and manages tcp connections to the server.
 * Created by Scott on 5/6/2017.
 */

public class Connection implements Runnable{
    /** The Ipv4 Address of the server */
    private String ipAddr;
    /** The port of the server */
    private int port;
    /*The socket read timeout in milliseconds */
    private int timeout;
    /** Used to write a packet to the socket */
    private PacketWriter packetWriter;
    /** Used to read a packet from the socket */
    private PacketReader packetReader;
    /** Socket object */
    private Socket mSocket;
    /** True if the read thread is running */
    private AtomicBoolean isRunning;
    /** True if the socket is connected */
    private AtomicBoolean isConnected;
    /** Handler from the thread object to send received packets to */
    private Handler callerHandler;
    /** Object receiving packets */
    private ConnectionMonitor connectionMonitor;
    /** Connection log tag */
    private static final String TAG = "Connection";

    /** Enum representing connection states */
    public enum ConnectionState { STATE_DISCONNECTED, STATE_CONNECTED, STATE_CONNECTION_FAILED, STATE_CONNECTION_LOST }

    /**
     * Constructor to create connection
     * @param ipAddr The ipv4 address of the server
     * @param port The port of the server
     * @param receiver A handler to receive packets from the socket.
     */
    public Connection(String ipAddr, int port, Handler receiver, ConnectionMonitor p)
    {
        //default timeout is 1000
        this(ipAddr, port, 1000, receiver, p);
    }

    /**
     * Constructor to create a connection
     * @param ipAddr The ipv4 address of the server
     * @param port The port of the server
     * @param timeout The length of time in milliseconds that the socket should wait.
     * @param receiver A handler to receive packets from the .
     */
    public Connection( String ipAddr, int port, int timeout, Handler receiver, ConnectionMonitor p)
    {
        this.ipAddr = ipAddr;
        this.port = port;
        this.timeout = timeout;
        callerHandler = receiver;
        connectionMonitor = p;
        isRunning = new AtomicBoolean(false);
        isConnected = new AtomicBoolean(false);
    }

    /**
     * Connect and startAuthenticate to the server
     * @throws ConnectException When the socket connection fails
     */
    private void connectSocket() throws ConnectException
    {
        //Create the socket and prep it for use
        try {
            InetAddress serverAddr = InetAddress.getByName(ipAddr);
            mSocket = new Socket(serverAddr, port);
            mSocket.setSoTimeout(timeout);


            try {
                //Get the Input and OutputStreams
                packetWriter = new PacketWriter(mSocket.getOutputStream());
                packetReader = new PacketReader(mSocket.getInputStream());
                isConnected.set(true);
            }
            catch(IOException e)
            {
                mSocket.close();
                mSocket = null;
                throw new ConnectException("Error getting packet streams");
            }
        }
        catch(UnknownHostException e) {
            throw new ConnectException("Unknown host: " + ipAddr);
        }
        catch(IOException e){
            throw new ConnectException("Error creating socket");
        }
    }

    @Override
    public void run()
    {
        try {
            Log.d(TAG, "Starting read thread. Connecting Socket..");
            connectSocket();
            connectionMonitor.onConnectionStateChanged(ConnectionState.STATE_CONNECTED);
        }catch (ConnectException e) {
            Log.e(TAG, e.getMessage());
            connectionMonitor.onConnectionStateChanged(ConnectionState.STATE_CONNECTION_FAILED);
            return;
        }

        Log.d(TAG, "Socket Connected. While loop starting");
        while(!Thread.currentThread().isInterrupted())
        {
            try {
                final PacketConstants.Packet p = packetReader.read();
                callerHandler.post(new Runnable() {
                    @Override
                    public void run(){
                        connectionMonitor.onPacketReceived(p);
                    }
                });
            }catch(SocketTimeoutException e){
                continue;
            } catch(IOException e) {
                Log.e(TAG, e.getMessage());
                connectionMonitor.onConnectionStateChanged(ConnectionState.STATE_CONNECTION_LOST);
            }
        }

        //If we make ithere, we should disconnect the socket.
        try {
            disconnect();
        }catch(ConnectException e) {
            Log.e(TAG, e.getMessage());
        } finally {
            connectionMonitor.onConnectionStateChanged(ConnectionState.STATE_DISCONNECTED);
        }
    }

    /**
     * Disconnect from the server
     * @throws ConnectException Thrown when there is an error disconnecting.
     */
    public void disconnect() throws ConnectException
    {
        try {
            isConnected.set(false);
            packetReader.close();
            packetWriter.close();
            mSocket.close();
        }
        catch(IOException e)
        {
            throw new ConnectException("Error disconnecting from server: " + e.getMessage());
        }
    }

    /**
     * Send a packet to the server
     * @param p packet being sent
     * @throws IOException Thrown when there is a problem sending the packet.
     */
    public synchronized void writePacket(PacketConstants.Packet p ) throws IOException
    {
        if(!isConnected.get())
            throw new IOException("Socket is not connected.");

        packetWriter.writePacket(p);
    }

    /**
     * Get the current connection state
     */
    public boolean getIsConnected()
    {
        return isConnected.get();
    }

    /**
     * A connection monitor
     */
    public interface ConnectionMonitor {
        void onPacketReceived(PacketConstants.Packet p);
        void onConnectionStateChanged(ConnectionState c);
    }


}
