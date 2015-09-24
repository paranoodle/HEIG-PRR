import java.io.*;
import java.net.*;
import java.lang.*;

public class PTP_Server {
    
    private static final int SERVER_SOCKET = 4446;
    private static final int CLIENT_SOCKET = 4445;
    private static final int MULTICAST_DELAY = 2000;
    
    public static void main(String[] args) throws IOException {
        
        // handles sync and follow-up messages at a set interval
        Thread multicast = new Thread() {
            public void run() {
                // initializing variables
                int id = 0;
                long time;
                byte[] buffer;
                DatagramPacket packet;
                
                // group of clients
                System.out.println("STARTING MULTICAST SERVER...");
                InetAddress server = InetAddress.getLocalHost();
                MulticastSocket socket = new MulticastSocket(SERVER_SOCKET);
                System.out.println("Multicast server address is: " + server + "\n");
                
                while(true) {
                    // SYNC message multicast
                    buffer = PTP_Shared.makeMessage(PTP_Shared.SYNC, id++); // ID incremented!
                    packet = new DatagramPacket(buffer, 5, server, CLIENT_SOCKET);
                    time = System.currentTimeMillis();
                    socket.send(packet);
                    System.out.println("SYNC packet sent, size " + (buffer.length) + " bytes");
                    
                    // FOLLOW_UP message multicast
                    buffer = PTP_Shared.makeTimeMessage(PTP_Shared.FOLLOW_UP, id, time);
                    packet = new DatagramPacket(buffer, 13, server, CLIENT_SOCKET);
                    socket.send(packet);
                    System.out.println("FOLLOW_UP packet sent, size " + (buffer.length) + " bytes");
                }
                
                socket.close();
            }
        }
        
        // handles responding to delay messages
        Thread response = new Thread() {
            public void run() {
                System.out.println("STARTING RESPONSE THREAD...");
                DatagramSocket socket = new DatagramSocket(CLIENT_SOCKET);
                
                while(true) {
                    DatagramPacket packet = new DatagramPacket();
                }
            }
        }
        
        multicast.start();
        response.start();
    }
}