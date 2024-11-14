import java.nio.ByteBuffer;
import java.util.ArrayList;

public class RoutingUpdateMessage {
    int numberOfUpdateFields;
    int serverPort;
    String serverIPAddress;

    int messageSize;

    ArrayList<RoutingEntry> routingEntries;

    public RoutingUpdateMessage(int numberOfUpdateFields, int serverPort, String serverIPAddress, ArrayList<RoutingEntry> routingEntries) {
        this.numberOfUpdateFields = numberOfUpdateFields;
        this.serverPort = serverPort;
        this.serverIPAddress = serverIPAddress;
        this.routingEntries = routingEntries;
    }
    public RoutingUpdateMessage(byte[] packet) {
        ByteBuffer buffer = ByteBuffer.wrap(packet);
        this.numberOfUpdateFields = buffer.getShort() & 0xFFFF;
        this.serverPort = buffer.getShort() & 0xFFFF;
        this.serverIPAddress = intToIp(buffer.getInt());

        int numOfServers = (numberOfUpdateFields - 2) / 4;
        System.out.println(numberOfUpdateFields);
        this.routingEntries = new ArrayList<>();
        for(int i = 0; i < numOfServers; i++) {
            String ip = intToIp(buffer.getInt());
            int port = buffer.getShort() & 0xFFFF;
            int id = buffer.getShort() & 0xFFFF;
            int cost = buffer.getShort() & 0xFFFF;
            this.routingEntries.add(new RoutingEntry(ip, port, id, cost));
        }
        // ask professor about number of update fields.
    }

    public int getPacketSize() {
        return 8 + routingEntries.size() * 10;
    }

    public int getServerID() {
        for(RoutingEntry entry : routingEntries) {
            if(entry.serverIPAddress.equals(this.serverIPAddress)) return entry.serverID;
        }
        return -1;
    }

    public byte[] getRoutingUpdatePacket() {
        ByteBuffer buffer = ByteBuffer.allocate(getPacketSize());
        buffer.putShort((short)numberOfUpdateFields);
        buffer.putShort((short)serverPort);
        buffer.putInt(ipToBytes(serverIPAddress));

        for(RoutingEntry entry : routingEntries) {
            buffer.putInt(ipToBytes(entry.serverIPAddress));
            buffer.putShort((short)entry.serverPort);
            buffer.putShort((short)entry.serverID);
            buffer.putShort((short)entry.cost);
        }
        return buffer.array();
    }

    // Utility function to convert IP address string to integer
    public static int ipToBytes(String ipAddress) {
        String[] octets = ipAddress.split("\\.");
        int ip = 0;
        for (String octet : octets) {
            ip = (ip << 8) | Integer.parseInt(octet);
        }
        return ip;
    }

    // Utility function to convert integer to IP address string
    public static String intToIp(int ipAddress) {
        return String.format("%d.%d.%d.%d",
                (ipAddress >> 24) & 0xFF,
                (ipAddress >> 16) & 0xFF,
                (ipAddress >> 8) & 0xFF,
                ipAddress & 0xFF);
    }


}
