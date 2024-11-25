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

    public static void main(String[] args) {
        Scanner input = new Scanner(System.in);

        while(true) {
            System.out.print(">> ");
            String line = input.nextLine();
            String[] inputs = line.split(" ");
            String command = inputs[0].trim().toLowerCase();
            //Alejandro Urbano 
            //Pre-validation: Validate the command and input arguments 
            if(isValidCommand(inputs)){
                continue;

            }
            switch(command) {
                case "server":
                    if(inputs[1].equals("-t") && inputs.length > 2) {
                        //Alejandro Urbano
                        if (isValidFile(inputs[2])) {
                            readTopology(inputs[2]); 
                            //Display sucess message 
                            System.out.println("server SUCESS");
                        
                    }else{
                        System.out.println("Invalid file name");
                    }
                }
                    if(inputs.length > 4 && inputs[3].equals("-i")) {
                        routingUpdateInterval = Integer.parseInt(inputs[4]);
                        RoutingUpdater ru = new RoutingUpdater(routingUpdateInterval);
                        if(Server.running && !ru.isRunning) ru.start();
                    }
                    break;
                case "update":
                    if(inputs.length == 4) {
                        int serverId = Integer.parseInt(inputs[1]);
                        int neighborId = Integer.parseInt(inputs[2]);
                        String newCost = inputs[3];
                        RoutingUpdater.updateLink(serverId, neighborId, newCost);
                        //Display sucess message 
                        System.out.println("update SUCCESS");
                    }
                    break;
                case "display":
                    Server.displayRoutingTable();
                    System.out.println("display SUCESS");
                    break;
                case "packets":
                    packets();
                    //Display the sucess message 
                    System.out.println("packets SUCESS");
                    break;
                case "disable":
                    if(inputs.length > 1) {
                        RoutingUpdater.disableServerLink(Integer.parseInt(inputs[1]));
                        //Display sucess message 
                        System.out.println("disable SUCESS");
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
    public static synchronized void printMessageFromThread(String s) {
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
     // Validate that the file name is not null or empty
     private static boolean isValidFile(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            System.out.println("File name cannot be null or empty.");
            return false;       
}
//Additional file checking 
return true;
}
// Validate the command and its arguments
private static boolean isValidCommand(String[] inputs) {
    // Validate the 'server' command
    if ("server".equals(inputs[0])) {
        if (inputs.length < 3 || !"-t".equals(inputs[1])) {
            System.out.println("Usage: server -t <topology-file> [-i <interval>]");
            return true;
        }
        // If '-i' is provided, check if it's followed by a valid integer
        if (inputs.length > 4 && "-i".equals(inputs[3])) {
            try {
                routingUpdateInterval = Integer.parseInt(inputs[4]);
            } catch (NumberFormatException e) {
                System.out.println("Interval must be a valid integer.");
                return true;
            }
        }
    }

    // Validate the 'update' command
    if ("update".equals(inputs[0])) {
        if (inputs.length != 4) {
            System.out.println("Usage: update <server-id> <neighbor-id> <cost>");
            return true;
        }
        try {
            Integer.parseInt(inputs[1]); // Server ID
            Integer.parseInt(inputs[2]); // Neighbor ID
        } catch (NumberFormatException e) {
            System.out.println("Server ID and Neighbor ID must be integers.");
            return true;
        }
    }

    // Validate the 'disable' command
    if ("disable".equals(inputs[0])) {
        if (inputs.length < 2) {
            System.out.println("Usage: disable <server-id>");
            return true;
        }
        try {
            Integer.parseInt(inputs[1]); // Server ID
        } catch (NumberFormatException e) {
            System.out.println("Server ID must be an integer.");
            return true;
        }
    }

    // Validate the 'crash', 'display', and 'packets' commands
    if ("crash".equals(inputs[0]) || "display".equals(inputs[0]) || "packets".equals(inputs[0])) {
        if (inputs.length != 1) {
            System.out.println("Usage: " + inputs[0]);
            return true;
        }
    }

    return false;
}
}
