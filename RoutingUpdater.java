import java.io.IOException;
import java.net.*;
import java.util.ArrayList;

/**
 * This class will be used for sending messages and routing updates
 * to neighboring servers. Ignore this current implementation because it's
 * just an example for udp client.
 */
public class RoutingUpdater extends Thread {
    private static DatagramSocket socket;
    public static boolean isRunning;
    public static int updateInterval;


    public RoutingUpdater(int updateInterval) {
        try {
            socket = new DatagramSocket();
            this.updateInterval = updateInterval;
        }
        catch (SocketException e) {
            throw new RuntimeException(e);
        }

    }

    /**
     * A thread that loops continuously to send routing updates
     * to this server's neighbors. The updateInterval is the number
     * of seconds it takes to send another update.
     */
    @Override
    public void run() {
        isRunning = true;
        while(isRunning) {
            try{
                Thread.sleep(1000*updateInterval);
            } catch(InterruptedException e) {
                throw new RuntimeException(e);
            }
            sendUpdateToNeighbors();
        }
    }

    /**
     * Checks if any neighbors have timed out, if they have the
     * routing table is updated accordingly. After this, it
     * sends routing updates to this server's neighbors.
     */
    public static void sendUpdateToNeighbors() {
        ArrayList<ServerNode> neighborsToTimeout = new ArrayList<>();

        synchronized (Server.servers) {
            ArrayList<ServerNode> servers = Server.servers;
            for (ServerNode server : servers) {
                if (!server.isNeighbor()) continue;

                if (server.isTimedOut(updateInterval)) {
                    neighborsToTimeout.add(server);
                }
            }

            for (ServerNode server : neighborsToTimeout) {
                neighborTimeout(server);
            }

            for (ServerNode server : servers) {
                if (!server.isNeighbor() || server.isTimedOut(updateInterval)) continue;

                try {
                    byte[] routingUpdate = getMessageAsPacket(server.serverID);
                    InetAddress address = InetAddress.getByName(server.serverIPAddress);
                    DatagramPacket packet = new DatagramPacket(
                            routingUpdate, routingUpdate.length, address, server.serverPort
                    );
                    socket.send(packet);

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    //Alejandro Urbano

    /**
     * Disables the link to a server if it is a neighbor.
     * It does this by setting the direct link cost to infinity
     * then it removes every reference the server's routing table.
     * @param serverId id of the server link to disable
     */
     public static void disableServerLink(int serverId){
        synchronized (Server.servers) {
            ServerNode serverToDisable = Server.getServer(serverId);
            if(serverToDisable != null && serverToDisable.isNeighbor()){
                serverToDisable.directLinkCost = Integer.MAX_VALUE;
                Server.removePath(serverId);
                System.out.println("disable SUCCESS\nServer link to " + serverId + " has been disabled.");
            }
            else{
                System.out.println("disable ERROR: Server " + serverId + " is not a neighbor or does not exist.");
            }
        }
     }

    /**
     * Removes direct link to this server and removes any
     * next hop path reference to this server from the routing table.
     * @param server
     */
    public static void neighborTimeout(ServerNode server) {
        DistanceVectorRouting.printMessageFromThread("Node " + server.serverID + " has timed out.");
        server.directLinkCost = Integer.MAX_VALUE;
        Server.removePath(server.serverID);
    }

    /**
     * Sends a routing update message to one neighbor
     * @param neighbor The neighbor that will receive the routing update message
     */
    private static void sendUpdateToNeighbor(ServerNode neighbor) {
        if(neighbor.isTimedOut(updateInterval)) return;
        try {
            byte[] routingUpdate = getMessageAsPacket(neighbor.serverID);
            InetAddress address = InetAddress.getByName(neighbor.serverIPAddress);
            DatagramPacket packet = new DatagramPacket(
                    routingUpdate, routingUpdate.length, address, neighbor.serverPort
            );
            socket.send(packet);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Updates the link of a server to a new cost. If the cost is infinity,
     * the direct link cost is set to -inf so that the server
     * knows that the link will be closed. If the next
     * hop to the neighbor is the direct link, the cost to the
     * neighbor is also updated. The cost is also updated if
     * the new link cost is less than current cost.
     * @param serverId This server's id
     * @param neighborId The neighbor whose link will change
     * @param newCost The new cost of the link
     */
    public static void updateLink(int serverId, int neighborId, String newCost) {
        synchronized (Server.servers) {
            if(serverId != Server.serverId) {
                System.out.println("update ERROR: server id of "+ serverId + " does not match this servers id");
                return;
            }
            ServerNode serverToUpdate = Server.getServer(neighborId);
            if(serverToUpdate != null && serverToUpdate.isNeighbor()) {
                int cost = 0;
                if(newCost.equalsIgnoreCase("inf")){
                    serverToUpdate.directLinkCost = Integer.MIN_VALUE;
                    sendUpdateToNeighbor(serverToUpdate);
                    serverToUpdate.directLinkCost = Integer.MAX_VALUE;
                    Server.removePath(neighborId);
                    System.out.println("update SUCCESS");
                    return;
                }
                try {
                    cost = Integer.parseInt(newCost);
                } catch (NumberFormatException e) {
                    System.out.println("update ERROR: cost must be 'inf' or numeric");
                    return;
                }

                serverToUpdate.directLinkCost = cost;
                if(serverToUpdate.nextHopId == neighborId) serverToUpdate.cost = cost;
                else if(cost < serverToUpdate.cost) {
                    serverToUpdate.cost = cost;
                    serverToUpdate.nextHopId = neighborId;
                }
                sendUpdateToNeighbor(serverToUpdate);
                System.out.println("update SUCCESS");
            }
            else {
                System.out.println("update ERROR: Server with id " + neighborId + " was not found or is not neighbor");
            }
        }
    }

    //Alejandro Urbano
    /**
     * Simulates a server crash by stopping the server
     * from sending and receiving routing update messages.
     */
    public static void CrashServer(){
        if(isRunning && Server.running) {
            isRunning = false;
            Server.running = false;
            System.out.println("crash SUCCESS");
        }
        else {
            System.out.println("crash ERROR: Crash was already called");
        }
    }

    /**
     * Creates a routing update message for the receiver
     * server. The Receivers id is given so that it can be
     * used to send the direct link cost.
     * @param id Receiving server's id
     * @return Returns the message as a byte array
     */
    public static byte[] getMessageAsPacket(int id) {
        RoutingUpdateMessage message = new RoutingUpdateMessage(
                id,
                2+Server.servers.size()*4,
                Server.port,
                Server.ipAddress,
                Server.servers
        );
        return message.getRoutingUpdatePacket();
    }

}

