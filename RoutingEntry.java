/**
 * This class is used to store routing information about a server in the network.
 */
class RoutingEntry {
    String serverIPAddress;
    int serverPort;
    int serverID;
    int cost;

    public RoutingEntry(String serverIPAddress, int serverPort, int serverID, int cost) {
        this.serverIPAddress = serverIPAddress;
        this.serverPort = serverPort;
        this.serverID = serverID;
        this.cost = cost;
    }

    public RoutingEntry(String serverIPAddress, int serverPort, int serverID) {
        this.serverIPAddress = serverIPAddress;
        this.serverPort = serverPort;
        this.serverID = serverID;
        this.cost = Integer.MAX_VALUE;
    }

    public RoutingEntry(RoutingEntry server, int cost) {
        this.serverIPAddress = server.serverIPAddress;
        this.serverPort = server.serverPort;
        this.serverID = server.serverID;
        this.cost = cost;
    }
}