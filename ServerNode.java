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

    public ServerNode(String serverIPAddress, int serverPort, int serverID, int cost) {
        this.serverIPAddress = serverIPAddress;
        this.serverPort = serverPort;
        this.serverID = serverID;
        this.cost = cost;
        this.nextHopId = -1;
        this.directLinkCost = Integer.MAX_VALUE;
        this.timeStamp = -1;
    }

    public ServerNode(String serverIPAddress, int serverPort, int serverID) {
        this.serverIPAddress = serverIPAddress;
        this.serverPort = serverPort;
        this.serverID = serverID;
        this.cost = Integer.MAX_VALUE;
        this.nextHopId = -1;
        this.timeStamp = -1;
        this.directLinkCost = Integer.MAX_VALUE;
    }

    public boolean isNeighbor() {
        return directLinkCost != Integer.MAX_VALUE;
    }

    public boolean isTimedOut(int updateInterval) {
        return this.timeStamp != -1 && System.currentTimeMillis() - this.timeStamp >= updateInterval*1000*3;
    }

    @Override
    public String toString() {
        String pathCost = (cost != Integer.MAX_VALUE) ? " "+cost : "inf";
        String nextHop = (nextHopId >= 0) ? " "+nextHopId : " -";
        return serverID + "\t" + nextHop + "\t " + pathCost;
    }

}