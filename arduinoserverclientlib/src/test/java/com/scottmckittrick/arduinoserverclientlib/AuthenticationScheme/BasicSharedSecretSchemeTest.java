package com.scottmckittrick.arduinoserverclientlib.AuthenticationScheme;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import static org.mockito.Mockito.when;

/**
 * Unit Test for BasicSharedSecretScheme
 * Created by Scott on 4/21/2017.
 */
@RunWith(MockitoJUnitRunner.class)
public class BasicSharedSecretSchemeTest {

    @Test
    public void testInstantiate() throws AuthenticationException
    {
        BasicSharedSecretScheme scheme = new BasicSharedSecretScheme("password", 1234);
        Assert.assertEquals("SHARED_SECRET_SCHEME", scheme.getSchemeName());
    }

    @Test
    public void testEquals()
    {
        BasicSharedSecretScheme scheme1 = new BasicSharedSecretScheme("equal", 1234);
        BasicSharedSecretScheme scheme2 = new BasicSharedSecretScheme("equal", 1234);
        BasicSharedSecretScheme scheme3 = new BasicSharedSecretScheme("notequal", 4321);

        Assert.assertEquals(scheme1, scheme2);
        Assert.assertNotEquals(scheme1, scheme3);
    }

    @Mock
    BasicSharedSecretScheme.TimeHelper helper;

    @Test
    public void testAuthenticate() throws ParseException, AuthenticationException
    {
        SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        f.setTimeZone(TimeZone.getTimeZone("GMT"));
        String expectedTimestamp ="2017-05-01T23:57:35Z";
        Date date = f.parse(expectedTimestamp);
        when(helper.getCurrentTime()).thenReturn(date);
        String expectedHash = "894320d44d452a4a9b2e1246099359734fbe1e45cdb4391b1f8172d7cfb74c91";

        BasicSharedSecretScheme scheme = new BasicSharedSecretScheme("password", 4680, helper);
        byte[] response = scheme.authenticate(null);

        int id = (response[BasicSharedSecretScheme.SHA_256_LENGTH] << 24) ;
        id |= (response[BasicSharedSecretScheme.SHA_256_LENGTH + 1] << 16) ;
        id |= (response[BasicSharedSecretScheme.SHA_256_LENGTH + 2] << 8) ;
        id |= response[BasicSharedSecretScheme.SHA_256_LENGTH + 3] & 0xFF;
        Assert.assertEquals(4680, id);

        String timestamp = new String(response, BasicSharedSecretScheme.SHA_256_LENGTH + 4, response.length - BasicSharedSecretScheme.SHA_256_LENGTH - 4);
        Assert.assertEquals(expectedTimestamp, timestamp);


        byte[] recvHash = new byte[BasicSharedSecretScheme.SHA_256_LENGTH];
        System.arraycopy(response, 0, recvHash, 0, BasicSharedSecretScheme.SHA_256_LENGTH);
        Assert.assertEquals(expectedHash.toUpperCase(), bytesToHex(recvHash).toUpperCase());
    }

    private final static char[] hexArray = "0123456789ABCDEF".toCharArray();
    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for ( int j = 0; j < bytes.length; j++ ) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }
}
