// Avromi Schneierson - 11/3/2023
package PacketCommunication;

import PacketCommunication.enums.PacketArgKey;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * This class is responsible for decoding a list of one or more individual packets. If the packets include a message,
 * the message is decoded based on the order indicated in the packets' headers. Additional packets can be added to this
 * class as they are received.
 * If the packets do not include a message and only include headers, a map of the args is built and retrievable from this
 * class.
 * Args only relevant for this class's internal works are not exposed (e.g., sequence numbers).
 */
public class MultiPacketDecoder {
    private final HashMap<PacketArgKey, String> args = new HashMap<>();
    private String[] packetMessages;
    private HashSet<Integer> missingPackets;
    private final StringBuilder fullMessage = new StringBuilder();
    private int totalPackets = 0;
    private int receivedPackets = 0;
    private float percentComplete = 0;

    public MultiPacketDecoder() {
    }

    public MultiPacketDecoder(List<PacketDecoder> packetDecoders) {
        packetDecoders.forEach(this::addPacket);
    }

    /**
     * Add a packet to this decoders' collection. If the packet added contains a message, the message is organized
     * the message is organized based on its sequence number. To build a full message from multiple packets, simply add
     * all the packets here and they will be organized based on their correct order.
     *
     * @param packetDecoder the packet to add to this decoder
     */
    public void addPacket(PacketDecoder packetDecoder) {
        boolean packetContainsMessage = packetDecoder.containsArg(PacketArgKey.SEQUENCE_NUM);
        if (packetContainsMessage) {
            boolean arrayNotYetCreated = packetMessages == null;
            if (arrayNotYetCreated) {
                totalPackets = Integer.parseInt(packetDecoder.getArg(PacketArgKey.TOTAL_PACKETS));
                packetMessages = new String[totalPackets];
                missingPackets = new HashSet<>();
                for (int i = 0; i < totalPackets; i++) {
                    missingPackets.add(i);
                }
            }

            boolean containsSequenceNum = packetDecoder.containsArg(PacketArgKey.SEQUENCE_NUM);
            if (containsSequenceNum) {  // else, packet will be left as missing, and will hopefully be sent correctly later with the SEQUENCE_NUM
                int sequenceNum = Integer.parseInt(packetDecoder.getArg(PacketArgKey.SEQUENCE_NUM));
                if (missingPackets.contains(sequenceNum)) {
                    packetMessages[sequenceNum] = packetDecoder.getMessage();
                    missingPackets.remove(sequenceNum);
                    receivedPackets++;
                    percentComplete = (receivedPackets / (float) totalPackets) * 100;
                }
            }
            // Always update the completed arg, as even if the packet isn't missing, we want to know if the server
            // completed sending all its packets
            args.put(PacketArgKey.COMPLETED, packetDecoder.getArg(PacketArgKey.COMPLETED));
        } else {
            for (PacketArgKey key : packetDecoder.getArgs().keySet()) {
                args.put(key, packetDecoder.getArg(key));
            }
        }
    }

    /**
     * @param onlyIfComplete whether to only return the message if all packets from the sequence have been received
     * @return the full message from all packets in this sequence
     */
    public String getFullMessage(boolean onlyIfComplete) {
        if (receivedAllPackets()) {
            if (fullMessage.length() == 0) {  // no need to build up the message if we've already stored it
                for (String msg : packetMessages) {
                    fullMessage.append(msg);
                }
            }
            return fullMessage.toString();
        }
        if (!onlyIfComplete) {
            StringBuilder incompleteMessage = new StringBuilder();
            for (String msg : packetMessages) {
                incompleteMessage.append(msg);
            }
            return incompleteMessage.toString();
        }
        return null;
    }

    /**
     * @return <code>true</code> if all packets for the larger message have been received or if the packets
     * added to this decoder do not have a message, else <code>false</code>
     */
    public boolean receivedAllPackets() {
        return missingPackets == null || getMissingPacketNumbers().size() == 0;
    }

    /**
     * @return the sequence numbers of the packets missing that are needed to construct the larger message
     */
    public Set<Integer> getMissingPacketNumbers() {
        return (Set<Integer>) missingPackets.clone();
    }

    /**
     * @return The args for this decoder
     */
    public HashMap<String, String> getArgs() {
        return (HashMap<String, String>) args.clone();
    }


    /**
     * @return <code>true</code> if the provided arg exists
     */
    public boolean containsArg(PacketArgKey key) {
        return args.containsKey(key);
    }

    /**
     * @return the value for the specified arg, if it exists, else null
     */
    public String getArg(PacketArgKey key) {
        return args.get(key);
    }

    public int getNumReceivedPackets() {
        return receivedPackets;
    }

    public int getNumTotalPackets() {
        return totalPackets;
    }

    public float getPercentComplete() {
        return percentComplete;
    }
}


