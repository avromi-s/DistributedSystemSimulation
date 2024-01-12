// Avromi Schneierson - 1/10/2024
package main.classes;

import PacketCommunication.PacketDecoder;
import javafx.concurrent.Task;

import java.io.BufferedReader;
import java.io.IOException;

/**
 * This abstract class is used for receiving packets from a BufferedReader
 */
public abstract class PacketReceiver extends Task<Void> {
    protected PacketDecoder receiveOnePacket(BufferedReader in) throws IOException {
        int input;
        if ((input = in.read()) != -1 && !isCancelled()) {
            PacketDecoder packetDecoder = new PacketDecoder(String.valueOf((char) input));
            while (!packetDecoder.isComplete()) {
                packetDecoder.appendToPacketString(String.valueOf((char) in.read()));
            }
            if (!packetDecoder.isValidPacketSoFar()) {
                return null;
            }
            return packetDecoder;
        } else {
            return null;
        }
    }
}
