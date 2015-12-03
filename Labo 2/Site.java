import java.io.*;
import java.net.*;
import java.lang.*;
import java.util.*;

/*
* Eléonore d'Agostino et Karim Ghozlani
*
* Cette classe symbolise un site pour gérer la cohérence d'une variable partagée,
* dans notre cas un entier.
*
* Le code est lancé avec en arguments les IP et ports des autres sites, sous la forme
* IP:PORT (p.ex: 228.5.6.7:4444) ou juste PORT (p.ex: 4444) si on travaille sur localhost
* Exemples: java Site 4445 4446 4447
*           java Site 192.164.0.1:4445
*
* Une fois le programme lancé, il sera demandé quel port et IP utiliser pour ce site-ci,
* avec le même format que précédemment. Si on travaille sur localhost, seul le port est necéssaire.
* Ceci est fait pour éviter les conflits de ports si plusieurs sites sont sur une même machine.
*
* Lorsque le programme tourne, on peut recevoir 4 types de messages:
*
* Sur le thread acceptant les paquets externes:
* - SITE_REQUEST (reçu d'un autre site):
*       Contient une estampille logique et le nom du site émetteur, et
*       correspond à une demande d'accès à la section critique. A sa réception,
*       on renvoie un message SITE_REPLY si on ne cherche pas à entrer en section critique,
*       ou si l'estampille est plus vieille que celle de notre demande.
* - SITE_REPLY (reçu d'un autre site):
*       Contient une estampille logique, le nom du site émetteur, et la valeur courante
*       de la variable partagée sur ce site. A sa récéption, on met à jour la valeur de la
*       variable courante si son estampille est plus récente que la dernière mise à jour de
*       la variable courante.
*       Puis, si on est en attente de la section critique, on
*       décrémente le nombre de site dont on attend encore une réponse.
*       Si on a reçu une réponse de tous les sites, on entre en section critique et on
*       modifie la variable partagée, puis on envoie un message SITE_REPLY à tous les sites
*       pour les tenir au courant de la nouvelle valeur et du fait que la section critique
*       est maintenant libre.
*
* Sur le thread acceptant les paquets locaux:
* - READ_REQUEST (reçu d'une application):
*       Message vide autre que son entête, qui demande simplement la valeur courante de la
*       variable partagée. On revoie un message READ_REPLY à l'application, contenant la
*       valeur de la variable partagée.
* - WRITE_REQUEST (reçu d'une application):
*       Message contenant un entier, qui demande à modifier la variable partagée. On stocke
*       cette nouvelle valeur de côté ainsi que le port de l'application (pour lui dire quand
*       on a modifié la variable), puis on envoie des messages SITE_REQUEST à tous les sites
*       pour tenter d'entrer en section critique. Une fois en section critique, on modifie
*       la variable, puis on envoie un message WRITE_REPLY à l'application ayant demandé
*       la modification.
*
* Si les outputs console commencent par un nombre, ceci correspond au temps courant
* de l'horloge logique du site.
*/
public class Site {
    // nombre de sites partageant la donnée
    public final int N;
    
    // identifiant du site
    private int name;
    // horloge logique du site
    private int time = 0;
    // valeur courante de la variable partagée
    private int sharedVar = 0;
    // dernière mise à jour de la variable partagée
    private int varTime = 0;
    
    // liste des IP et ports des sites partageant la donnée
    private InetAddress[] neighborIPs;
    private int[] neighborPorts;
    
    // socket pour l'envoi et la récéption des paquets entre les sites
    private DatagramSocket socket;
    // socket pour l'envoi et la récéption des paquets avec l'application
    private DatagramSocket localSocket;
    
    // sommes-nous en attente de la section critique: oui/non
    private boolean csDemand = false;
    // moment où la demande d'accès à la section critique a été faite
    private int csTime = 0;
    // nombre de sites dont on attend encore les messages REPLY
    private int waitingFor = 0;
    
    // nouvelle valeur de la variable qui remplacera la vieille quand on entre en section critique
    private int newVar;
    // port de l'application qui a demandé l'entrée en section critique
    private int newPort;
    
    // Thread sur lequel tourne la récéption de messages d'autres sites
    private Thread siteThread;
    // Thread sur lequel tourne la récéption de messages des applications
    private Thread appThread;
    private boolean end = false;
    
