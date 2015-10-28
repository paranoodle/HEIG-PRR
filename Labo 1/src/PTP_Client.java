import java.io.IOException;
import java.lang.System;
import java.net.*;
import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.nio.ByteBuffer;
import java.util.Random;

public class PTP_Client {
    public static void main(String args[]) throws IOException{
        if(args.length < 1){
            System.out.println("IP Address of server is missing !");
            System.exit(-1);
        }
        
        String serverIP = args[0];
        InetAddress server = InetAddress.getByName(serverIP);
        
        boolean end = false;
        
        Thread client = new Thread() {
            public void run() {
                // Initialization des variables
                byte[] buffer;
                DatagramPacket packet;
                int sync_id, delay_id;
                long sync_receive_time, sync_emit_time, delay_request_time, delay_response_time;
                long offset = 0;
                long delay = 0;
                long time = 0;
                
                try {
                    MulticastSocket multi_socket = new MulticastSocket(PTP_Shared.MULTICAST_CLIENT_PORT);
                    // Set-up du socket pour l'envoi et la récéption de messages de delay
                    DatagramSocket delay_socket = new DatagramSocket(PTP_Shared.CLIENT_PORT);
                    
                    // Boucle principale du thread
                    while(!end) {
                        // Set-up du socket multicast pour la récéption
                        if (multi_socket.isClosed())
                            multi_socket = new MulticastSocket(PTP_Shared.MULTICAST_CLIENT_PORT);
                        
                        // On attend le message SYNC
                        buffer = new byte[PTP_Shared.MESSAGE_SIZE];
                        packet = new DatagramPacket(buffer, buffer.length);
                        multi_socket.receive(packet);
                        sync_receive_time = System.currentTimeMillis();
                        
                        // On ignore les messages autre que SYNC
                        if (PTP_Shared.getMessageType(packet.getData()) != PTP_Shared.SYNC)
                            continue;
                        
                        // On extrait les données du packet
                        sync_id = PTP_Shared.getMessageID(packet.getData());
                        System.out.println("Received ID " + sync_id + " SYNC message at time " + sync_receive_time);
                        
                        // On attend le message FOLLOW_UP
                        buffer = new byte[PTP_Shared.TIME_MESSAGE_SIZE];
                        packet = new DatagramPacket(buffer, buffer.length);
                        multi_socket.receive(packet);
                                                
                        // Si le message n'est pas un FOLLOW_UP, on recommence
                        if (PTP_Shared.getMessageType(packet.getData()) != PTP_Shared.FOLLOW_UP)
                            continue;
                        
                        // Si l'id du FOLLOW_UP ne correspond pas à celle du SYNC, on recommence
                        if (sync_id != PTP_Shared.getMessageID(packet.getData()))
                            continue;
                        
                        // On extrait les données du packet et on fait les calculs
                        System.out.println("Received ID " + sync_id + " FOLLOW_UP message");
                        sync_emit_time = PTP_Shared.getMessageTime(packet.getData());
                        offset = sync_receive_time - sync_emit_time;
                        System.out.println("Calculated new offset of " + offset + "ms");
                        
                        // On ferme le socket, on ne veut pas recevoir d'autres messages avant de finir
                        // de gérer les DELAY_REQUEST et DELAY_RESPONSE
                        multi_socket.close();
                        
                        // On attend avant d'envoyer le message DELAY_REQUEST
                        // TODO: mettre les vraies valeurs
                        Thread.sleep((int)(Math.random() * 56 + 4) * PTP_Shared.MULTICAST_DELAY);
                        delay_id = (int)(Math.random() * 1000);
                        
                        // Envoi de DELAY_REQUEST avec notre id
                        delay_request_time = System.currentTimeMillis();
                        delay_socket.send(new DatagramPacket(PTP_Shared.makeMessage(PTP_Shared.DELAY_REQUEST, delay_id),
                                        PTP_Shared.MESSAGE_SIZE, server, PTP_Shared.SERVER_PORT));
                        System.out.println("Sent DELAY_REQUEST packet #" + delay_id + " at time " + delay_request_time);
                        
                        // Réception de DELAY_RESPONSE
                        buffer = new byte[PTP_Shared.TIME_MESSAGE_SIZE];
                        packet = new DatagramPacket(buffer, buffer.length);
                        delay_socket.receive(packet);
                        
                        // Si on a pas reçu un DELAY_RESPONSE, on recommence:
                        if (PTP_Shared.getMessageType(packet.getData()) != PTP_Shared.DELAY_RESPONSE)
                            continue;
                            
                        // Si la réponse n'a pas le bon id, on recommence:
                        if (PTP_Shared.getMessageID(packet.getData()) != delay_id)
                            continue;
                            
                        // On extrait les données du packet et on fait les calculs
                        System.out.println("Received DELAY_RESPONSE packet #" + delay_id);
                        delay_response_time = PTP_Shared.getMessageTime(packet.getData());
                        delay = (delay_response_time - delay_request_time) / 2;
                        System.out.println("Calculated new delay of " + delay + "ms");
                        
                        // On affiche "l'heure"
                        time = System.currentTimeMillis();
                        long adj = time + offset + delay;
                        System.out.println("-- Current time is " + time + " before adjusting");
                        System.out.println("-- Current time is " + adj + " after adjusting");
                    }
                    
                    multi_socket.close();
                    delay_socket.close();
                    
                } catch (Exception e) {
                    System.out.println("ERROR IN CLIENT THREAD");
                    return;
                }
            }
        };
        
        // On démarre le thread
        client.start();
    }
}