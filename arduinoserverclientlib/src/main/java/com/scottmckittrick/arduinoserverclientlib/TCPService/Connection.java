package com.scottmckittrick.arduinoserverclientlib.TCPService;

import android.os.AsyncTask;

import com.scottmckittrick.arduinoserverclientlib.RequestObject;

import java.io.IOException;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

/**
 * Provides and manages tcp connections to the server
 * Created by Scott on 5/6/2017.
 */

public class Connection implements PacketReceiver {
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
    private boolean isRunning;
    /** True if the socket is connected */
    private boolean isConnected;
    /** Object to send received packets to */
    private PacketReceiver receiver;
    /** Set the object to send parsed requests to */
    private RequestObject.RequestReceiver reqReceiver;
    /** The read task */
    private PacketReadTask readTask;


    /**
     * Constructor to create connection
     * @param ipAddr The ipv4 address of the server
     * @param port The port of the server
     */
    public Connection(String ipAddr, int port)
    {
        //default timeout is 1000
        this(ipAddr, port, 1000);
    }

    /**
     * Constructor to create a connection
     * @param ipAddr The ipv4 address of the server
     * @param port The port of the server
     * @param timeout The length of time in milliseconds that the socket should wait.
     */
    public Connection(String ipAddr, int port, int timeout)
    {
        this(ipAddr, port, timeout, null);
    }


    /**
     * Constructor to create a connection
     * @param ipAddr The ipv4 address of the server
     * @param port The port of the server
     * @param timeout The length of time in milliseconds that the socket should wait.
     * @param r The object recieving a packet
     */
    public Connection( String ipAddr, int port, int timeout, RequestObject.RequestReceiver r)
    {
        this.ipAddr = ipAddr;
        this.port = port;
        this.timeout = timeout;
        isRunning = false;
        isConnected = false;
        reqReceiver = r;
        receiver = this;
    }

    /**
     * Set the object that will receive packets from the connection
     * @param r Packet Receiver object
     */
    public void setPacketReceiver(RequestObject.RequestReceiver r)
    {
        reqReceiver = r;
    }

    /**
     * Connect and authenticate to the server
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
                isConnected = true;
            }
            catch(IOException e)
            {
                throw new ConnectException("Error getting packet streams");
            }
            finally{
                mSocket.close();
                mSocket = null;
            }

        }
        catch(UnknownHostException e) {
            throw new ConnectException("Unknown host: " + ipAddr);
        }
        catch(IOException e){
            throw new ConnectException("Error creating socket");
        }
    }

    /**
     * Connect to the server
     * @throws ConnectException Throws when there is an issue connecting
     */
    public void connect() throws ConnectException
    {
        //Connect the socket.
        connectSocket();

        //Start the read task
        isRunning = true;
        readTask = new PacketReadTask();
        readTask.execute(packetReader);
    }

    /**
     * Disconnect from the server
     * @throws ConnectException Thrown when there is an error disconnecting.
     */
    public void disconnect() throws ConnectException
    {
        try {
            isRunning = false;
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
     * @param packet Packet to be sent
     * @throws IOException Thrown when there is a problem sending the packet.
     */
    public void writePacket(PacketConstants.Packet packet) throws IOException
    {
        packetWriter.writePacket(packet);
    }

    @Override
    public void onPacketReceived(PacketConstants.Packet packet) {
        //ToDo do something with the packet
    }

    /**
     * Async Task for reading from the socket
     */
    private class PacketReadTask extends AsyncTask<PacketReader, PacketConstants.Packet, Boolean>
    {
        @Override
        protected Boolean doInBackground(PacketReader... p)
        {
            while(isRunning)
            {
                try {
                    PacketConstants.Packet result = p[0].read();
                    publishProgress(result);
                }
                catch(SocketTimeoutException e)
                {
                    //Read again if the socket times out.
                    continue;
                }
                catch(IOException e)
                {
                    return false;
                }
            }
            //If we get here, we exited cleanly
            return true;
        }

        @Override
        protected void onProgressUpdate(PacketConstants.Packet... packets){
            if(receiver != null)
                receiver.onPacketReceived(packets[0]);
        }
    }
}
