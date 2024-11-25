import java.nio.ByteBuffer;
import java.util.ArrayList;

/**
 * A class that contains all information a routing update message.
 * This class is used to read and create routing update messages.
 */
public class RoutingUpdateMessage {
    int numberOfUpdateFields;
    int serverPort;
    String serverIPAddress;

    int receiverId;

    ArrayList<ServerNode> serverNodes;

    /**
     * A constructor to create a routing update message
     * @param receiverId The id of the receiving server
     * @param numberOfUpdateFields The number of update fields in the message
     * @param serverPort The port number of this server
     * @param serverIPAddress The ip of this server
     * @param routingEntries The routing table of this server
     */
    public RoutingUpdateMessage(int receiverId, int numberOfUpdateFields, int serverPort, String serverIPAddress, ArrayList<ServerNode> routingEntries) {
        this.receiverId = receiverId;
        this.numberOfUpdateFields = numberOfUpdateFields;
        this.serverPort = serverPort;
        this.serverIPAddress = serverIPAddress;
        this.serverNodes = routingEntries;
    }

    /**
     * Constructor to read a routing update packet
     * @param packet A byte array containing the routing update message
     */
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

    /**
     * Method to determine the size of a packet
     * @return Returns the size of this packet
     */
    public int getPacketSize() {
        int headerSize = 8;
        int serverNodeSize = 12;
        return headerSize + (serverNodes.size() * serverNodeSize);
    }

    /**
     * Finds the id of the sender by using
     * the ip and port in the header.
     * @return Returns the id of the sender
     */
    public int getSenderID() {
        for(ServerNode entry : serverNodes) {
            if(entry.serverIPAddress.equals(this.serverIPAddress)
                    && entry.serverPort == this.serverPort
            ) return entry.serverID;
        }
        return -1;
    }

    /**
     * Creates a routing update packet using a byte buffer.
     * All values are written to this buffer, then the
     * byte array is returned.
     * @return Returns a byte array of the message
     */
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

    /**
     * A utility function to turn a string ip into
     * an integer. This is done so that it can be
     * added to the packet.
     * @param ipAddress The ip as a string
     * @return Returns the ip stored in an integer
     */
    public static int ipToBytes(String ipAddress) {
        String[] octets = ipAddress.split("\\.");
        int ip = 0;
        for (String octet : octets) {
            ip = (ip << 8) | Integer.parseInt(octet);
        }
        return ip;
    }

    /**
     * Reads an ip that was stored in an integer.
     * This is done to read the ip that comes from
     * a packet.
     * @param ipAddress Ip stored in an integer
     * @return Returns the ip as string
     */
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
