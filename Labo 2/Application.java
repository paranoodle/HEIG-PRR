import java.lang.System;
import java.net.*;
import java.io.*;

/*
* Eléonore d'Agostino et Karim Ghozlani
*
* Cette classe correspond à une application qui voudrait pouvoir accéder à une
* variable partagée gérée par un des sites.
*
* Le programme est lancé avec en argument le port utilisé par l'application, suivi du
* port du site que l'on souhaite utiliser. On part du principe que tous les sites
* ont été lancés avant de lancer une des applications.
* Exemple: java Application 4444 4446
*
* Une fois le programme lancé, on peut utiliser deux instructions:
* - read:
*       L'application envoie une requête READ_REQUEST à son site associé, puis attend
*       un message READ_REPLY contenant la valeur courante de la variable partagée.
* - write:
*       L'application demande ensuite d'entrer une valeur entière. Ceci fait, elle
*       envoie une requête WRITE_REQUEST contenant cette nouvelle valeur à son site associé,
*       puis attend un message WRITE_REPLY qui contient à nouveau cette même valeur.
*
* N'importe quelle autre instruction sera ignorée, mais la casse n'est pas prise en compte.
*/
public class Application {
    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Please enter application port and local site port");
            System.exit(-1);
        }
        
        System.out.println("Please ensure site has been started on the given port before attempting any operations");
        int appPort = Integer.parseInt(args[0]);
        int sitePort = Integer.parseInt(args[1]);
        
        System.out.println("Instructions:\n-------------");
        System.out.println("read - view shared variable");
        System.out.println("write - modify shared variable");
        
        BufferedReader input = new BufferedReader(new InputStreamReader(System.in));
        
        try {
            DatagramSocket socket = new DatagramSocket(appPort);
        
            while (true) {
                String s = input.readLine().toLowerCase();
                
                if (s.equals("read")) {
                    byte[] buffer = {Shared.READ_REQUEST};
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length,
                                            InetAddress.getLocalHost(), sitePort);
                    
                    socket.send(packet);
                    System.out.println("Read request sent, waiting for reply...");
                    
                    buffer = new byte[Shared.APP_MESSAGE_SIZE];
                    packet = new DatagramPacket(buffer, buffer.length);
                    socket.receive(packet);
                    
                    byte[] data = packet.getData();
                    if (Shared.getMessageType(data) == Shared.READ_REPLY) {
                        System.out.println("Current shared variable value is: " + Shared.getMessageValue(data));
                    }
                } else if (s.equals("write")) {
                    System.out.println("Please insert new value for shared variable");
                    int value = Integer.parseInt(input.readLine());
                    DatagramPacket packet = new DatagramPacket(Shared.makeAppMessage(Shared.WRITE_REQUEST, value),
                                            Shared.APP_MESSAGE_SIZE, InetAddress.getLocalHost(), sitePort);
                    socket.send(packet);
                    System.out.println("Write request sent, waiting for critical section access...");
                    
                    byte[] buffer = new byte[Shared.APP_MESSAGE_SIZE];
                    packet = new DatagramPacket(buffer, buffer.length);
                    socket.receive(packet);
                    
                    byte[] data = packet.getData();
                    if (Shared.getMessageType(data) == Shared.WRITE_REPLY) {
                        int response = Shared.getMessageValue(data);
                        if (value == response) {
                            System.out.println("Shared variable was updated correctly");
                        } else {
                            System.out.println("Shared variable was not updated correctly");
                        }
                        System.out.println("New value is: " + response);
                    }
                } else {
                    System.out.println("Instruction not recognized");
                }
            }
        } catch (Exception e) {
            System.out.println("ERROR IN MAIN APPLICATION LOOP");
            e.printStackTrace();
        }
    }
}