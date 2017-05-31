package com.scottmckittrick.arduinoserverclientlib.TCPService;

import java.io.IOException;
import java.io.InputStream;

/**
 * Wrapper class that reads an InputStream and translates the bytestream into packets.
 * Created by Scott on 5/1/2017.
 */

public class PacketReader {
    /** Input stream to read */
    private InputStream istream;

    /** The number of bytes read into the framing header */
    private int frameCount = 0;

    /** The length of the packet as read from the header **/
    private int dataLen = 0;
    /** The type of the packet as read from the header */
    private byte packetType = 0;
    /** The data array */
    private byte[] data;
    /** the amount of data that has been read */
    private int dataRead = 0;

    /**
     * Constructor for the packet reader. Takes an input stream to read.
     * @param i InputStream to be read.
     */
    public PacketReader(InputStream i)
    {
        istream = i;
    }

    /**
     * Read a packet. This function will block until a full packet is read.
     *
     * @return Packet object representing the received packet.
     * @throws IOException If input stream is invalid.
     */
    public PacketConstants.Packet read() throws IOException
    {
        if(istream == null)
            throw new IOException("InputStream is null");

        //Read the frame header first
        while(frameCount < PacketConstants.FRAME_LEAD_SIZE) {
            int tmp = istream.read() << (PacketConstants.FRAME_LEAD_SIZE - frameCount++ - 1);//As we read it in, place it in the data length variable
            if (tmp < 0)
                throw new IOException("Lost Connection");
            dataLen |= tmp;
        }

        //Next read the type
        packetType = (byte)istream.read();
        if(packetType < 0)
            throw new IOException("Lost Connection");
        //Account for the fact that part of the length is the type
        dataLen--;

        //Allocate memory for the data
        data = new byte[dataLen];

        //Begin reading data;
        while(dataRead < dataLen) {
            int tmp = istream.read(data, dataRead, (dataLen - dataRead));
            if(tmp < 0)
                throw new IOException("Connection Lost");
            dataRead += tmp;
        }

        //Once the packet is read. Package it.
        PacketConstants.Packet p = new PacketConstants.Packet(packetType, data);
        return p;
    }

    /**
     * close the reader and stream.
     * @throws IOException
     */
    public void close() throws IOException
    {
        if(istream != null)
            istream.close();
    }

}
