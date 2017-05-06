package com.scottmckittrick.arduinoserverclientlib.AuthenticationScheme;

import android.os.Parcel;
import android.os.Parcelable;

import org.junit.Test;

import java.io.UnsupportedEncodingException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;


/**
 * Created by Scott on 4/23/2017.
 */

public class AuthenticatorTest {
    public static String schemeName = "FAKE_AUTH_SCHEME";
    public static String authRequestStr = "authRequest";

    @Test
    public void authenticationTest() throws AuthenticationException, InvalidAuthenticationMessageException, UnsupportedEncodingException
    {
        FakeAuthScheme fa = new FakeAuthScheme();
        Authenticator a = new Authenticator(fa);

        //Start by making sure we aren't authenticated
        assertFalse(a.getAuthenticated());

        //Start the first authentication request
        byte[] response = a.handleAuthPacket(null);
        assertEquals(Authenticator.AUTHTYPE_CLIENT_HELLO, response[0]);
        byte[] strBytes = new byte[response.length - 1];
        System.arraycopy(response, 1, strBytes, 0, response.length -1);
        String returnedSchemeName = new String(strBytes, "US-ASCII");
        assertEquals(schemeName, returnedSchemeName);
        assertFalse(a.getAuthenticated());

        //Second Auth Request
        response[0] = Authenticator.AUTHTYPE_SERVER_HELLO;
        response = a.handleAuthPacket(response);
        assertEquals(response[0], Authenticator.AUTHTYPE_AUTHREQ);
        strBytes = new byte[response.length - 1];
        System.arraycopy(response, 1, strBytes, 0, response.length - 1);
        assertEquals(authRequestStr + fa.state, new String(strBytes, "US-ASCII"));
        assertFalse(a.getAuthenticated());

        //More info required.
        response[0] = Authenticator.AUTHTYPE_RSPMOREINFO;
        response = a.handleAuthPacket(response);
        assertEquals(response[0], Authenticator.AUTHTYPE_AUTHREQ);
        strBytes = new byte[response.length - 1];
        System.arraycopy(response, 1, strBytes, 0, response.length - 1);
        assertEquals(authRequestStr + fa.state, new String(strBytes, "US-ASCII"));
        assertFalse(a.getAuthenticated());

        //Success
        response[0] = Authenticator.AUTHTYPE_RSPSUCCESS;
        response = a.handleAuthPacket(response);
        assertNull(response);
        assertTrue(a.getAuthenticated());

        //More info required.
        byte[] schemeNameStrBytes = schemeName.getBytes("US-ASCII");
        response = new byte[schemeNameStrBytes.length + 1];
        response[0] = Authenticator.AUTHTYPE_RSPMOREINFO;
        System.arraycopy(schemeNameStrBytes, 0, response, 1, schemeNameStrBytes.length);
        response = a.handleAuthPacket(response);
        assertEquals(response[0], Authenticator.AUTHTYPE_AUTHREQ);
        assertFalse(a.getAuthenticated());
        strBytes = new byte[response.length - 1];
        System.arraycopy(response, 1, strBytes, 0, response.length - 1);
        assertEquals(authRequestStr + fa.state, new String(strBytes, "US-ASCII"));

        //Success again
        response[0] = Authenticator.AUTHTYPE_RSPSUCCESS;
        response = a.handleAuthPacket(response);
        assertNull(response);
        assertTrue(a.getAuthenticated());
    }

    //ToDo Add exception testing to make sure the object is reset every time.

    public static class FakeAuthScheme extends AuthenticationScheme implements Parcelable
    {
        public int state = 0;

        public FakeAuthScheme()
        {
            state = 0;
        }

        @Override
        public String getSchemeName()
        {
            return schemeName;
        }

        @Override
        public byte[] authenticate(byte[] packet) throws AuthenticationException, InvalidAuthenticationMessageException
        {
            try {
                    state++;
                    byte[] rsp = (authRequestStr + state).getBytes("US-ASCII");
                    return rsp;
            }
            catch(UnsupportedEncodingException e)
            {
                throw new AuthenticationException("Unsupported encoding in test");
            }
        }

        @Override
        public int describeContents()
        {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel out, int flags)
        {
            out.writeInt(state);
        }

        public static final Parcelable.Creator<FakeAuthScheme> CREATOR = new Parcelable.Creator<FakeAuthScheme>() {
            public FakeAuthScheme createFromParcel(Parcel p) {
                return new FakeAuthScheme(p);
            }

            public FakeAuthScheme[] newArray(int size) {
                return new FakeAuthScheme[size];
            }
        };

        /**
         * Constructor for creating from a parcel
         * @param p Parcel to be referenced
         */
        private FakeAuthScheme(Parcel p){
            state = p.readInt();
        }

        public boolean equals(FakeAuthScheme other)
        {
            return this.state == other.state;

        }
    }
}