    /* 
    * Constructeur du site, nécéssite un identifiant, le socket pour communiquer avec
    * les autres sites, le socket pour communiquer avec les applications,
    * ainsi que la liste des IP et des ports des autres sites partageant la donnée.
    * Démarre les deux threads de récéption de messages.
    */
    public Site(int name, DatagramSocket socket, DatagramSocket local, InetAddress[] sites, int[] ports) {
        this.name = name;
        N = sites.length;
        
        this.socket = socket;
        localSocket = local;
                
        neighborIPs = sites;
        neighborPorts = ports;
        
        System.out.println("Initialized new site\nName: " + name);
        
        // Thread qui reçoit les messages et les traites
        siteThread = new Thread() {
            public void run() {
                try {
                    while (!end) {
                        byte[] buffer = new byte[Shared.SITE_REPLY_SIZE];
                        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                        
                        // On est en attente d'un message
                        socket.receive(packet);
                        
                        int port = packet.getPort();
                        
                        System.out.println(time + ": Message received from site " + packet.getAddress() + ":" + port);
                        
                        byte type = Shared.getMessageType(packet.getData());
                        if (type == Shared.SITE_REQUEST) {
                            receiveRequest(packet);
                        } else if (type == Shared.SITE_REPLY) {
                            receiveReply(packet);
                        }
                    }
                    socket.close();
                } catch (Exception e) {
                    System.out.println("ERROR IN SITE THREAD");
                    e.printStackTrace();
                    System.exit(-1);
                }
            }
        };
        
        appThread = new Thread() {
            public void run() {
                try {
                    while (!end) {
                        byte[] buffer = new byte[Shared.APP_MESSAGE_SIZE];
                        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                        
                        // On est en attente d'un message
                        localSocket.receive(packet);
                        int port = packet.getPort();
                        
                        System.out.println(time + ": Message received from application at port " + port);
                        
                        byte type = Shared.getMessageType(packet.getData());
                        if (type == Shared.READ_REQUEST) {
                            // Si on reçoit une requête de lecture, on renvoie la valeur courante
                            System.out.println("-- Sending current shared value to application: " + sharedVar);
                            localSocket.send(new DatagramPacket(Shared.makeAppMessage(Shared.READ_REPLY, sharedVar),
                                        Shared.APP_MESSAGE_SIZE, InetAddress.getLocalHost(), port));
                        } else if (type == Shared.WRITE_REQUEST) {
                            // Si on reçoit une requête d'écriture, on notifie les autres sites
                            if (!csDemand) {
                                System.out.println("-- Acquiring critical section access for application");
                                newVar = Shared.getMessageValue(packet.getData());
                                newPort = port;
                                sendRequests();
                            } else {
                                System.out.println("Critical section has already been requested by another application on this site");
                            }
                        }
                    }
                    localSocket.close();
                } catch (Exception e) {
                    System.out.println("ERROR IN APP THREAD");
                    e.printStackTrace();
                    System.exit(-1);
                }
            }
        };
        
        siteThread.start();
        appThread.start();
    }
    
    // Méthode qui gère la récéption des messages REQUEST
    public void receiveRequest(DatagramPacket packet) {
        byte[] request = packet.getData();
        int sender = Shared.getMessageSender(request);
        
        if (sender != name) {
            System.out.println(time + ": REQUEST received from site " + sender);
            
            int ts = Shared.getMessageTime(request);
            time = Math.max(time, ts) + 1;
            
            if (csDemand && (ts > csTime || (ts == csTime && sender > name))) {
                System.out.println("-- Waiting for CS exit before sending reply");
            } else {
                sendReply(packet.getAddress(), packet.getPort());
            }
        }
    }
    
