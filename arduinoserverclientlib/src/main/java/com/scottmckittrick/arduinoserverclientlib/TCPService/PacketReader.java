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
    /** The size of a framing header **/
    private static final int FRAME_LEAD_SIZE = 2;

    /** The length of the packet as read from the header **/
    private int dataLen = 0;
    /** The type of the packet as read from the header */
    private byte packetType = 0;
    /** The data array */
    private byte[] data;
    /** the amount of data that has been read */
    private int dataRead = 0;

    /** Auth type identifier. Value is 0x01 */
    public static final byte PACKET_TYPE_AUTH = 0x01;
    /** Data type identifier. Value is 0x02 */
    public static final byte PACKET_TYPE_DATA = 0x02;

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
    public Packet read() throws IOException
    {
        if(istream == null)
            throw new IOException("InputStream is null");

        //Read the frame header first
        while(frameCount < FRAME_LEAD_SIZE)
            dataLen |= istream.read() << (FRAME_LEAD_SIZE - frameCount++ - 1); //As we read it in, place it in the data length variable

        //Next read the type
        packetType = (byte)istream.read();
        //Account for the fact that part of the length is the type
        dataLen--;

        //Allocate memory for the data
        data = new byte[dataLen];

        //Begin reading data;
        while(dataRead < dataLen) {
            dataRead += istream.read(data, dataRead, (dataLen - dataRead));
        }

        //Once the packet is read. Package it.
        Packet p = new Packet(packetType, data);
        return p;
    }


    /**
     * Immutable packet class containing the packet that was received.
     */
    public static class Packet {
        private byte packetType;
        private byte[] data;

        /**
         * Creates a new packet
         * @param type The type of packet.
         * @param data Byte array of data.
         */
        public Packet(byte type, byte[] data)
        {
            this.packetType = type;
            this.data = new byte[data.length];
            System.arraycopy(data, 0, this.data, 0, data.length);
        }

        /**
         * Get the type of packet
         * @return byte representing the type of packet.
         */
        public byte getType() { return packetType; };

        /**
         * Retrieve the data from the packet
         * @return Copy of the byte array of data.
         */
        public byte[] getData()
        {
            byte[] out = new byte[data.length];
            System.arraycopy(data, 0, out, 0, data.length);
            return out;
        }
    }

}
