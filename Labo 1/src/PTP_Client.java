import java.io.IOException;
import java.net.*;
import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.nio.ByteBuffer;

public class PTP_Client {
    public static void main(String args[]) throws IOException{
        if(args.length < 1){
            System.out.println("IP Address of server is missing !");
            System.exit(-1); // TODO
        }
        String ipAddress = args[0];
        byte[] buffer = ByteBuffer.allocate(5).array();
        long time;

        DatagramSocket socket = new DatagramSocket();
        MulticastSocket socket = new MulticastSocket(4445);
        InetAddress group = InetAddress.getByAddress(ipAddress);
        socket.joinGroup(group);

        // On attend un SYNC
        DatagramPacket packet = new DatagramPacket(buffer,buffer.length);
        socket.receive(packet);// méthode bloquante !
        byte[] data = packet.getData();
        ByteBuffer bb = ByteBuffer.wrap(data);
        byte type = PTP_Shared.getMessageType(buffer);
        int id = PTP_Shared.getMessageID(buffer);
        System.out.println("Message of type " + type + "recieved, ID :" + id);


        // On attend le Follow up correspondant
        buffer = ByteBuffer.allocate(13).array();
        byte type_2 = PTP_Shared.getMessageType(buffer);
        int id_2 = PTP_Shared.getMessageID(buffer);
        DatagramPacket packet = new DatagramPacket(buffer,buffer.length);
        socket.receive(packet);// méthode bloquante !
        byte[] data_2 = packet.getData();
        System.out.println("Message of type " + type + "recieved, ID :" + id);

        // on vérifie que l'id est bien le même, on récupère le temps
        if(id == id_2){
            // on calcule l'écart, et on envoie un message delay_request
            long timeOfMaster = PTP_Shared.getMessageTime(buffer);
            long timeOfSlave = System.currentTimeMillis();
            long delay = timeOfMaster - timeOfSlave;
            byte[] delay_response = PTP_Shared.makeTimeMessage(3, id, delay);
            DatagramPacket packetToSend = new DatagramPacket(buffer, 13, group, 4446);
        }


        socket.leaveGroup(group);
        socket.close();
    }
}