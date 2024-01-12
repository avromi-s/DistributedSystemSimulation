// Avromi Schneierson - 11/3/2023
package PacketCommunication;

import PacketCommunication.enums.PacketArgKey;

import java.util.HashMap;

/**
 * This class decodes a string of tokens sent as a packet into a map of the args and the message string, if applicable
 */
public class PacketDecoder {
    private HashMap<PacketArgKey, String> args = new HashMap<>();
    private final StringBuilder message = new StringBuilder();
    private final StringBuilder packetString = new StringBuilder();

    // Packet symbols:
    private final char LEADING_LENGTH_INDICATOR_CHAR = '(';
    private final char TRAILING_LENGTH_INDICATOR_CHAR = ')';
    private final char KEY_TO_VALUE_SEPARATOR = ':';
    private final char ARG_SEPARATOR = ',';
    private final char ARRAY_VALUES_SEPARATOR = ARG_SEPARATOR;
    private final char LEADING_ARRAY_INDICATOR = '[';
    private final char TRAILING_ARRAY_INDICATOR = ']';
    private final char END_HEADER_INDICATOR = '\n';

    /**
     * Length of the packet's length indicator, including parentheses.
     */
    private int lengthIndicatorNumChars = 0;
    /**
     * The length of the packet string, as indicated in the packet strings preceding parentheses. This does not include
     * the length of the characters in the length indicator itself.
     */
    private int packetContentsLength = 0;

    /**
     * Construct a packet with the packet String
     */
    public PacketDecoder(String packet) {
        packetString.append(packet);
        if (packetLengthMatchesIndicator()) {
            parseContents(packet);
        }
    }

    /**
     * Add additional characters to the packet string. Once the Packet class has received all the characters, the Packet
     * is parsed.
     *
     * @return a <code>boolean</code> indicating if the additional characters were added to the packet string
     */
    public boolean appendToPacketString(String additional) {
        if (!packetLengthMatchesIndicator()) {
            packetString.append(additional);
            if (packetLengthMatchesIndicator()) {
                parseContents(this.packetString.toString());
            }
            return true;
        }
        return false;
    }

    public HashMap<PacketArgKey, String> getArgs() {
        if (packetLengthMatchesIndicator())
            return (HashMap<PacketArgKey, String>) args.clone();
        throw new RuntimeException("Packet is incomplete");
    }

    /**
     * @return <code>true</code> if the provided arg exists
     */
    public boolean containsArg(PacketArgKey key) {
        return args.containsKey(key);
    }


    /**
     * @return the value to which the specified key is mapped, or null if this map contains no mapping for the key
     */
    public String getArg(PacketArgKey key) {
        if (packetLengthMatchesIndicator())
            return args.get(key);
        throw new RuntimeException("Packet is incomplete");
    }

    /**
     * @return the values for the given key when the key points to an array of Strings. If the key doesn't exist or is
     * not an array, null is returned.
     */
    public String[] getStringArrayArg(PacketArgKey key) {
        if (packetLengthMatchesIndicator()) {
            String raw = getArg(key);
            if (raw != null
                    && raw.charAt(0) == LEADING_ARRAY_INDICATOR
                    && raw.charAt(raw.length() - 1) == TRAILING_ARRAY_INDICATOR) {
                return raw.substring(1, raw.length() - 1).split(Character.toString(ARRAY_VALUES_SEPARATOR));
            } else {
                return null;
            }
        }
        throw new RuntimeException("Packet is incomplete");
    }

    /**
     * @return the values for the given key when the key points to an array of ints. If the elements cannot be parsed to
     * ints or the key doesn't exist, null is returned.
     */
    public int[] getIntArrayArg(PacketArgKey key) {
        if (packetLengthMatchesIndicator()) {
            try {
                String[] values = getStringArrayArg(key);
                int[] ints = new int[values.length];
                for (int i = 0; i < values.length; i++) {
                    ints[i] = Integer.parseInt(values[i]);
                }
                return ints;
            } catch (Exception e) {
                return null;
            }
        }
        throw new RuntimeException("Packet is incomplete");
    }

    /**
     * @return the message included in this packet if the packet is complete, else null.
     * Note that even if some of the message has been received, if the packet has not been fully received this method will still
     * return null
     */
    public String getMessage() {
        if (packetLengthMatchesIndicator())
            return message.toString();
        return null;
    }

