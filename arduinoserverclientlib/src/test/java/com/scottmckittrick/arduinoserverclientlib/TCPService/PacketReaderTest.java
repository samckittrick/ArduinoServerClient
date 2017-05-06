package com.scottmckittrick.arduinoserverclientlib.TCPService;

import junit.framework.Assert;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;

/**
 * Created by Scott on 5/1/2017.
 */

public class PacketReaderTest {
    @Test(expected=IOException.class)
    public void testNullInputStream() throws IOException
    {
        PacketReader p = new PacketReader(null);
        p.read();
    }

    @Test
    public void testPacketReader() throws IOException
    {
        byte[] testPacket = { 0x00, 0x04, 0x01, 0x01, 0x02, 0x03 };
        byte[] testData = { 0x01, 0x02, 0x03 };
        ByteArrayInputStream b = new ByteArrayInputStream(testPacket);
        PacketReader p = new PacketReader(b);

        PacketReader.Packet read = p.read();
        Assert.assertEquals(PacketReader.PACKET_TYPE_AUTH, read.getType());
        Assert.assertTrue(Arrays.equals(testData, read.getData()));
    }
}
