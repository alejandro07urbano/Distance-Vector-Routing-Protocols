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
    public static ArrayList<RoutingEntry> servers;
    public static ArrayList<RoutingEntry> neighbors;

    // serverId, nextHopServerId
    private boolean running;
    private byte[] buf = new byte[256];
    private int port;
    private int numOfServers;
    private int numOfNeighbors;
    private int serverId;
    public int numOfUpdates = 0;
    String ipAddress;

    /**
     * Initializes instance variables and calls a function that reads
     * the topology file. It also validates that the user's ip is in the
     * topology file.
     */
    public Server() {
        servers = new ArrayList<>();
        neighbors = new ArrayList<>();
        ipAddress = getIPAddress();
    }

    /**
     * Will be used to receive distance vector updates from the server's
     * neighbors.
     */
    public void run() {
        running = true;
        RoutingEntry server = getServer(serverId);
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

            InetAddress address = packet.getAddress();
            int port = packet.getPort();
            String received
                    = new String(packet.getData(), 0, packet.getLength());
            buf = "This is a very long message for testing.".getBytes();
            packet = new DatagramPacket(buf, buf.length, address, port);


            if (received.equals("end")) {
                running = false;
                continue;
            }
            try {
                socket.send(packet);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        socket.close();
    }

    /**
     * Validates the server id that was found in the topology file. It does this
     * by calling a function that checks for the ip of the server and looks for it
     * in the list of servers. If the id is found, it will check if the id matches
     * the last lines of the topology file where neighbor cost is defined.
     * @return Returns a string that describes any error
     */
    private String validateServerId() {
        int serverIdFromIp = getServerId();
        if(serverIdFromIp == -1) {
            return "ERROR: Your ip is not in the topology file.";
        }
        if(numOfNeighbors > 0 && serverId != serverIdFromIp) {
            return "ERROR: Server ID does not match the neighbor cost lines in topology file.";
        }
        serverId = serverIdFromIp;
        RoutingEntry server = getServer(serverId);
        server.cost = 0;
        server.nextHopId = serverId;
        return "SUCCESS";
    }

    /**
     * Gets the IP of the computer running the server.
     * @return Will return an ipv4 address of the computer running the server
     */
    private static String getIPAddress() {
        try {
            Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();

            while (networkInterfaces.hasMoreElements()) {
                NetworkInterface networkInterface = networkInterfaces.nextElement();
                Enumeration<InetAddress> inetAddresses = networkInterface.getInetAddresses();

                while (inetAddresses.hasMoreElements()) {
                    InetAddress inetAddress = inetAddresses.nextElement();
                    // Check if it's an IPv4 address and not a loopback address
                    if (inetAddress instanceof java.net.Inet4Address && !inetAddress.isLoopbackAddress()) {
                        return inetAddress.getHostAddress();
                    }
                }
            }
        } catch (SocketException e) {
            e.printStackTrace();
        }
        return "No ipv4 addresses found";
    }

    /**
     * Gets the server id from the topology file, it does this by looking for the
     * server's ip address in the list of servers that was listed in the topology file.
     * If found it will return the id, if not, it will return -1;
     * @return Will return the id of the server.
     */
    public int getServerId() {
        int index = 1;
        for(RoutingEntry server : servers) {
            if(server.serverIPAddress.equals(ipAddress)) {
                return server.serverID;
            }
            index++;
        }
        return -1;
    }

    /**
     * Not finished yet but will return the routing table for this server
     * as a string
     * @return Will return routing table as a string
     */
    @Override // change this later to the requirements on the assignment for display
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Current serverID: " + serverId + "\n");
        for(int i = 0 ; i < servers.size(); i++){
            RoutingEntry server = servers.get(i);
            if(server == null) continue;
            sb.append(server.serverID + " ")
                .append(server.serverIPAddress + " ")
                .append(server.serverPort + " ")
                .append(server.cost + "\n");
        }
        return sb.toString();
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
                RoutingEntry server = new RoutingEntry(ip, port, id);
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
                RoutingEntry neighbor = getServer(neighborId);
                neighbor.nextHopId = neighbor.serverID;
                neighbor.cost = cost;
                neighbors.add(new RoutingEntry(neighbor, cost));
            }
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
    private boolean addServer(RoutingEntry server) {
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

    public RoutingEntry getServer(int id) {
        int high = servers.size()-1;
        int low = 0;
        while(low <= high) {
            int mid = low + (high - low) / 2;
            RoutingEntry server = servers.get(mid);
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