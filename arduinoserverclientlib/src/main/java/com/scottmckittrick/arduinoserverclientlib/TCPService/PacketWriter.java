package com.scottmckittrick.arduinoserverclientlib.TCPService;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Wraps an output stream and formats input into packets
 * Created by Scott on 5/6/2017.
 */

public class PacketWriter {
     /** Output stream to write to */
    private OutputStream oStream;

    /**
     * Constructor to create the packet writer.
     * @param o OutputStream to write to.
     */
    public PacketWriter(OutputStream o) { oStream = o;}

    /**
     * Write a packet to the outputstream
     * @param p The packet to write.
     * @throws IOException Throws exception if write fails
     */
    public void writePacket(PacketConstants.Packet p) throws IOException
    {
        if(p == null)
            throw new IOException("Invalid input");

        byte[] data = p.getData();
        byte type = p.getType();
        //Allocate memory for the packet
        byte[] packet = new byte[PacketConstants.FRAME_LEAD_SIZE + 1 + p.getData().length];

        //Write the frame length
        int frameLength = data.length + 1;
        packet[0] = (byte)(frameLength >> 8);
        packet[1] = (byte) (frameLength & 0xFF);

        //Write the type
        packet[2] = type;

        //Copy in the data
        System.arraycopy(data, 0, packet, 3, data.length);

        //Write the data to outputstream
        if(oStream == null)
            throw new IOException("Outputstream is null");

        oStream.write(packet);
    }

    public void close() throws IOException
    {
        if(oStream != null);
            oStream.close();
    }
}
