import java.sql.Timestamp;

/**
 * This class is used to store routing information about a server in the network.
 */
class ServerNode {
    String serverIPAddress;
    int serverPort;
    int serverID;
    int cost;
    int directLinkCost;
    int nextHopId;

    long timeStamp;

    /**
     * Initializes variables and sets default values for nextHopId,
     * directLinkCost, and timeStamp.
     * @param serverIPAddress The ip of a server in the network
     * @param serverPort The port of a server in the network
     * @param serverID The server id of a server in the network
     * @param cost The cost of a server in the network
     */
    public ServerNode(String serverIPAddress, int serverPort, int serverID, int cost) {
        this.serverIPAddress = serverIPAddress;
        this.serverPort = serverPort;
        this.serverID = serverID;
        this.cost = cost;
        this.nextHopId = -1;
        this.directLinkCost = Integer.MAX_VALUE;
        this.timeStamp = -1;
    }

    /**
     * This is another constructor that is used when cost is not known.
     * This is used when reading the topology file.
     * @param serverIPAddress The ip of a server in the network
     * @param serverPort The port of a server in the network
     * @param serverID The server id of a server in the network
     */
    public ServerNode(String serverIPAddress, int serverPort, int serverID) {
        this.serverIPAddress = serverIPAddress;
        this.serverPort = serverPort;
        this.serverID = serverID;
        this.cost = Integer.MAX_VALUE;
        this.nextHopId = -1;
        this.timeStamp = -1;
        this.directLinkCost = Integer.MAX_VALUE;
    }

    /**
     * Checks if server is a neighbor.
     * @return Returns a boolean signifying if it is a neighbor.
     */
    public boolean isNeighbor() {
        return directLinkCost != Integer.MAX_VALUE;
    }

    /**
     * Checks if this server has timed out by checking
     * if the timestamp was initialized and then checks
     * if three routing updates have passed before receiving
     * a message.
     * @param updateInterval Routing update interval
     * @return Returns boolean if server is timed out
     */
    public boolean isTimedOut(int updateInterval) {
        return this.timeStamp != -1 && System.currentTimeMillis() - this.timeStamp >= updateInterval*1000*3;
    }

    /**
     * A to string method that returns a row in the routing table
     * for this destination.
     * @return Returns a row in the routing table
     */
    @Override
    public String toString() {
        String pathCost = (cost != Integer.MAX_VALUE) ? " "+cost : "inf";
        String nextHop = (nextHopId >= 0) ? " "+nextHopId : " -";
        return serverID + "\t" + nextHop + "\t " + pathCost;
    }

}