// Avromi Schneierson - 11/3/2023
package PacketCommunication;

import PacketCommunication.enums.PacketArgKey;

import java.util.HashMap;

/**
 * This class encodes args and an optional message into the appropriate string of tokens to be sent as a packet
 */
public class PacketEncoder {
    private HashMap<PacketArgKey, String> args = new HashMap<>();
    private final StringBuilder message = new StringBuilder();

    // Packet symbols:
    private final char LEADING_LENGTH_INDICATOR_CHAR = '(';
    private final char TRAILING_LENGTH_INDICATOR_CHAR = ')';
    private final char KEY_TO_VALUE_SEPARATOR = ':';
    private final char ARG_SEPARATOR = ',';
    private final char ARRAY_VALUES_SEPARATOR = ARG_SEPARATOR;
    private final char LEADING_ARRAY_INDICATOR = '[';
    private final char TRAILING_ARRAY_INDICATOR = ']';
    private final char END_HEADER_INDICATOR = '\n';

    public PacketEncoder() {
    }

    public PacketEncoder(HashMap<PacketArgKey, String> args, HashMap<PacketArgKey, Object[]> arrayArgs, String message) {
        setArgs(args, arrayArgs, true);
        setMessage(message);
    }

    public PacketEncoder(HashMap<PacketArgKey, String> args, HashMap<PacketArgKey, Object[]> arrayArgs) {
        setArgs(args, arrayArgs, true);
    }

    public PacketEncoder(HashMap<PacketArgKey, String> args) {
        setArgs(args, new HashMap<>(), true);
    }

    /**
     * Set multiple args at a time
     *
     * @param args      the map of args to set
     * @param arrayArgs the map of args to set where the values are arrays
     * @param resetArgs boolean indicating if any other args already set should be deleted or kept if they are not being
     *                  overwritten
     */
    public void setArgs(HashMap<PacketArgKey, String> args, HashMap<PacketArgKey, Object[]> arrayArgs, boolean resetArgs) {
        if (resetArgs) {
            this.args = new HashMap<>();
        }
        for (PacketArgKey key : args.keySet()) {
            setArg(key, args.get(key));
        }
        for (PacketArgKey key : arrayArgs.keySet()) {
            setArg(key, arrayArgs.get(key));
        }
    }

    /**
     * Set an arg to a value
     */
    public void setArg(PacketArgKey key, String value) {
        this.args.put(key, value);
    }

    /**
     * Set an arg to an array of values
     */
    public void setArg(PacketArgKey key, Object[] values) {
        StringBuilder valuesStr = new StringBuilder();
        valuesStr.append(LEADING_ARRAY_INDICATOR);
        for (Object value : values) {
            valuesStr.append(value).append(ARRAY_VALUES_SEPARATOR);
        }
        valuesStr.setCharAt(valuesStr.length() - 1, TRAILING_ARRAY_INDICATOR);
        setArg(key, valuesStr.toString());
    }

    public void deleteArg(PacketArgKey key) {
        this.args.remove(key);
    }

    /**
     * Set the packet's message
     *
     * @param message the String to set as this packet's message
     */
    public void setMessage(String message) {
        this.message.setLength(0);
        this.message.append(message);
    }

    public void addToMessage(String additional) {
        message.append(additional);
    }

    public String getMessage() {
        return message.toString();
    }

    /**
     * @return a String of this packet in the correct format for transmission
     */
    public String getPacketString() {
        StringBuilder packet = new StringBuilder();
        for (PacketArgKey key : args.keySet()) {
            packet.append(key).append(KEY_TO_VALUE_SEPARATOR).append(args.get(key)).append(ARG_SEPARATOR);
        }
        packet.setCharAt(packet.length() - 1, END_HEADER_INDICATOR);
        packet.append(getMessage());
        String packetStr = packet.toString();

        // Now, prepend the length indicator
        int packetLength = packetStr.length();
        packetStr = LEADING_LENGTH_INDICATOR_CHAR + String.valueOf(packetLength) + TRAILING_LENGTH_INDICATOR_CHAR + packetStr;
        return packetStr;
    }

    public String toString() {
        return getPacketString();
    }
}