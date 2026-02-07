import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.nio.charset.StandardCharsets;

public class Server {

    public static void main(String args[]) {

        System.out.println("JAVA UDP SERVER");
        DatagramSocket socket = null;
        int port = 9009;

        try {
            socket = new DatagramSocket(port);
            byte[] receiveBuffer = new byte[1024];

            while (true) {
                DatagramPacket receivePacket = new DatagramPacket(receiveBuffer, receiveBuffer.length);
                socket.receive(receivePacket);
                String message = new String(receivePacket.getData(), 0, receivePacket.getLength(),StandardCharsets.UTF_8 );
                System.out.println("Java UDP server received: " + message);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        finally {
            if (socket != null) {
                socket.close();
            }
        }
    }
}