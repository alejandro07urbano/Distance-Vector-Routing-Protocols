import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicBoolean;


/**
 * This will be our driver class which will listen for user input.
 */
public class DistanceVectorRouting {
    public static int routingUpdateInterval;
    public static boolean waitingForInput;

    public static void main(String[] args) {
        Scanner input = new Scanner(System.in);

        while(true) {
            waitingForInput = true;
            System.out.print(">> ");
            String line = input.nextLine();
            waitingForInput = false;
            String[] inputs = line.split(" ");

            switch(inputs[0]) {
                case "server":
                    if(inputs[1].equals("-t") && inputs.length > 2) {
                        readTopology(inputs[2]);
                    }
                    if(inputs.length > 4 && inputs[3].equals("-i")) {
                        routingUpdateInterval = Integer.parseInt(inputs[4]);
                        RoutingUpdater ru = new RoutingUpdater(routingUpdateInterval);
                        if(Server.running && !ru.isRunning) ru.start();
                    }
                    break;
                case "display":
                    Server.displayRoutingTable();
                    break;
                case "packets":
                    packets();
                    break;
                case "disable":
                    if(inputs.length > 1) {
                        RoutingUpdater.disableServerLink(Integer.parseInt(inputs[1]));
                    }
                    break;
                case "crash":
                    RoutingUpdater.CrashServer();
                    break;
                default:
                    System.out.println(inputs[0] + " is not a command.");
            }
        }
    }

    // Safely print a message and preserve the user's input
    public static void printMessageFromThread(String s) {
        StringBuilder sb = new StringBuilder();
        sb.append(s);
        sb.append("\n>> ");
        sb.insert(0,'\r');
        System.out.print(sb);
    }

    // Method to display the number of packets received since the last call
    public static void packets() {
        System.out.println("Number of distance vector packets received since last call: " + Server.packetCount);
        // Reset the packet count
        Server.packetCount = 0;
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
            Server server = new Server();
            String status = server.readTopologyFile(filePath.toString());
            System.out.println("server " + status);
            if(!status.equals("SUCCESS")) return;
            server.start();
            Server.displayRoutingTable();
        } catch(URISyntaxException e) {
            e.printStackTrace();
        }
    }
}