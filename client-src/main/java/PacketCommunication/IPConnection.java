package PacketCommunication;

/**
 * Class representing a unique connection to an ip and port
 * */
public class IPConnection {
    private String ip;
    private int port;
    public IPConnection(String ip, int port) throws IllegalArgumentException {
        setIp(ip);
        setPort(port);
    }

    public void setIp(String ip) throws IllegalArgumentException {
        if (!isValidIPv4Address(ip)) {
            throw new IllegalArgumentException("Invalid IPv4 address provided");
        }
        this.ip = ip;
    }

    public String getIp() {
        return ip;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public int getPort() {
        return port;
    }

    /**
     * @return <code>true</code> if the provided IP string is a valid IPv4 address, else <code>false</code>
     * */
    public boolean isValidIPv4Address(String ip) {
        String ipPattern = "\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}";
        return ip != null && ip.matches(ipPattern);
    }

    @Override
    public boolean equals(Object compare) {
        if (compare.getClass() != IPConnection.class) {
            return false;
        }
        return (((IPConnection) compare).getIp().equals(ip) && ((IPConnection) compare).getPort() == port);
    }

    @Override
    public int hashCode() {
        return (ip + port).hashCode();
    }
}