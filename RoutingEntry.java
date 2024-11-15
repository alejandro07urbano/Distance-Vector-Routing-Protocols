/**
 * This class is used to store routing information about a server in the network.
 */
class RoutingEntry {
    String serverIPAddress;
    int serverPort;
    int serverID;
    int cost;
    int nextHopId;

    public RoutingEntry(String serverIPAddress, int serverPort, int serverID, int cost) {
        this.serverIPAddress = serverIPAddress;
        this.serverPort = serverPort;
        this.serverID = serverID;
        this.cost = cost;
        this.nextHopId = -1;
    }

    public RoutingEntry(String serverIPAddress, int serverPort, int serverID) {
        this.serverIPAddress = serverIPAddress;
        this.serverPort = serverPort;
        this.serverID = serverID;
        this.cost = Integer.MAX_VALUE;
        this.nextHopId = -1;
    }

    public RoutingEntry(RoutingEntry server, int cost) {
        this.serverIPAddress = server.serverIPAddress;
        this.serverPort = server.serverPort;
        this.serverID = server.serverID;
        this.cost = cost;
        this.nextHopId = server.nextHopId;
    }
}