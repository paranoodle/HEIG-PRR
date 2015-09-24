import java.io.*;
import java.net.*;
import java.lang.*;

public class PTP_Server {
    
    private static final int SERVER_SOCKET = 4446;
    private static final int CLIENT_SOCKET = 4445;
    private static final int MULTICAST_DELAY = 1000;
    
    public static void main(String[] args) throws IOException {
        
        boolean end = false;
        
        // handles sync and follow-up messages at a set interval
        Thread multicast = new Thread() {
            public void run() {
                
                // initializing variables
                int id = 0;
                long time;
                byte[] buffer;
                DatagramPacket packet;
                
                try {
                    // multicast set-up
                    System.out.println("STARTING MULTICAST SERVER...");
                    InetAddress server = InetAddress.getLocalHost();
                    MulticastSocket socket = new MulticastSocket(SERVER_SOCKET);
                    System.out.println("Multicast server address is: " + server + "\n");
                
                    while(!end) {
                        // SYNC message multicast
                        buffer = PTP_Shared.makeMessage(PTP_Shared.SYNC, id++); // ID incremented!
                        packet = new DatagramPacket(buffer, 5, server, CLIENT_SOCKET);
                        time = System.currentTimeMillis();
                        socket.send(packet);
                        System.out.println("SYNC packet sent");
                        
                        // FOLLOW_UP message multicast
                        buffer = PTP_Shared.makeTimeMessage(PTP_Shared.FOLLOW_UP, id, time);
                        packet = new DatagramPacket(buffer, 13, server, CLIENT_SOCKET);
                        socket.send(packet);
                        System.out.println("FOLLOW_UP packet sent");
                        
                        Thread.sleep(MULTICAST_DELAY);
                    }
                    
                    socket.close();
                    
                } catch (Exception e) {
                    System.out.println("ERROR IN MULTICAST THREAD");
                    return;
                }
                
                //socket.close();
            }
        };
        
        // handles responding to delay messages
        Thread response = new Thread() {
            public void run() {
                
                // initializing variables
                int id;
                long time;
                byte[] buffer;
                DatagramPacket packet;
                InetAddress client;
                int port;
                
                try {
                    // response set-up
                    System.out.println("STARTING RESPONSE THREAD...");
                    DatagramSocket socket = new DatagramSocket(CLIENT_SOCKET);
                    
                    while(!end) {
                        buffer = new byte[PTP_Shared.MESSAGE_SIZE];
                        packet = new DatagramPacket(buffer, buffer.length);
                        
                        // DELAY_REQUEST reception
                        socket.receive(packet);
                        time = System.currentTimeMillis();
                        
                        if (PTP_Shared.getMessageType(packet.getData()) == PTP_Shared.DELAY_REQUEST) {
                            // obtaining packet data
                            id = PTP_Shared.getMessageID(packet.getData());
                            client = packet.getAddress();
                            port = packet.getPort();
                            System.out.println("Received delay_request packet #" + id + " from " + client + ":" + port);
                            
                            // DELAY_RESPONSE emission
                            buffer = PTP_Shared.makeTimeMessage(PTP_Shared.DELAY_RESPONSE, id, time);
                            packet = new DatagramPacket(buffer, buffer.length, client, port);
                            socket.send(packet);
                            System.out.println("Sent delay_response #" + id + " to " + client + ":" + port);
                        }
                    }
                    
                    socket.close();
                    
                } catch (Exception e) {
                    System.out.println("ERROR IN RESPONSE THREAD");
                    return;
                }
            }
        };
        
        response.start();
        multicast.start();
    }
}