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
        Assert.assertEquals(devId, result.getDeviceAddress());
        Assert.assertEquals(command, result.getCommand());
        Assert.assertArrayEquals(dataArray, result.getData());
    }

    @Test
    public void testEquals()
    {
        byte[] arr1 = { 0x01, 0x02, 0x03 };
        byte[] arr2 = {0x03, 0x02, 0x01, 0x00 };
        RequestObject req1 = new RequestObject(1, (short)2, arr1);
        RequestObject req2 = new RequestObject(1, (short)2, arr1);
        RequestObject req3 = new RequestObject(2, (short)3, arr2);

        Assert.assertEquals(req1, req2);
        Assert.assertNotEquals(req1, req3);
    }

}
