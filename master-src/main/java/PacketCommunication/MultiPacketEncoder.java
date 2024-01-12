// Avromi Schneierson - 11/3/2023
package PacketCommunication;

import PacketCommunication.enums.PacketArgKey;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * This class is responsible for taking a full message and encoding it into packet Strings formatted to be sent to or
 * from a Client
 */
public class MultiPacketEncoder {
    private final int MAX_MESSAGE_LENGTH = 100;
    private final ArrayList<PacketEncoder> packets = new ArrayList<>();

    private int totalPackets = 0;

    /**
     * Constructor that creates the individual packet(s) for this message
     *
     * @param fullMessage the message to send, if applicable. If included, it will be broken up into packets each with a
     *                    max length of MAX_MESSAGE_LENGTH. The Completed, TotalPackets, and SequenceNum args are
     *                    automatically applied to the packets as broken down.
     * @param args        the args to include in each packet. This is primarily used when trying to send a packet without any
     *                    message (e.g., a packet sent from the client just indicating which packets it is missing). However, it
     *                    can also be used to include additional args besides for the auto-generated ones when sending a message.
     */
    public MultiPacketEncoder(HashMap<PacketArgKey, String> args, HashMap<PacketArgKey, Object[]> arrayArgs, String fullMessage) {
        boolean containsMessage = fullMessage != null && fullMessage.length() > 0;
        if (containsMessage) {
            totalPackets = (fullMessage.length() / MAX_MESSAGE_LENGTH) +
                    (fullMessage.length() % MAX_MESSAGE_LENGTH > 0 ? 1 : 0);
            StringBuilder currMessage = new StringBuilder();
            int sequenceNum = 0;
            for (int charNum = 0; charNum < fullMessage.length(); charNum++) {
                currMessage.append(fullMessage.charAt(charNum));
                if ((charNum + 1) % MAX_MESSAGE_LENGTH == 0 || charNum == fullMessage.length() - 1) {
                    PacketEncoder packet = new PacketEncoder(args, arrayArgs, currMessage.toString());

                    // All packets are given a default value of F, even the last. It is up to the packet sender to modify
                    // this value if it is the last packet they send, as they determine which packet is actually sent last.
                    packet.setArg(PacketArgKey.COMPLETED, "F");
                    packet.setArg(PacketArgKey.TOTAL_PACKETS, String.valueOf(totalPackets));
                    packet.setArg(PacketArgKey.SEQUENCE_NUM, String.valueOf(sequenceNum));
                    sequenceNum++;
                    packets.add(packet);
                    currMessage = new StringBuilder();
                }
            }
        } else {
            packets.add(new PacketEncoder(args, arrayArgs));
        }
    }

    /**
     * @return an ArrayList of all the packets
     */
    public ArrayList<PacketEncoder> getPackets() {
        return packets;
    }

    public int getNumTotalPackets() {
        return totalPackets;
    }
}



