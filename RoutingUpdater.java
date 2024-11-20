import java.io.IOException;
import java.net.*;
import java.util.ArrayList;

/**
 * This class will be used for sending messages and routing updates
 * to neighboring servers. Ignore this current implementation because it's
 * just an example for udp client.
 */
public class RoutingUpdater extends Thread {
    private DatagramSocket socket;
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
        ArrayList<ServerNode> servers = Server.servers;
        while(isRunning) {
            try{
                Thread.sleep(1000*updateInterval);
            } catch(InterruptedException e) {
                throw new RuntimeException(e);
            }
            for(ServerNode server : servers) {
                if(!server.isNeighbor()) continue;
                while(Server.updatingValues); // wait for distance vector to finish to avoid incomplete data
                try{
                    byte[] routingUpdate = getMessageAsPacket(server.serverID);
                    InetAddress address = InetAddress.getByName(server.serverIPAddress);
                    DatagramPacket packet = new DatagramPacket(routingUpdate,
                                            routingUpdate.length,
                                            address,
                            server.serverPort);
                    socket.send(packet);

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
    //Alejandro Urbano 
     public void disableServerLink(int serverId){
        ServerNode serverToDisable = Server.getServer(serverId);
        if(serverToDisable != null && serverToDisable.isNeighbor()){
            serverToDisable.cost = Integer.MAX_VALUE;
            serverToDisable.nextHopId =-1;
            System.out.println("Server link to " + serverId + " has been disabled.");
        }
        else{
            System.out.println("Server " + serverId + " is not a neighbor or does not exist.");
        }

     }
    //Alejandro Urbano
    public void CrashServer(int crashedServerId){
        ArrayList<ServerNode> servers = Server.servers;
        for(ServerNode server : servers){
            if(server.serverID == crashedServerId){
                server.cost = Integer.MAX_VALUE;
                server.nextHopId =-1;
                System.out.println("Server " + crashedServerId + " has crashed and is now unreachable.");
                break;
            }
        }
    }

    // update it so that sending link to node send directlink cost for update


    /**
     * Creates a routing update message for the receiver
     * server.
     * @param id Receiving server's id
     * @return
     */
    public byte[] getMessageAsPacket(int id) {
        RoutingUpdateMessage message = new RoutingUpdateMessage(
                id,
                2+Server.servers.size()*4,
                Server.port,
                Server.ipAddress,
                Server.servers
        );
        return message.getRoutingUpdatePacket();
    }


    public String sendEcho(String msg) {
        try {
            buf = msg.getBytes();
            DatagramPacket packet
                    = new DatagramPacket(buf, buf.length, address, 4445);
            socket.send(packet);
            buf = new byte[256];
            packet = new DatagramPacket(buf, buf.length);
            socket.receive(packet);
            String received = new String(
                    packet.getData(), 0, packet.getLength());
            return received;
        } catch(IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void close() {
        socket.close();
    }
}

