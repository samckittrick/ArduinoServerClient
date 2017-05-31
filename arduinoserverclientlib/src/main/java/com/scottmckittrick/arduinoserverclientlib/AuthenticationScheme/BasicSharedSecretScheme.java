package com.scottmckittrick.arduinoserverclientlib.AuthenticationScheme;

import android.os.Parcel;
import android.os.Parcelable;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

/**
 * Basic Shared Secret Scheme. Initialize it with a password and the client id and it will startAuthenticate to the server.
 * Created by Scott on 4/21/2017.
 */

public class BasicSharedSecretScheme extends AuthenticationScheme implements Parcelable {
    private String secret;
    private int id;
    private TimeHelper helper;
    private boolean isAuthenticated;
    public static final String schemeName = "SHARED_SECRET_SCHEME";
    public static final int SHA_256_LENGTH = 32; //256 bits = 32 bytes
    public static final int TIME_LENGTH = 20;
    /**
     * Constructor for BasicSharedSecretScheme
     * @param secret Secret password known to client and server
     * @param id Client ID to be passed to server.
     */
    public BasicSharedSecretScheme(String secret,  int id) {
        this(secret,id, new TimeHelper());
    }

    protected BasicSharedSecretScheme(String secret, int id, TimeHelper helper)
    {
        this.secret = secret;
        this.id = id;
        isAuthenticated = false;
        this.helper = helper;
    }

    @Override
    public String getSchemeName() {
        return schemeName;
    }

    @Override
    protected byte[] authenticate(byte[] message) throws AuthenticationException
    {
        byte[] array = new byte[56];
        SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        f.setTimeZone(TimeZone.getTimeZone("GMT"));
        String dateString = f.format(helper.getCurrentTime());
        String s = secret + id + dateString;
        //System.out.println(s);
        //Log.d("SharedSecretScheme", "Prehash: " + s);
        MessageDigest dgst = null;
        try
        {
            //Create the digest
            dgst = MessageDigest.getInstance("SHA-256");
            dgst.reset();
            byte[] digest = dgst.digest(s.getBytes("US-ASCII"));
            int index = 0;
            System.arraycopy(digest, 0, array, index, SHA_256_LENGTH);
            index += SHA_256_LENGTH;

            //Copy the id
            array[index++] = (byte) ((id >> 24)& 0xFF);
            array[index++] = (byte) ((id >> 16) & 0xFF);
            array[index++] = (byte) ((id >> 8) & 0xFF);
            array[index++] = (byte) (id & 0xFF);

            //Copy in the timestamp
            System.arraycopy(dateString.getBytes("US-ASCII"), 0, array, index, TIME_LENGTH);
            return array;
        }
        catch(NoSuchAlgorithmException e)
        {
            throw new AuthenticationException(e.getMessage());
        }
        catch(UnsupportedEncodingException e)
        {
            throw new AuthenticationException(e.getMessage());
        }
    }

    //Parcelable functions
    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags)
    {
        out.writeString(secret);
        if(isAuthenticated)
            out.writeInt(1);
        else
            out.writeInt(0);
        out.writeInt(id);
    }

    public static final Parcelable.Creator<BasicSharedSecretScheme> CREATOR = new Parcelable.Creator<BasicSharedSecretScheme>() {
        @Override
        public BasicSharedSecretScheme createFromParcel(Parcel p) {
            return new BasicSharedSecretScheme(p);
        }

        @Override
        public BasicSharedSecretScheme[] newArray(int size) {
            return new BasicSharedSecretScheme[size];
        }
    };

    /**
     * Constructor for creating from a parcel
     * @param p Parcel to be referenced
     */
    private BasicSharedSecretScheme(Parcel p)
    {
        secret = p.readString();
        isAuthenticated = p.readInt() == 1;
        id = p.readInt();
        helper = new TimeHelper();
    }

    @Override
    public boolean equals(Object other)
    {
        if(other == null)
            return false;

        if(!BasicSharedSecretScheme.class.isAssignableFrom(other.getClass()))
            return false;

        final BasicSharedSecretScheme otherScheme = (BasicSharedSecretScheme)other;

        boolean result = true;
        result &= this.isAuthenticated == otherScheme.isAuthenticated;
        result &= this.secret.equals(otherScheme.secret);
        result &= this.id == otherScheme.id;
        return result;

    }

    /**
     * This class is created to allow the time to be mocked in unit testing.
     */
    protected static class TimeHelper {
        /**
         * Returns the current time as a date object.
         * @return Date object representing the current time.
         */
        public Date getCurrentTime()
            {
                return new Date();
            }
    }
}