    // Méthode qui gère la récéption des messages REPLY
    // ainsi que l'accès à la section critique
    public void receiveReply(DatagramPacket packet) {
        byte[] reply = packet.getData();
        int sender = Shared.getMessageSender(reply);
        
        if (sender != name) {
            System.out.println(time + ": REPLY received from site " + sender);
            int messageTime = Shared.getMessageTime(reply);
            
            // Si le message est plus récent, on update la valeur de la variable partagée
            if (varTime < messageTime) {
                sharedVar = Shared.getMessageValue(packet.getData());
                varTime = messageTime;
                System.out.println(time + ": Shared variable updated from REPLY: " + sharedVar);
            }
            
            time = Math.max(time, messageTime) + 1;
            
            // Si on a reçu la permission de tous les autres sites, on entre en section critique
            if (csDemand && --waitingFor == 0) {
                System.out.println(time + ": Entering critical section, " + 
                                    "current shared variable value: " + sharedVar);
                
                // On modifie la valeur de la variable partagée et on met à jour l'heure de sa modification
                sharedVar = newVar;
                varTime = time;
                
                try {
                    // On renvoie un message WRITE_REPLY à l'application qui avait demandé l'accès
                    // à la section critique
                    localSocket.send(new DatagramPacket(Shared.makeAppMessage(Shared.WRITE_REPLY, sharedVar),
                                Shared.APP_MESSAGE_SIZE, InetAddress.getLocalHost(), newPort));
                } catch (Exception e) {
                    System.out.println("Error: failed to send updated value to application");
                }
                
                csDemand = false;
                System.out.println(time + ": Exiting critical section, " +
                                    "new shared variable value: " + sharedVar);
                
                // On envoie des messages SITE_REPLY à tous les sites pour leur informer de notre sortie
                // de la section critique
                for (int i=0; i < N; i++) {
                    sendReply(neighborIPs[i], neighborPorts[i]);
                }
            }
        }
    }
    
    // Méthode qui gère l'envoi des messages REQUEST
    public void sendRequests() {
        try {
            csDemand = true;
            time++;
            csTime = time;
            waitingFor = N;
            
            for (int i=0; i < N; i++) {
                socket.send(new DatagramPacket(Shared.makeSiteRequest(csTime, name),
                        Shared.SITE_REQUEST_SIZE, neighborIPs[i], neighborPorts[i]));
                System.out.println(time + ": REQUEST sent to " + neighborIPs[i] + ":" + neighborPorts[i]);
            }
            
            System.out.println(time + ": All REQUESTs sent");
        } catch (Exception e) {
            System.out.println("ERROR ON SENDING REQUEST");
            e.printStackTrace();
        }
    }
    
    // Méthode qui gère l'envoi des messages REPLY
    public void sendReply(InetAddress receiver, int port) {
        try {            
            DatagramPacket packet = new DatagramPacket(Shared.makeSiteReply(time, name, sharedVar),
                        Shared.SITE_REPLY_SIZE, receiver, port);
            
            socket.send(packet);
            System.out.println(time + ": REPLY sent");
        } catch (Exception e) {
            System.out.println("ERROR ON SENDING REPLY");
            e.printStackTrace();
        }
    }
    
    public static void main(String[] args) {
        // On cherche la liste des IP en entrée
        if (args.length < 1) {
            System.out.println("Please enter IP addresses and ports of other sites, or ports only if working locally");
            System.exit(-1);
        }
        
        InetAddress[] sites = new InetAddress[args.length];
        int[] ports = new int[args.length];
        
        try {
            BufferedReader input = new BufferedReader(new InputStreamReader(System.in));
            System.out.println("Please enter site IP and port for site communication, or only port if working locally:");
            String s = input.readLine();
            
            DatagramSocket socket;
            
            if (s.contains(".")) {
                System.out.println(s);
                String[] split = s.split(":");
                socket = new DatagramSocket(null);
                InetSocketAddress address = new InetSocketAddress(InetAddress.getByName(split[0]),
                                                Integer.parseInt(split[1]));
                if (address.isUnresolved()) {
                    System.out.println("Error: cannot resolve address");
                    return;
                } else {
                    socket.bind(address);
                }
            } else {
                socket = new DatagramSocket(Integer.parseInt(s));
            }
            
            System.out.println("Please enter site socket for communication with applications");
            s = input.readLine();
            
            DatagramSocket local = new DatagramSocket(Integer.parseInt(s));
            
            for (int i=0; i < args.length; i++) {
                if (args[i].contains(".")) {
                    // on a une address IP, suivie d'un port
                    String[] split = args[i].split(":");
                    sites[i] = InetAddress.getByName(split[0]);
                    ports[i] = Integer.parseInt(split[1]);
                } else {
                    // on n'a que le port, donc on travaille sur localhost
                    sites[i] = InetAddress.getLocalHost();
                    ports[i] = Integer.parseInt(args[i]);
                }
            }
            
            // On initialise le site avec un nom aléatoire, les sockets pour communiquer avec les sites
            // et les applications, puis la liste des sites et des ports qui partageant la donnée
            Site site = new Site((int)(Math.random() * Integer.MAX_VALUE), socket, local, sites, ports);
            
        } catch (Exception e) {
            System.out.println("Failed to read site IPs");
            e.printStackTrace();
        }
    }
}