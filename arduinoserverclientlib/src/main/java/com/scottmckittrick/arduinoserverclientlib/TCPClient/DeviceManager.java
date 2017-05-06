package com.scottmckittrick.arduinoserverclientlib.TCPClient;

import com.scottmckittrick.arduinoserverclientlib.RequestObject;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

/**
 * Instantiates and manages the list of devices available for a given server.
 * Created by Scott on 4/23/2017.
 */

public class DeviceManager {
    /**
     * Device type representing an RGB lamp.
     */
    public static final int DEVICE_TYPE_RGB_LAMP = 0x01;

    private ArrayList<BasicDevice> devList;
    private RequestObject.RequestReceiver receiver;


    /**
     *  Get the device by its id. Throw an exception if the device doesn't exist.
     * @param id Id of the device being searched for.
     * @return BasicDevice instance of requested device.
     * @throws DeviceNotFoundException Throws DeviceNotFoundException when the requested id isn't a currently available device.
     */
    public BasicDevice getDeviceById(int id) throws DeviceNotFoundException
    {
        for(int i = 0; i < devList.size(); i++)
        {
            if(devList.get(i).getDeviceId() == id)
                return devList.get(i);
        }

        //If the device isn't found throw an exception
        throw new DeviceNotFoundException("Device ID " + id + " not found.");
    }

//ToDo Javadoc
    public void updateDeviceList(byte[] packet) throws InvalidDeviceListException
    {
        //The device list data should have at least one byte saying 0 devices.
        if((packet == null)||(packet.length < 1))
            throw new InvalidDeviceListException("Null Device List Received.");

        //First byte is the number of device entries.
        int index = 0;
        int numDevices = packet[index++];

        if(numDevices == 0)
            return;

        try {
            //Read each device entry
            for (int i = 0; i < numDevices; i++) {
                int entryLength = packet[index++];
                int pointer = 0;

                //Begin parsing the entry
                int devType = packet[index + pointer];
                int devId = packet[index + pointer];
                byte[] devNameBytes = new byte[entryLength - 2];
                String devName = new String(devNameBytes, "US-ASCII");

                //If the device already exists, skip it.
                for(int j= 0; j < devList.size(); j++)
                    if(devList.get(j).getDeviceId() == devId)
                        continue;

                //Otherwise create a new one.
                try {
                    devList.add(DeviceManager.instantiateDevice(devType, devId, devName));
                } catch(UnknownDeviceTypeException e) {
                    //ToDo Handle unknown device
                }

                index += entryLength;

            }
        }catch(ArrayIndexOutOfBoundsException e) {
            throw new InvalidDeviceListException("Packet length is too short for the indicated number of devices.");
        }catch(UnsupportedEncodingException e) {
            throw new InvalidDeviceListException("Invalid encoding from server.");
        }
    }

    /**
     * Instantiates a class of the requested type and returns it to the caller.
     * @param type The type of device being requested.
     * @param id The id to assign to device.
     * @param deviceName The name to assign to the device.
     * @return An object of BasicDevice that can be cast to the requested type.
     * @throws UnknownDeviceTypeException Throws Unknown device type when the type of device requested is unknown.
     */
    private static BasicDevice instantiateDevice(int type, int id, String deviceName) throws UnknownDeviceTypeException
    {
        switch(type)
        {
            case DEVICE_TYPE_RGB_LAMP:
                return new RGBLampDevice(id, deviceName);
            default:
                throw new UnknownDeviceTypeException("Unknown Device Type: " + type);
        }
    }


}
