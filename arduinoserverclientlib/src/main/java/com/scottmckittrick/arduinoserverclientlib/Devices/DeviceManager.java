package com.scottmckittrick.arduinoserverclientlib.Devices;

import android.util.Log;

import com.scottmckittrick.arduinoserverclientlib.RequestObject;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

/**
 * Instantiates and manages the list of devices available for a given server.
 * Created by Scott on 4/23/2017.
 */

public class DeviceManager implements RequestObject.RequestReceiver {
    /**
     * Device type representing an RGB lamp.
     */
    public static final int DEVICE_TYPE_RGB_LAMP = 0x01;

    /**
     * Command Code representing Device get info.
     */
    public static final short DEV_GET_INFO = 0x0001;
    /**
     * Command Code representing device get info response
     */
    public static final short DEV_GET_INFO_RSP = 0x0100;

    public static final String TAG = "DeviceManager";

    /** Identifier used for passing the device address between activities **/
    public static final String devAddrTag = "DeviceAddress";
    /** Identifier used for passing the device name between activities **/
    public static final String devNameTag = "DeviceName";
    /** Identifier used for passing the device id between activities **/
    public static final String devIdTag = "DeviceID";

    /**
     * The list of devices sent by the server
     */
    private ArrayList<BasicDevice> devList;

    /**
     * The object that requests should be sent to.
     */
    private RequestObject.RequestReceiver requestReceiver;

    /**
     * List of objects listening for changes to the devices.
     */
    private ArrayList<DeviceChangeListener> deviceChangeListeners;

    /**
     * Basic Constructor.
     */
    public DeviceManager() {
        Log.d("Device Manager", "Device Manager being created");
        devList = new ArrayList();
        deviceChangeListeners = new ArrayList();
    }

    /**
     * Set the object that this object should send requests to.
     * @param r The RequestReceiver Object.
     */
    public void setRequestReceiver(RequestObject.RequestReceiver r) {
        requestReceiver = r;

        //Update any already instantiated devices.
        for(int i = 0; i < devList.size(); i++)
            devList.get(i).setRequestReceiver(r);
    }

    @Override
    public void handleRequest(RequestObject r) {
        //If the request is coming from the server device manager, handle it here
        if(r.getDeviceAddress() == 0) {
            if(r.getCommand() == DEV_GET_INFO_RSP){
                try {
                    updateDeviceList(r.getData());
                }catch(InvalidDeviceListException e) {
                    Log.e(TAG, "Server sent invalid device List");
                }
            }
            return;
        } else { //Otherwise it should be handled by one of the devices.
            try {
                BasicDevice dev = getDeviceByAddress(r.getDeviceAddress());
                dev.handleRequest(r);
            } catch (DeviceNotFoundException e) {
                Log.i(TAG, "Device Id " + r.getDeviceAddress() + " not found.");
            }
        }
    }

    /**
     * Returns the list of Devices currently known to the client
     * @return ArrayList of devices.
     */
    public ArrayList<BasicDevice> getDeviceList()
    {
        return devList;
    }

    /**
     * Refresh the Device List
     */
    public void refreshDeviceList()
    {
        RequestObject r = new RequestObject(0, DEV_GET_INFO, null);
        if(requestReceiver != null)
            requestReceiver.handleRequest(r);
    }

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

    /**
     * Get the device by its id. Thrpw an exception if the device doesn't exist.
     * @param address Address of the device being searched for.
     * @return BasicDevice instance of requested Device.
     * @throws DeviceNotFoundException Throws DeviceNotFoundException when the requested address isn't a currently available device.
     */
    public BasicDevice getDeviceByAddress(int address) throws DeviceNotFoundException
    {
        for(int i = 0; i < devList.size(); i++)
        {
            if(devList.get(i).getDeviceAddr() == address)
                return devList.get(i);
        }

        //If the device isn't found throw an exception
        throw new DeviceNotFoundException("Device Address " + address + " not found.");
    }

    /**
     * Unmarshall the device list sent from the server and instantiate new devices into the device list.
     * @param packet The byte string representing the device list
     * @throws InvalidDeviceListException Thrown if the device list is an invalid format.
     */
    private void updateDeviceList(byte[] packet) throws InvalidDeviceListException
    {
        ArrayList<BasicDevice> changedList = new ArrayList<>();
        //The device list data should have at least one byte saying 0 devices.
        if((packet == null)||(packet.length < 1))
            throw new InvalidDeviceListException("Null Device List Received.");

        //First byte is the number of device entries.
        int index = 0;
        int numDevices = packet[index++];
        Log.d(TAG, "Number of Devices: " + numDevices);

        if(numDevices == 0)
            return;

        try {
            //Read each device entry
            for (int i = 0; i < numDevices; i++) {
                int entryLength = packet[index++];
                Log.d(TAG, "Entry Length: " + entryLength);
                int pointer = 0;

                //Begin parsing the entry
                int devAddr = packet[index + pointer++];
                Log.d(TAG, "Device address: " + devAddr);
                int devType = packet[index + pointer++];
                Log.d(TAG, "Device Type: " + devType);
                int devId = packet[index + pointer++];
                Log.d(TAG, "Device Id: " + devId);
                byte[] devNameBytes = new byte[entryLength - 3];
                for(int j = 0; j < devNameBytes.length; j++)
                    devNameBytes[j] = packet[index + pointer++];
                String devName = new String(devNameBytes, "US-ASCII");
                Log.d(TAG, "Device Name: " + devName);

                //If the device already exists, skip it.
                boolean deviceFound = false;
                for(int j= 0; j < devList.size(); j++) {
                    if (devList.get(j).getDeviceAddr() == devAddr) {
                        deviceFound = true;
                        break;
                    }
                }

                //If the device is already in the list, then skip it.
                if(deviceFound)
                    continue;

                //Otherwise create a new one.
                try {
                    Log.d(TAG, "Instantiating "+ devName);
                    BasicDevice d = DeviceManager.instantiateDevice(devType, devId, devName, devAddr);
                    d.setRequestReceiver(requestReceiver);
                    devList.add(d);
                    changedList.add(d);
                } catch(UnknownDeviceTypeException e) {
                    //ToDo Handle unknown device
                }

                index += entryLength;

            }

            if(changedList.size() > 0)
                notifyDeviceChange(changedList);

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
    private static BasicDevice instantiateDevice(int type, int id, String deviceName, int deviceAddr) throws UnknownDeviceTypeException
    {
        switch(type)
        {
            case DEVICE_TYPE_RGB_LAMP:
                return new RGBLampDevice(id, deviceName, deviceAddr);
            default:
                throw new UnknownDeviceTypeException("Unknown Device Type: " + type);
        }
    }

    /**
     * Register to receive notifications if one or more devices change
     * @param d The object listening for notifications
     */
    public void registerDeviceChangeListener(DeviceChangeListener d)
    {
        if(!deviceChangeListeners.contains(d))
            deviceChangeListeners.add(d);
    }

    /**
     * Unregister from receiving notifications if one or more devices change
     * @param d The object to unregister.
     */
    public void unRegisterDeviceChangeListener(DeviceChangeListener d)
    {
        if(deviceChangeListeners.contains(d))
            deviceChangeListeners.remove(d);
    }

    /**
     * Notify all listening objects that one or more devices has changed.
     * @param list The list of devices that have changed.
     */
    private void notifyDeviceChange(ArrayList<BasicDevice> list)
    {
        for(int i = 0; i < deviceChangeListeners.size(); i++)
            deviceChangeListeners.get(i).onDeviceChange(list);
    }

}
