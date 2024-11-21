import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.File;
import java.nio.file.Path;
import java.sql.Array;
import java.util.*;
import java.net.*;

/**
 * This class is responsible for all server information and activity.
 * It handles receiving messages and calculating vector updates. It also
 * handles reading the topology file.
 */
public class Server extends Thread {

    private DatagramSocket socket;
    public static ArrayList<ServerNode> servers;
    public static boolean running;
    private byte[] buf = new byte[256];
    public static int port;
    private int numOfServers;
    private int numOfNeighbors;
    public static int serverId;
    public int numOfUpdates = 0;
    public static String ipAddress;
    public static boolean updatingValues;

    public static int packetCount;

    /**
     * Initializes instance variables and calls a function that reads
     * the topology file. It also validates that the user's ip is in the
     * topology file.
     */
    public Server() {
        updatingValues = false;
        running = false;
        packetCount = 0;
    }

    /**
     * Will be used to receive distance vector updates from the server's
     * neighbors.
     */
    public void run() {
        running = true;
        ServerNode server = getServer(serverId);
        try {
            socket = new DatagramSocket(server.serverPort);
        } catch (SocketException e) {
            throw new RuntimeException(e);
        }

        while (running) {
            DatagramPacket packet
                    = new DatagramPacket(buf, buf.length);
            try {
                socket.receive(packet);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            RoutingUpdateMessage message = new RoutingUpdateMessage(packet.getData());
            distanceVector(message);
            packetCount++;
        }
        socket.close();
    }

    /**
     * Searches for better path from vector updates received from its neighbors.
     * It works by looking through the current routing table and comparing it with
     * the one received through the message. If it finds a better path, it will
     * update the cost and change the next hop id.
     * @param message The vector update message
     * @see RoutingUpdateMessage
     */
    public void distanceVector(RoutingUpdateMessage message) {
        updatingValues = true;
        int senderId = message.getSenderID();
        ServerNode senderServer = getServer(senderId);


        DistanceVectorRouting.printMessageFromThread("RECEIVED A MESSAGE FROM SERVER " + senderId);
        int senderCost = senderServer.cost;
        ArrayList<ServerNode> updateServers = message.serverNodes;
        for(ServerNode destination : servers) {
            if(destination.serverID == serverId) continue;
            for(ServerNode updateServer : updateServers) {
                if(destination.serverID != updateServer.serverID) continue;
                int newCost = (updateServer.cost == Integer.MAX_VALUE) ? Integer.MAX_VALUE :
                                                                        senderCost + updateServer.cost;
                if(newCost < destination.cost) {
                    destination.cost = newCost;
                    destination.nextHopId = senderId;
                }
            }
        }
        updatingValues = false;
    }

    /**
     * Validates the server id that was found in the topology file. It does this
     * by calling a function that checks for the ip of the server and looks for it
     * in the list of servers. If the id is found, it will check if the id matches
     * the last lines of the topology file where neighbor cost is defined.
     * @return Returns a string that describes any error
     */
    private String validateServerId() {
        String serverIP = getIPAddress();
        ServerNode server = getServer(serverId);

        if(serverIP == null) return "ERROR: Your ip is not in the topology file.";

        if(server != null && !server.serverIPAddress.equals(serverIP))
            return "ERROR: Server ID does not match the neighbor cost lines in topology file.";

        ipAddress = serverIP;
        port = server.serverPort;
        server.cost = 0;
        server.nextHopId = serverId;
        return "SUCCESS";
    }

    private String getIPAddress() {
        ArrayList<String> ips = getIPAddresses();
        for(ServerNode server : servers) {
            for(int i = 0; i < ips.size(); i++) {
                String ip = ips.get(i);
                if(server.serverIPAddress.equals(ip)) return ip;
            }
        }
        return null;
    }

    /**
     * Gets the IP of the computer running the server.
     * @return Will return an ipv4 address of the computer running the server
     */
    private static ArrayList<String> getIPAddresses() {
        ArrayList<String> ips = new ArrayList<>();
        try {
            Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();

            while (networkInterfaces.hasMoreElements()) {
                NetworkInterface networkInterface = networkInterfaces.nextElement();

                Enumeration<InetAddress> inetAddresses = networkInterface.getInetAddresses();

                while (inetAddresses.hasMoreElements()) {
                    InetAddress inetAddress = inetAddresses.nextElement();
                    // Check if it's an IPv4 address and not a loopback address
                    if (inetAddress instanceof java.net.Inet4Address && !inetAddress.isLoopbackAddress()) {
                        ips.add(inetAddress.getHostAddress());
                    }
                }
            }
        } catch (SocketException e) {
            e.printStackTrace();
        }
        return ips;
    }

    /**
     * Gets the server id from the topology file, it does this by looking for the
     * server's ip address in the list of servers that was listed in the topology file.
     * If found it will return the id, if not, it will return -1;
     * @return Will return the id of the server.
     */
    public int getServerId() {
        int index = 1;
        for(ServerNode server : servers) {
            if(server.serverIPAddress.equals(ipAddress)) {
                return server.serverID;
            }
            index++;
        }
        return -1;
    }


    // Display the routing table
    public static void displayRoutingTable() {
        // Display the sorted routing table
        System.out.println("Routing Table:");
        for (ServerNode node : servers) {
            System.out.println(node);
        }
    }

    /**
     * Reads topology file with the Scanner class. This will read line by line
     * and add all the data from the topology file to instance variables
     * such as numOfServers, numOfNeighbors, servers, and neighbors.
     * @param topologyName
     */
    public String readTopologyFile(String topologyName) {
        File topologyFile = new File(topologyName);
        try {
            Scanner fileReader = new Scanner(topologyFile);
            numOfServers = Integer.parseInt(fileReader.nextLine());
            numOfNeighbors = Integer.parseInt(fileReader.nextLine());

            servers = new ArrayList<>();
            for(int i = 0; i < numOfServers; i++) {
                String[] lineEntry = fileReader.nextLine().split(" ");
                int id = Integer.parseInt(lineEntry[0]);
                String ip = lineEntry[1];
                if(!ip.contains(".")) return "ERROR: Number of servers does not match server lines in the topology file";

                int port = Integer.parseInt(lineEntry[2]);
                ServerNode server = new ServerNode(ip, port, id);
                boolean isAdded = addServer(server);
                if(!isAdded) return "ERROR: Duplicate server ids in topology file";
            }

            int prevServerId = -1;
            for(int i = 0; i < numOfNeighbors; i++) {
                String[] lineValues = fileReader.nextLine().split(" ");
                int serverId = Integer.parseInt(lineValues[0]);
                int neighborId = Integer.parseInt(lineValues[1]);
                int cost = Integer.parseInt(lineValues[2]);

                if(prevServerId != -1 && serverId != prevServerId) {
                    return "ERROR: Number of neighbors does not match neighbor lines in the topology file";
                }
                prevServerId = serverId;
                ServerNode neighbor = getServer(neighborId);
                neighbor.cost = cost;
                neighbor.nextHopId = neighborId;
                neighbor.directLinkCost = cost;
            }
            while(fileReader.hasNextLine())
                if(fileReader.nextLine().trim() != "") return "ERROR: More neighbor lines than expected";
            this.serverId = prevServerId;
            return validateServerId();
        }
        catch(NumberFormatException e) {
            return "ERROR: Topology file expected a numeric value";
        }
        catch(NoSuchElementException e) {
            return "ERROR: Topology file does not have enough lines";
        }
        catch (FileNotFoundException e) {
            return "ERROR: Topology file was not found";
        }
    }

    /**
     * Inserts the server in the list to maintain a sorted list.
     * @param server
     * @return Returns a boolean signifying whether it could insert.
     */
    private boolean addServer(ServerNode server) {
        int position = Collections.binarySearch(servers, server, Comparator.comparingInt(s -> s.serverID));
        if (position < 0) {
            position = -position - 1;
        }
        else {
            return false;
        }
        servers.add(position, server);
        return true;
    }

    public static ServerNode getServer(int id) {
        int high = servers.size()-1;
        int low = 0;
        while(low <= high) {
            int mid = low + (high - low) / 2;
            ServerNode server = servers.get(mid);
            if(server.serverID == id) {
                return server;
            }
            if(server.serverID < id) {
                low = mid + 1;
            }
            else {
                high = mid - 1;
            }
        }
        return null;
    }
}