    /**
     * Takes a packet string and parses it - putting the args and message into their respective variables
     */
    private void parseContents(String packet) {
        String currKey = "";
        HashMap<PacketArgKey, String> keyValueMap = new HashMap<>();
        StringBuilder currToken = new StringBuilder();
        int i = lengthIndicatorNumChars;  // start from after the length indicator
        boolean ignoreCommas = false;
        for (; i < packet.length() && packet.charAt(i) != '\n'; i++) {
            switch (packet.charAt(i)) {
                case KEY_TO_VALUE_SEPARATOR:
                    currKey = currToken.toString();
                    currToken.setLength(0);
                    break;
                case ARG_SEPARATOR:
                    if (!ignoreCommas) {
                        try {
                            keyValueMap.put(PacketArgKey.valueOf(currKey), currToken.toString());
                        } catch (IllegalArgumentException e) {
                            throw new IllegalArgumentException(currKey + " is not a valid PacketArg. " + e.getMessage());
                        }
                        currKey = "";
                        currToken.setLength(0);
                        break;
                        // Only break if we are not ignoring commas. If we are ignoring commas then the above doesn't
                        // run, and we need to continue accumulating the currToken, so we don't break so that this falls
                        // through to the default
                    }
                case LEADING_ARRAY_INDICATOR:
                    ignoreCommas = true;  // so that array elements are not treated as separate args
                    currToken.append(packet.charAt(i));
                    break;
                case TRAILING_ARRAY_INDICATOR:
                    ignoreCommas = false;
                    currToken.append(packet.charAt(i));
                    break;
                default:
                    currToken.append(Character.toUpperCase(packet.charAt(i)));
            }
        }
        try {
            keyValueMap.put(PacketArgKey.valueOf(currKey), currToken.toString());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(currKey + " is not a valid PacketArg. " + e.getMessage());
        }
        boolean containsMessage = i + 1 < packet.length();
        if (containsMessage) {
            message.append(packet.substring(i + 1));
        }
        args = keyValueMap;
    }

    /**
     * @return a boolean indicating if the packet is complete, i.e., contains all characters as indicated by the length
     * indicator
     */
    public boolean isComplete() {
        return packetLengthMatchesIndicator();
    }

    public boolean isValidPacketSoFar() {
        // The below regex essentially allows a string to match if it matches the correct packet syntax up to any point.
        // it does this by setting each character/group optional from the end towards the beginning. so the string
        // is required to match up to any point, but not any in particular.
        String validPacketPattern = "\\((\\d+(\\)((\\w+:\\w+,)*((\\w+(:(\\w+(\\n)?)?)?)?)?)?)?)?(\\w*\\n*)*";
        return packetString.toString().matches(validPacketPattern);
    }

    /**
     * @return a boolean indicating if this instance of the packet is complete, i.e., contains the full packet string and
     * subsequent args, etc. as indicated by the packet string's length with the 'Length' arg
     */
    private boolean packetLengthMatchesIndicator() {
        if (getPacketStringLength(true) == packetString.length()) {
            return true;
        }
        return packetLengthMatchesIndicator(this.packetString.toString());
    }

    /**
     * @return a boolean indicating if the specified packet is complete, i.e., contains the full packet string and
     * subsequent args, etc. as indicated by the packet string's length with the 'Length' arg
     */
    private boolean packetLengthMatchesIndicator(String packet) {
        /*
         * First, make sure the packet has at least the num of characters for the length indicator and starts with the
         * correct character. Then, if it matches the contentsLength + num chars of the length indicator, it has all chars
         * and is complete. If not, then extract the length indicator and check if the length matches up.
         * */
        final int NUM_ENCLOSING_CHARS = 2;
        if (packet.length() < NUM_ENCLOSING_CHARS + 1 || packet.charAt(0) != LEADING_LENGTH_INDICATOR_CHAR || packet.indexOf(TRAILING_LENGTH_INDICATOR_CHAR) < 2) {
            return false;
        }
        if (packetContentsLength + lengthIndicatorNumChars == packet.length()) {
            return true;
        }
        String lengthString = packet.substring(1, packet.indexOf(TRAILING_LENGTH_INDICATOR_CHAR));
        packetContentsLength = Integer.parseInt(lengthString);
        lengthIndicatorNumChars = lengthString.length() + NUM_ENCLOSING_CHARS;
        int actualLengthWithoutIndicator = packet.length() - lengthIndicatorNumChars;  // length of the packet's length indicator is not included in the packets length count
        return actualLengthWithoutIndicator == packetContentsLength;
    }

    private int getPacketStringLength(boolean withLengthIndicator) {
        return withLengthIndicator ? packetContentsLength + lengthIndicatorNumChars : packetContentsLength;
    }

    /**
     * @return a String of this packet in the correct format for transmission
     */
    public String getPacketString() {
        return packetString.toString();
    }

    public String toString() {
        return getPacketString();
    }
}
