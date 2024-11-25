import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Scanner;

/**
 * This will be our driver class which will listen for user input.
 */
public class DistanceVectorRouting {
    public static int routingUpdateInterval;

    /**
     * Takes command line arguments to get the topology
     * file name and the routing interval. This
     * method also listens for user input and calls
     * appropriate methods for the command.
     * @param args Includes topology name and routing interval
     */
    public static void main(String[] args) {
        if(args.length == 4) {
            if(!args[0].equalsIgnoreCase("-t")){
                System.err.println("Error: first argument must be -t");
                System.exit(1);
            }
            if(!args[2].equalsIgnoreCase("-i")){
                System.err.println("Error: third argument must be -i");
                System.exit(1);
            }
            readTopology(args[1]);
            routingUpdateInterval = Integer.parseInt(args[3]);
            RoutingUpdater ru = new RoutingUpdater(routingUpdateInterval);
            if(Server.running && !ru.isRunning) ru.start();
        }
        else {
            System.err.println("Error: unexpected number of arguments");
            System.exit(1);
        }

        Scanner input = new Scanner(System.in);

        while(true) {
            System.out.print(">> ");
            String line = input.nextLine();
            String[] inputs = line.split(" ");
            String command = inputs[0].trim().toLowerCase();
            switch(command) {
                case "update":
                    if(inputs.length == 4) {
                        int serverId = Integer.parseInt(inputs[1]);
                        int neighborId = Integer.parseInt(inputs[2]);
                        String newCost = inputs[3];
                        RoutingUpdater.updateLink(serverId, neighborId, newCost);
                    }
                    break;
                case "step":
                    RoutingUpdater.sendUpdateToNeighbors();
                    System.out.println("step SUCCESS");
                    break;
                case "display":
                    if(!Server.running) {
                        System.out.println("display ERROR: This server is no longer running because" +
                                " crash was called.");
                    }
                    else {
                        System.out.println("display SUCCESS");
                        Server.displayRoutingTable();
                    }
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
                case "exit":
                    System.exit(0);
                    break;
                default:
                    System.out.println(inputs[0] + " is not a command.");
            }
        }
    }

    /**
     * This method ensures that threads don't print out
     * messages at the same time.
     * @param s The message to be printed
     */
    public static synchronized void printMessageFromThread(String s) {
        StringBuilder sb = new StringBuilder();
        sb.append(s);
        sb.append("\n>> ");
        sb.insert(0,'\r');
        System.out.print(sb);
    }

    /**
     * Prints out the number of packets this server has
     * received since its last call.
     */
    public static void packets() {
        System.out.println("packets SUCCESS");
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
                System.err.println("The file with named \"" +fileName+"\" was not found.");
                System.exit(1);
            }

            Path filePath = Paths.get(resourceUrl.toURI());
            Server server = new Server();
            String status = server.readTopologyFile(filePath.toString());

            if(!status.equals("SUCCESS")) {
                System.err.println(status);
                System.exit(1);
            }
            server.start();
        } catch(URISyntaxException e) {
            e.printStackTrace();
        }
    }
}