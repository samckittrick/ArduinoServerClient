package com.scottmckittrick.arduinoserverclientlib.TCPService;

import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Created by Scott on 5/6/2017.
 */

public class PacketWriterTest {
    @Test(expected= IOException.class)
    public void testNullOutputStream() throws IOException
    {
        byte[] falseData = {0x01 };
        PacketWriter o = new PacketWriter(null);
        o.writePacket(new PacketConstants.Packet(PacketConstants.PACKET_TYPE_AUTH, falseData));
    }

    @Test
    public void testPacketWriter() throws IOException
    {
        byte[] testPacket = { 0x00, 0x05, 0x01, 0x01, 0x02, 0x03, 0x04 };
        byte[] testData = { 0x01, 0x02, 0x03, 0x04 };
        PacketConstants.Packet p = new PacketConstants.Packet(PacketConstants.PACKET_TYPE_AUTH, testData);

        ByteArrayOutputStream oStream = new ByteArrayOutputStream();
        PacketWriter writer = new PacketWriter(oStream);
        writer.writePacket(p);
        byte[] resultPacket = oStream.toByteArray();

        Assert.assertArrayEquals(testPacket, resultPacket);
    }
}
