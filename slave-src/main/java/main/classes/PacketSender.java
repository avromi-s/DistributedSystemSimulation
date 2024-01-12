// Avromi Schneierson - 1/10/2024
package main.classes;

import PacketCommunication.PacketEncoder;
import javafx.concurrent.Task;

import java.io.PrintWriter;

/**
 * This abstract class is used for sending a packet to a PrintWriter
 */
public abstract class PacketSender extends Task<Void> {
    protected boolean sendPacket(PrintWriter out, PacketEncoder packetEncoder) {
        out.print(packetEncoder);
        return !out.checkError();
    }
}
