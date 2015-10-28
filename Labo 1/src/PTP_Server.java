import java.io.*;
import java.net.*;
import java.lang.*;
import java.util.*;

/**
 * Classe qui gère toute la partie du maître. Elle contient 2 threads:
 *   1. Une thread qui gère l'envoi du temps courant à intervalle régulière
 *   2. Une thread qui répond aux requêtes de delay des esclaves
 */
public class PTP_Server {
    public static void main(String[] args) throws IOException {
        
        // Gère l'arrêt du maître
        boolean end = false;
        
        // Thread qui gère l'envoi des messages SYNC et FOLLOW_UP
        Thread multicast = new Thread() {
            public void run() {
                
                // Initialization des variables
                int id = 0;
                long time;
                
                try {
                    // Obtention de l'addresse IP
                    InetAddress server = null;
                    Enumeration en = NetworkInterface.getNetworkInterfaces();
                    while(en.hasMoreElements()) {
                        NetworkInterface ni=(NetworkInterface) en.nextElement();
                        Enumeration ee = ni.getInetAddresses();
                        while(ee.hasMoreElements()) {
                            InetAddress ia = (InetAddress) ee.nextElement();
                            if (ia.getHostAddress().contains("192.168."))
                                server = ia;
                        }
                    }
                    
                    if (server == null) {
                        System.out.println("ERROR: Unable to obtain server address");
                        System.exit(-1);
                    }
                    
                    // Set-up du socket multicast pour l'envoi
                    System.out.println("Multicast server address is: " + server + "\n");
                    System.out.println("STARTING MULTICAST THREAD...");
                    MulticastSocket socket = new MulticastSocket(PTP_Shared.MULTICAST_SERVER_PORT);
                
                    // Boucle principale du thread
                    while(!end) {
                        // Multicast de SYNC (avec incrémentation de l'identifiant
                        time = System.currentTimeMillis();
                        socket.send(new DatagramPacket(PTP_Shared.makeMessage(PTP_Shared.SYNC, ++id),
                                        PTP_Shared.MESSAGE_SIZE, server, PTP_Shared.MULTICAST_CLIENT_PORT));
                        System.out.println("ID " + id + " SYNC packet sent");
                        
                        // Multicast de FOLLOW_UP avec le timestamp obtenu précedemment
                        socket.send(new DatagramPacket(PTP_Shared.makeTimeMessage(PTP_Shared.FOLLOW_UP, id, time),
                                        PTP_Shared.TIME_MESSAGE_SIZE, server, PTP_Shared.MULTICAST_CLIENT_PORT));
                        System.out.println("ID " + id + " FOLLOW_UP packet sent, time was " + time);
                        
                        Thread.sleep(PTP_Shared.MULTICAST_DELAY);
                    }
                    
                    socket.close();
                    
                } catch (Exception e) {
                    System.out.println("ERROR IN MULTICAST THREAD");
                    return;
                }
            }
        };
        
        // Thread qui gère l'envoi des DELAY_RESPONSE
        Thread response = new Thread() {
            public void run() {
                
                // Initialization des variables
                int id;
                long time;
                byte[] buffer;
                DatagramPacket packet;
                InetAddress client;
                int port;
                
                try {
                    // Set-up du socket pour l'envoi et la récéption de messages
                    System.out.println("STARTING RESPONSE THREAD...");
                    DatagramSocket socket = new DatagramSocket(PTP_Shared.SERVER_PORT);
                    
                    // Boucle principale du thread
                    while(!end) {
                        // Réception du message
                        buffer = new byte[PTP_Shared.MESSAGE_SIZE];
                        packet = new DatagramPacket(buffer, buffer.length);
                        socket.receive(packet);
                        time = System.currentTimeMillis();
                        
                        // On ignore tous les messages autre que DELAY_REQUEST
                        if (PTP_Shared.getMessageType(packet.getData()) == PTP_Shared.DELAY_REQUEST) {
                            
                            // On extrait les données du packet
                            id = PTP_Shared.getMessageID(packet.getData());
                            client = packet.getAddress();
                            port = packet.getPort();
                            System.out.println("Received DELAY_REQUEST packet #" + id + " from " + client + ":" + port);
                            
                            // Envoi de DELAY_RESPONSE avec l'id du DELAY_REQUEST
                            // ainsi que le timestamp de sa réception
                            socket.send(new DatagramPacket(PTP_Shared.makeTimeMessage(PTP_Shared.DELAY_RESPONSE, id, time),
                                            PTP_Shared.TIME_MESSAGE_SIZE, client, port));
                            System.out.println("Sent DELAY_RESPONSE #" + id + " to " + client + ":" + port);
                        }
                    }
                    
                    socket.close();
                    
                } catch (Exception e) {
                    System.out.println("ERROR IN RESPONSE THREAD");
                    return;
                }
            }
        };
        
        // On démarre les deux threads
        response.start();
        multicast.start();
    }
}