package com.scottmckittrick.arduinoserverclientlib;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.Arrays;

/**
 * Immutable object representing an request from one object to another.
 * Created by Scott on 4/23/2017.
 */

//ToDo make parcelable.
public class RequestObject implements Parcelable {
    /** The size of the serialized header */
    public static int HEADER_SIZE = 3;

    /** Id of the device the request refers to */
    private int deviceId;
    /** Command being sent */
    private short command;
    /** Any accompanying data */
    private byte[] data;

    /**
     * Constructor to create request object
     * @param deviceId The id of the device being referenced.
     * @param command The command that the request contains.
     * @param data any data that goes with the command.
     */
    public RequestObject(int deviceId, short command, byte[] data)
    {
        this.deviceId = deviceId;
        this.command = command;

        if(data == null) {
            this.data = new byte[0];
        }
        else {
            this.data = new byte[data.length];
            System.arraycopy(data, 0, this.data, 0, data.length);
        }
    }

    /**
     * Constructor to create request object with array indexes
     * @param deviceId The id of the device being referenced.
     * @param command The command that the request contains.
     * @param data any data that goes with the command.
     * @param offset The starting point in the array.
     * @param count The number of elements to copy.
     */
    public RequestObject(int deviceId, short command, byte[] data, int offset, int count) throws InvalidRequestDataException
    {
        this.deviceId = deviceId;
        this.command = command;

        //Make sure we won't run into an array out of bounds condition
        if((count > 0 && data == null) || (data.length - offset) < count)
            throw new InvalidRequestDataException("Data and count don't match up");

        if(count == 0) {
            this.data = new byte[0];
        }
        else {
            this.data = new byte[count];
            System.arraycopy(data, offset, this.data, 0, count);
        }
    }



    /**
     * Get the id of the device that the request is going to or coming from.
     * @return Integer representing the device id.
     */
    public int getDeviceId()
    {
        return deviceId;
    }

    /**
     * Get the command that is being requested.
     * @return Short representing the command to be requested.
     */
    public short getCommand()
    {
        return command;
    }

    /**
     * Get any data that is accompanying the command.
     * @return Array of bytes representing the data that was sent.
     */
    public byte[] getData()
    {
        byte[] arr = new byte[data.length];
        System.arraycopy(data, 0, arr, 0, data.length);
        return arr;
    }

    //Parcelable functions
    @Override
    public int describeContents() { return 0; }

    @Override
    public void writeToParcel(Parcel out, int flags)
    {
        out.writeInt(deviceId);
        out.writeInt(command);
        out.writeInt(data.length);
        out.writeByteArray(data);
    }

    public static final Parcelable.Creator<RequestObject> CREATOR = new Parcelable.Creator<RequestObject>() {
        @Override
        public RequestObject createFromParcel(Parcel p) {
            return new RequestObject(p);
        }

        @Override
        public RequestObject[] newArray(int size) {
            return new RequestObject[size];
        }
    };

    /**
     * Constructor for creating from a parcel
     * @param p Parcel to be referenced
     */
    private RequestObject(Parcel p)
    {
        deviceId = p.readInt();
        command = (short)p.readInt();
        int dataLen = p.readInt();
        data = new byte[dataLen];
        p.readByteArray(data);
    }

    @Override
    public boolean equals(Object other)
    {
        if(other == null)
            return false;

        if(!RequestObject.class.isAssignableFrom(other.getClass()))
            return false;

        final RequestObject otherReq = (RequestObject)other;

        boolean result = true;
        result &= (this.deviceId == otherReq.deviceId);
        result &= (this.command == otherReq.command);
        result &= Arrays.equals(this.data, otherReq.data);
        return result;
    }

    /**
     * Interface to be implemented by objects that are ready to receive request objects.
     */
    public interface RequestReceiver {
        /**
         * Handle incoming request objects
         * @param r Incoming RequestObject.
         */
        void handleRequest(RequestObject r);
    }

    /**
     * Function for deserializing request objects from a byte array.
     * @param input Byte array to deserialize
     * @return Request object that was generated
     * @throws InvalidRequestDataException If the input byte array is invalid
     */
    public static RequestObject deserializeRequestObject(byte[] input) throws InvalidRequestDataException
    {
        if(input == null)
            throw new InvalidRequestDataException("Null request data");

        if(input.length < HEADER_SIZE)
            throw new InvalidRequestDataException("Request data too short.");

        int deviceId = input[0];
        short command = (short)((input[1] << 8) | input[2]);
        return new RequestObject(deviceId, command, input, HEADER_SIZE, input.length - HEADER_SIZE);
    }

    /**
     * Serialize a request object
     * @param r Request Object to be serialized
     * @return Byte array containing serialized object.
     */
    public static byte[] serializeRequestObject(RequestObject r)
    {
        byte[] output = new byte[r.data.length + HEADER_SIZE];
        output[0] = (byte)(r.deviceId & 0xFF);
        output[1] = (byte)(r.command >> 8);
        output[2] = (byte) (r.command & 0xFF);
        System.arraycopy(r.data, 0, output, 3, r.data.length);
        return output;
    }
}
