import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class Client {

    public static void main(String[] args) {

        System.out.println("JAVA UDP CLIENT");
        DatagramSocket socket = null;

        try {
            socket = new DatagramSocket();

            InetAddress serverAddress = InetAddress.getByName("localhost");
            int serverPort = 9008;

            String message = "Ping Java Udp";
            byte[] sendBuffer = message.getBytes();

            DatagramPacket sendPacket =new DatagramPacket(sendBuffer,sendBuffer.length, serverAddress,serverPort);

            socket.send(sendPacket);

            byte[] receiveBuffer = new byte[1024];
            DatagramPacket receivePacket =new DatagramPacket(receiveBuffer, receiveBuffer.length);
            socket.receive(receivePacket);
            String response = new String(receivePacket.getData(),0,receivePacket.getLength());
            System.out.println("Response server: " + response);

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (socket != null) {
                socket.close();
            }
        }
    }
}