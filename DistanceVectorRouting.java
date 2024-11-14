import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Scanner;

/**
 * This will be our driver class which will listen for user input.
 */
public class DistanceVectorRouting {
    static int routingUpdateInterval;

    public static void main(String[] args) {

        // example of how to write and read message
        ArrayList<RoutingEntry> servers = new ArrayList<>();
        servers.add(new RoutingEntry("192.168.1.232", 3939, 4, 3));
        servers.add(new RoutingEntry("192.168.1.233", 3940, 1, 6));
        servers.add(new RoutingEntry("192.168.1.234", 3941, 2, 9));
        servers.add(new RoutingEntry("192.168.0.112", 4949, 3, 0));
        RoutingUpdateMessage message = new RoutingUpdateMessage(
                                    2+servers.size()*4,
                                    4949,
                                    "192.168.0.112",
                                    servers
        );

        // The getRoutingUpdatePacket returns a byte array of all the information
        RoutingUpdateMessage test = new RoutingUpdateMessage(message.getRoutingUpdatePacket());
        System.out.println("Current ServerID: " + test.getServerID() + "\n");
        System.out.println("Number of update fields: " + test.numberOfUpdateFields);
        System.out.println("Port: " + test.serverPort);
        System.out.println("IP: " + test.serverIPAddress + "\n");
        for(RoutingEntry entry: test.routingEntries) {
            System.out.println("ServerID: " + entry.serverID);
            System.out.println("ServerIP: " + entry.serverIPAddress);
            System.out.println("ServerPort: " + entry.serverPort);
            System.out.println("ServerCost: " + entry.cost + "\n");
        }

        Scanner input = new Scanner(System.in);
        while(true) {
            System.out.print(">> ");
            String inputLine = input.nextLine();
            String[] inputs = inputLine.split(" ");

            switch(inputs[0]) {
                case "server":
                    if(inputs[1].equals("-t") && inputs.length > 2) {
                        readTopology(inputs[2]);
                    }
                    if(inputs.length > 4 && inputs[3].equals("-i")) {
                        routingUpdateInterval = Integer.parseInt(inputs[4]);
                    }
                    break;
                default:
                    System.out.println(inputs[0] + " is not a command.");
            }
        }
    }

    /**
     * If the topology file is found, it will create a new server object which
     * takes the topology file path. The object will read the topology file
     * and store its values.
     * @param fileName the name of the topology file
     */
    public static void readTopology(String fileName) {
        try {
            URL resourceUrl = DistanceVectorRouting.class.getResource(fileName);
            if(resourceUrl == null) {
                System.out.println("The file with named \"" +fileName+"\" was not found.");
                return;
            }

            Path filePath = Paths.get(resourceUrl.toURI());
            Server server = new Server(filePath.toString());
            System.out.println(server);
        } catch(URISyntaxException e) {
            e.printStackTrace();
        }
    }
}