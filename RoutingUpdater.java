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
    private InetAddress address;
    public static boolean isRunning;
    public static int updateInterval;

    private byte[] buf;

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
     *
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
     public static void disableServerLink(int serverId){
        synchronized (Server.servers) {
            ServerNode serverToDisable = Server.getServer(serverId);
            if(serverToDisable != null && serverToDisable.isNeighbor()){
                serverToDisable.directLinkCost = Integer.MAX_VALUE;
                Server.removePath(serverId);
                System.out.println("Server link to " + serverId + " has been disabled.");
            }
            else{
                System.out.println("Server " + serverId + " is not a neighbor or does not exist.");
            }
        }
     }

    public static void neighborTimeout(ServerNode server) {
        DistanceVectorRouting.printMessageFromThread("Node " + server.serverID + " has timed out.");
        server.directLinkCost = Integer.MAX_VALUE;
        Server.removePath(server.serverID);
        Server.displayRoutingTable();
    }

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

    public static void updateLink(int serverId, int neighborId, String newCost) {
        synchronized (Server.servers) {
            if(serverId != Server.serverId) return;
            ServerNode serverToUpdate = Server.getServer(neighborId);
            if(serverToUpdate != null && serverToUpdate.isNeighbor()) {
                int cost = 0;
                if(newCost.equalsIgnoreCase("inf")){
                    serverToUpdate.directLinkCost = Integer.MIN_VALUE;
                    sendUpdateToNeighbor(serverToUpdate);
                    serverToUpdate.directLinkCost = Integer.MAX_VALUE;
                    Server.removePath(neighborId);
                    return;
                }
                else cost = Integer.parseInt(newCost);

                serverToUpdate.directLinkCost = cost;
                if(serverToUpdate.nextHopId == neighborId) serverToUpdate.cost = cost;
                else if(cost < serverToUpdate.cost) {
                    serverToUpdate.cost = cost;
                    serverToUpdate.nextHopId = neighborId;
                }
                sendUpdateToNeighbor(serverToUpdate);
            }
        }
    }

    //Alejandro Urbano
    public static void CrashServer(){
        isRunning = false;
        Server.running = false;
    }

    /**
     * Creates a routing update message for the receiver
     * server.
     * @param id Receiving server's id
     * @return
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

    public void close() {
        socket.close();
    }
}

