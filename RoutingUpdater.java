import java.io.IOException;
import java.net.*;

/**
 * This class will be used for sending messages and routing updates
 * to neighboring servers. Ignore this current implementation because it's
 * just an example for udp client.
 */
public class RoutingUpdater extends Thread {
    private DatagramSocket socket;
    private InetAddress address;
    private byte[] buf;

    public RoutingUpdater() {
        try {
            socket = new DatagramSocket();
            address = InetAddress.getByName("localhost");
        } catch(UnknownHostException e) {
            throw new RuntimeException(e);
        }
        catch (SocketException e) {
            throw new RuntimeException(e);
        }

    }

    public void run() {

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

