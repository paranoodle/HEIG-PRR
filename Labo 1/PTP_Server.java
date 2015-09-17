import java.io.*;
import java.net.*;
import java.lang.*;

public class PTP_Server {
    
    private static final int SERVER_SOCKET = 4446;
    private static final int CLIENT_SOCKET = 4445;
    
    public static void main(String[] args) throws IOException {
        
        long time;
        byte[] buffer;
        
        
        // group of clients
        InetAddress server = InetAddress.getLocalHost();
        System.out.println("Server address is: " + server);
        MulticastSocket socket = new MulticastSocket(SERVER_SOCKET);
        
        // SYNC message multicast
        buffer = {PTP_Shared.SYNC};
        DatagramPacket packet = new DatagramPacket(buffer, 1, server, CLIENT_SOCKET);
        time = System.currentTimeMillis();
        socket.send(packet);
        
        // FOLLOW_UP message multicast
        buffer = new byte[9];
        buffer[0] = PTP_Shared.FOLLOW_UP;
        
        // after end of main loop
        socket.close();
    }
}