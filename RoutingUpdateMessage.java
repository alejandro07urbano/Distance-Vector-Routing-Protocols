import java.nio.ByteBuffer;
import java.util.ArrayList;

public class RoutingUpdateMessage {
    int numberOfUpdateFields;
    int serverPort;
    String serverIPAddress;

    int receiverId;

    int messageSize;

    ArrayList<ServerNode> serverNodes;

    public RoutingUpdateMessage(int receiverId, int numberOfUpdateFields, int serverPort, String serverIPAddress, ArrayList<ServerNode> routingEntries) {
        this.receiverId = receiverId;
        this.numberOfUpdateFields = numberOfUpdateFields;
        this.serverPort = serverPort;
        this.serverIPAddress = serverIPAddress;
        this.serverNodes = routingEntries;
    }
    public RoutingUpdateMessage(byte[] packet) {
        ByteBuffer buffer = ByteBuffer.wrap(packet);
        this.numberOfUpdateFields = buffer.getShort() & 0xFFFF;
        this.serverPort = buffer.getShort() & 0xFFFF;
        this.serverIPAddress = intToIp(buffer.getInt());

        int numOfServers = (numberOfUpdateFields - 2) / 4;
        this.serverNodes = new ArrayList<>();
        for(int i = 0; i < numOfServers; i++) {
            String ip = intToIp(buffer.getInt());
            int port = buffer.getShort() & 0xFFFF;
            int id = buffer.getShort() & 0xFFFF;
            int cost = buffer.getInt();
            this.serverNodes.add(new ServerNode(ip, port, id, cost));
        }
    }

    public int getPacketSize() {
        int headerSize = 8;
        int serverNodeSize = 12;
        return headerSize + (serverNodes.size() * serverNodeSize);
    }

    public int getSenderID() {
        for(ServerNode entry : serverNodes) {
            if(entry.serverIPAddress.equals(this.serverIPAddress)
                    && entry.serverPort == this.serverPort
            ) return entry.serverID;
        }
        return -1;
    }

    public byte[] getRoutingUpdatePacket() {
        ByteBuffer buffer = ByteBuffer.allocate(getPacketSize());
        buffer.putShort((short)numberOfUpdateFields);
        buffer.putShort((short)serverPort);
        buffer.putInt(ipToBytes(serverIPAddress));

        for(ServerNode server : serverNodes) {
            int cost = (receiverId == server.serverID) ? server.directLinkCost : server.cost;
            buffer.putInt(ipToBytes(server.serverIPAddress));
            buffer.putShort((short)server.serverPort);
            buffer.putShort((short)server.serverID);
            buffer.putInt(cost);
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

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("sender id: " + getSenderID() + "\n\n");
        for(ServerNode node : serverNodes) {
            sb.append("ID: " + node.serverID + "\n")
                .append("cost: " + node.cost + "\n");
        }
        return sb.toString();
    }

}
