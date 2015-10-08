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
            System.exit(-1); // TODO
        }
        String ipAddress = args[0];
        byte[] buffer = ByteBuffer.allocate(PTP_Shared.MESSAGE_SIZE).array();
        long time;

        MulticastSocket socket = new MulticastSocket(PTP_Shared.MULTICAST_CLIENT_PORT);
        InetAddress group = InetAddress.getByName(ipAddress);
    //    socket.joinGroup(group);

        // On attend un SYNC
        DatagramPacket packet = new DatagramPacket(buffer,buffer.length);
        socket.receive(packet);// méthode bloquante !
        byte[] data = packet.getData();
        ByteBuffer bb = ByteBuffer.wrap(data);
        byte type = PTP_Shared.getMessageType(buffer);
        int id = PTP_Shared.getMessageID(buffer);
        System.out.println("Message of type " + type + " recieved, ID : " + id);
        // on récupère le temps où le sync a été reçu
        long ti = System.currentTimeMillis();

        // On attend le Follow up correspondant
        buffer = ByteBuffer.allocate(PTP_Shared.TIME_MESSAGE_SIZE).array();
        byte type_2 = PTP_Shared.getMessageType(buffer);
        int id_2 = PTP_Shared.getMessageID(buffer);
        packet = new DatagramPacket(buffer,buffer.length);
        socket.receive(packet);// méthode bloquante !
        //byte[] data_2 = packet.getData();
        System.out.println("Message of type " + type_2 + " recieved, ID : " + id_2);

        // on vérifie que l'id est bien le même, on récupère le temps
        if(id == id_2){
            // on calcule l'écart, et on envoie un message delay_request
            long timeOfMaster = PTP_Shared.getMessageTime(buffer);
            long timeOfSlave = System.currentTimeMillis();
            long delay = timeOfMaster - timeOfSlave;

        }
        Random r = new Random();
        int randomWaitingTime = ( Math.max(PTP_Shared.LOWERBOUND, r.nextInt(PTP_Shared.UPPERBOUND + 1) ) ) * PTP_Shared.MULTICAST_DELAY ; //random Time between 4k and 60k
        int delayRequestID = r.nextInt(1000);

        //l'esclave débute la seconde étape après un temps aléatoire tiré de l'intervalle [4k,60k]
        try{
            Thread.sleep(randomWaitingTime);
        } catch (Exception e) {
            System.out.println("ERROR WHILE SLAVE IS RANDOMLY WAITING");
            return;
        }
        byte[] delay_request = PTP_Shared.makeMessage(PTP_Shared.DELAY_REQUEST, delayRequestID);
        DatagramPacket packetToSend = new DatagramPacket(buffer, PTP_Shared.MESSAGE_SIZE, group, PTP_Shared.SERVER_PORT);

        //on attend le delay_response
        buffer = ByteBuffer.allocate(PTP_Shared.TIME_MESSAGE_SIZE).array();
        byte type_3 = PTP_Shared.getMessageType(buffer);
        int id_3 = PTP_Shared.getMessageID(buffer);
        packet = new DatagramPacket(buffer,buffer.length);
        socket.receive(packet);// méthode bloquante !
        System.out.println("Message of type " + type_3 + " recieved, ID : " + id_3);



        socket.leaveGroup(group);
        socket.close();
    }
}