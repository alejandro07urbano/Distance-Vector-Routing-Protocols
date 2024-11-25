import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.File;
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
    public static String ipAddress;

    public static int packetCount;

    /**
     * Initializes instance variables and calls a function that reads
     * the topology file. It also validates that the user's ip is in the
     * topology file.
     */
    public Server() {
        servers = new ArrayList<>();
        running = false;
        packetCount = 0;
    }

    /**
     * This is a new thread that listens for routing update messages from this
     * server's neighbors.
     */
    public void run() {
        ServerNode server = getServer(serverId);
        try {
            socket = new DatagramSocket(server.serverPort);

        } catch (BindException e) {
            System.err.println("ERROR: The port " + server.serverPort + " is already in use");
            System.exit(1);
        }
        catch (SocketException e) {
            throw new RuntimeException(e);
        }
        running = true;
        System.out.println("Server started successfully");
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

        }
        socket.close();
    }

    /**
     * Searches for better path from vector updates received from its neighbors.
     * It works by looking through the current routing table and comparing it with
     * the one received through the message. If it finds a better path, it will
     * update the cost and change the next hop id. This also updates existing paths
     * if they were changed.
     * @param message The routing update message
     * @see RoutingUpdateMessage
     */
    public void distanceVector(RoutingUpdateMessage message) {
        synchronized (servers) {
            int senderId = message.getSenderID();
            ServerNode senderServer = getServer(senderId);

            if(!senderServer.isNeighbor()) return;
            senderServer.timeStamp = System.currentTimeMillis();
            packetCount++;

            ArrayList<ServerNode> updateServers = message.serverNodes;
            if(!updateDirectPath(senderServer, updateServers)) return;

            DistanceVectorRouting.printMessageFromThread("RECEIVED A MESSAGE FROM SERVER " + senderId);
            int senderCost = senderServer.directLinkCost;
            for(ServerNode destination : servers) {
                if(destination.serverID == serverId) continue;
                for(ServerNode updateServer : updateServers) {
                    if(destination.serverID != updateServer.serverID) continue;

                    int newCost = (updateServer.cost == Integer.MAX_VALUE)
                            ? Integer.MAX_VALUE : senderCost + updateServer.cost;

                    if(senderId == destination.nextHopId ) {
                        if(destination.cost != newCost) {
                            destination.cost = newCost;
                            destination.nextHopId = (newCost == Integer.MAX_VALUE) ? -1 : senderId;
                        }
                    }

                    else if (newCost < destination.cost || destination.cost == Integer.MAX_VALUE) {
                        destination.cost = newCost;
                        destination.nextHopId = (newCost == Integer.MAX_VALUE) ? -1 : senderId;
                    }
                }
            }
        }
    }

    /**
     * This updates the direct link from a server. This is called within the distanceVector
     * method before values are updated. This is done so that the latest link is used when
     * comparing paths.
     * @param senderServer The server that is updating the link
     * @param updateServers The servers routing table to find the direct link
     * @return Returns false if link was removed
     */
    private static boolean updateDirectPath(ServerNode senderServer, ArrayList<ServerNode> updateServers) {
        for(ServerNode updateServer : updateServers) {
            if(updateServer.serverID != serverId) continue;
            if(updateServer.cost == Integer.MIN_VALUE) {
                senderServer.directLinkCost = Integer.MAX_VALUE;
                removePath(senderServer.serverID);
                return false;
            }

            senderServer.directLinkCost = updateServer.cost;
            if(senderServer.nextHopId == senderServer.serverID) senderServer.cost = updateServer.cost;
            else if(updateServer.cost < senderServer.cost) {
                senderServer.cost = updateServer.cost;
                senderServer.nextHopId = senderServer.serverID;
            }
        }
        return true;
    }

    /**
     * Removes the path of a server from the routing table. It does this
     * by looking through the routing table and comparing the next hop id
     * with the path id. If they are equal, the path is reset to inf and the
     * next hop is set to -1.
     * @param pathId The id of the server that will be removed from the routing table
     */
    public static void removePath(int pathId) {
        synchronized (servers) {
            for (ServerNode server : servers) {
                if (server.serverID == serverId) continue;

                if (pathId == server.serverID || server.nextHopId == pathId) {
                    server.cost = Integer.MAX_VALUE;
                    server.nextHopId = -1;
                }
            }
        }
    }

    /**
     * Checks if the computers ip is found in the topology file. It
     * also checks if the neighbor lines have the correct id.
     * @return Returns a string that describes any error
     */
    private String validateServerIP() {
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

    /**
     * Finds the computers ip from the topology file. It does this
     * by comparing every ipv4 address from the computer with every ip
     * in the topology file. This is done just in case there is
     * more ipv4 addresses that are not currently used like wsl ip address.
     * @return Returns the ip found in the topology file
     */
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
     * Looks through all addresses on the computer, makes sure they are not loopback,
     * and makes sure they are ipv4 addresses.
     * @return Will return all ipv4 addresses of the computer
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
     * Prints out the routing table for this server.
     * The ServerNode class contains a toString method
     * which prints out a row of the routing table.
     * @see ServerNode
     */
    public static void displayRoutingTable() {
        synchronized (servers) {
            System.out.println("Routing Table for " +serverId+":");
            for (ServerNode node : servers) {
                System.out.println(node);
            }
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
                if(fileReader.nextLine().trim() != "") return "ERROR: More lines than expected in topology file";
            this.serverId = prevServerId;
            return validateServerIP();
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
     * This is done so that the routing table printed in sorted order
     * by id.
     * @param server
     * @return Returns a boolean signifying whether it could be inserted.
     */
    private boolean addServer(ServerNode server) {
        synchronized (servers) {
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
    }

    /**
     * Gets a server from the routing table by their id.
     * @param id The id of the server
     * @return Returns a ServerNode from the routing table
     */
    public static ServerNode getServer(int id) {
        synchronized (servers) {
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
}