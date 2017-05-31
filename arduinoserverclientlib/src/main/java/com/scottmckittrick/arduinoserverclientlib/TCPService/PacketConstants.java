package com.scottmckittrick.arduinoserverclientlib.TCPService;

/**
 * Definition of constants used when processing packets.
 * Created by Scott on 5/6/2017.
 */

class PacketConstants {

    /** Data type identifier. Value is 0x02 */
    protected static final byte PACKET_TYPE_DATA = 0x02;
    /** Auth type identifier. Value is 0x01 */
    protected static final byte PACKET_TYPE_AUTH = 0x01;
    /** The size of a framing header **/
    protected static final int FRAME_LEAD_SIZE = 2;

    /**
     * Immutable packet class containing the packet that was received.
     */
    public static class Packet{
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
