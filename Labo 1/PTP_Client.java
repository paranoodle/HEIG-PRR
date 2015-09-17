import java.io.IOException;
import java.net.*;
import java.io.*;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;

public class PTP_Client {
    public static void main(String args[]) throws IOException{
        if(args.length < 1){
            System.out.println("IP Address of server is missing !");
            System.exit(-1); // TODO
        }
        String ipAddress = args[0];
        byte[] buffer = new byte[256];
        //
        MulticastSocket socket = new MulticastSocket(4445);
        InetAddress group = InetAddress.getByAddress(ipAddress);
        socket.joinGroup(group);

        //
        DatagramPacket packet = new DatagramPacket(buffer,buffer.length);
        socket.receive(packet);// méthode blocante !

        String recievedMessage = new String(packet.getData(),0);
        System.out.println("Message recieved: "+ recievedMessage);
        socket.leaveGroup(group);
        socket.close();
    }
}