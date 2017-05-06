package com.scottmckittrick.arduinoserverclientlib;

import org.junit.Assert;
import org.junit.Test;

/**
 * Created by Scott on 5/2/2017.
 */

public class RequestObjectTest {

    @Test
    public void testSerialize()
    {
        byte[] dataArray = { 0x01, 0x02, 0x03 };
        int devId = 1;
        short command = 2;
        byte[] expectedSerialized = { 0x01, 0x00, 0x02, 0x01, 0x02, 0x03 };

        RequestObject r = new RequestObject(devId, command, dataArray);

        byte[] result = RequestObject.serializeRequestObject(r);
        Assert.assertArrayEquals(expectedSerialized, result);
    }

    @Test(expected=InvalidRequestDataException.class)
    public void testDeserializeNull() throws InvalidRequestDataException
    {
        RequestObject.deserializeRequestObject(null);
    }

    @Test(expected=InvalidRequestDataException.class)
    public void testDeserializeShort() throws InvalidRequestDataException
    {
        byte[] req = { 0x01, 0x01 };
        RequestObject.deserializeRequestObject(req);
    }

    @Test
    public void testDeserializeObject() throws InvalidRequestDataException
    {
        byte[] dataArray = { 0x01, 0x02, 0x03 };
        int devId = 1;
        short command = 2;
        byte[] expectedSerialized = { 0x01, 0x00, 0x02, 0x01, 0x02, 0x03 };

        RequestObject result = RequestObject.deserializeRequestObject(expectedSerialized);
        Assert.assertEquals(devId, result.getDeviceId());
        Assert.assertEquals(command, result.getCommand());
        Assert.assertArrayEquals(dataArray, result.getData());
    }

}
