package com.scottmckittrick.arduinoserverclientlib.TCPService;

/**
 * Interface for an object to receive packets.
 * Created by Scott on 5/6/2017.
 */

public interface PacketReceiver {
    void onPacketReceived(PacketConstants.Packet packet);
}
