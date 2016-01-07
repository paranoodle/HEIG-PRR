import java.io.*;
import java.net.*;
import java.lang.*;
import java.util.*;

/*
 * Eléonore d'Agostino et Karim Ghozlani
 *
 * Cette classe implémente la logique de l'élection en anneau.
 *
 * Le code est lancé avec en argument le nom du site, un entier entre 0 et 3.
 * Son port et son adresse doient être définis dans le fichier Config.java.
 *
 * Lorsque le programme tourne, il lance deux threads:
 *       1. Le premier correspond à la partie applicative et permet de lancer
 *          des élections.
 *       2. Le deuxième se charge de recevoir des messages de l'anneau et d'y
 *          répondre de manière appropriée.
 *
 * Sur le thread gestionnaire, on peut recevoir 2 types de messages:
 * - ANNONCE:
 *      Contient une liste de paires (site, aptitudes). 
 *      Si elle ne contient pas le site courant, on le lui ajoute, on envoie un
 *      message ANNONCE au prochain site de l'anneau, et on passe en mode
 *      ANNONCE si ce n'est pas déjà le cas.
 *      Si elle contient le site courant, on détermine l'élu à partir des
 *      aptitudes du messages, on passe en mode RESULT, et on envoie un message
 *      de type RESULT contenant le gagnant et le site courant au prochain site.
 * - RESULT:
 *      Contient l'élu et une liste de sites.
 *      Si elle contient le site courant, on a fini de traverser l'anneau et on
 *      connait l'élu. On peut donc s'arrêter.
 *      Si elle ne contient pas le site courant, qu'on est en mode RESULT,
 *      et que l'élu ne correspond pas à celui qu'on a en interne, rien ne va
 *      plus et on recommence une éléction.
 *      Si on n'est pas en mode RESULT, c'est qu'on vient de recevoir le nouvel
 *      élu. On passe en mode RESULT, on s'ajoute à la liste de sites, on envoie
 *      un message RESULT au prochain site, et on s'arrête.
 *
 * On utilise un troisième type de message, RECEIPT, pour confirmer la récéption
 * d'un message ANNONCE ou RESULT. A chaque récéption d'un message ANNONCE ou
 * RESULT, on envoie au site précédent un message RECEIPT, mais seulement après
 * avoir traité le message original et avoir envoyé les messages ANNONCE ou
 * RESULT adéquats.
 * Si un message RECEIPT n'est pas reçu dans les délais (définis dans Config),
 * on compte le site ciblé comme "en panne" et on renvoie le message original
 * au prochain site de l'anneau.
 */
public class Site {
    // Nom du site (en byte pour simplifier les messages)
    private byte name;
    // Offset entre ce site et le prochain site de l'anneau
    // Utilisé lors de pannes
    private byte next = 1;
    // Phase RESULT si true, ANNONCE si false
    private boolean resultPhase = false;
    // Nom du gagnant, -1 tant qu'il n'y en a pas
    private byte winner = -1;
    
    // Socket utilisé pour la récéption de messages ANNONCE et RESULT
    private DatagramSocket socket;
    // SiteConfig correspondant à ce site
    private SiteConfig config;
    
    // Thread gérant la récéption de messages ANNONCE et RESULT
    private Thread electionThread;
    // Thread gérant la partie applicative
    private Thread appThread;
    // Si true nous sommes en élection, si false non
    private boolean election = false;
    
    public Site(byte name) {
        // On obtient les informations du site depuis Config
        config = Config.SITE_MAP[name];
        try {
            socket = new DatagramSocket(config.port);
        } catch (Exception e) {
            System.out.println("Failed to create socket");
            e.printStackTrace();
            System.exit(-1);
        }
        this.name = name;
        
        System.out.println("New site: " + name);
        
        // Thread qui reçoit les messages ANNONCE et RESULT et les traites
        electionThread = new Thread() {
            public void run() {
                try{
                    while (true) {
                        byte[] buffer = new byte[Message.MAX_SIZE];
                        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                        
                        // On est en attente d'un message
                        socket.receive(packet);
                        
                        switch (Message.getType(packet.getData())) {
                            case Message.ANNONCE:
                                receiveAnnonce(packet);
                                break;
                            case Message.RESULT:
                                receiveResult(packet);
                                break;
                            default:
                                break;
                        }
                    }
                } catch (Exception e) {
                    System.out.println("EXCEPTION IN ELECTION THREAD");
                    e.printStackTrace();
                }
            }
        };
        
        appThread = new Thread(new Application(this));
        
        electionThread.start();
        appThread.start();
    }
    
    // Méthode appelée depuis l'application pour obtenir l'élu
    public byte getWinner() {
        if (resultPhase && winner != -1) {
            // L'élection précédente s'est terminée et on a un élu valide
            System.out.println("Keeping previous winner");
            return winner;
        } else {
            try {
                // On démarre une nouvelle élection
                startElection();
            
                // On utilise synchronized pour pouvoir réveiller
                // appThread lorsqu'on a trouvé l'élu
                synchronized(appThread) {
                    appThread.wait(Config.ELECTION_TIMEOUT);
                    System.out.println("!!!!! WAKEY WAKEY !!!!!!!!");
                    if (election) {
                        // Si une élection est en cours mais qu'appThread
                        // s'est réveillé, on a trop attendu
                        System.out.println("Election timed out");
                        throw new Exception(">:(");
                    } else {
                        // appThread a été réveillé à temps
                        System.out.println("Obtained winner!");
                        // On rappelle getWinner pour vérifier que l'élu est valide
                        return getWinner();
                    }
                }
            } catch (Exception e) {
                System.out.println("Unable to obtain winner");
                e.printStackTrace();
                return -1;
            }
        }
    }
    
    // (Re)démarre une élection
    private void startElection() throws Exception {
        System.out.println("Starting election...");
        
        election = true;
        resultPhase = false;
        winner = -1;
        next = 1;
        
        Map apt = new HashMap<>();
        apt.put(name, aptitude());
        
        send(new AnnonceMessage(apt));
    }
    
    // Finalize une élection
    private void endElection() {
        synchronized(appThread) {
            System.out.println("Election over!");
            election = false;
            
            // On réveille l'application si elle est en attente
            appThread.notify();
        }
    }
    
    // Gère la récéption de messages ANNONCE
    private void receiveAnnonce(DatagramPacket packet) throws Exception {
        AnnonceMessage message = new AnnonceMessage(packet.getData());
        System.out.println("Received message: " + message);
        
        Map<Byte,Integer> apt = message.getAptitudes();
        
        if (apt.containsKey(name)) {
            for (Map.Entry e: apt.entrySet()) {
                if (((int) e.getValue()) > apt.get(winner)) {
                    winner = (byte) e.getKey();
                }
            }
            
            List sites = new ArrayList<>();
            sites.add(name);
            
            resultPhase = true;
            send(new ResultMessage(winner, sites),
                    packet.getAddress(), packet.getPort());
        } else {
            apt.put(name, aptitude());
            resultPhase = false;
            
            send(new AnnonceMessage(apt), packet.getAddress(), packet.getPort());
        }
    }
    
    // Gère la réception de messages RESULT
    private void receiveResult(DatagramPacket packet) throws Exception {
        ResultMessage message = new ResultMessage(packet.getData());
        System.out.println("Received message: " + message);
        
        List sites = message.getSites();
                                
        if (sites.contains(name)) {
            sendReceipt(packet.getAddress(), packet.getPort());
            endElection();
        } else if (resultPhase && winner != message.getWinner()) {
            sendReceipt(packet.getAddress(), packet.getPort());
            startElection();
        } else if (!resultPhase) {
            winner = message.getWinner();
            sites.add(name);
            
            resultPhase = true;
            send(new ResultMessage(winner, sites),
                    packet.getAddress(), packet.getPort());
        } else {
            endElection();
        }
    }
    
    // Envoie un message au prochain site sans envoyer de reçu au site précedent
    // Utilisé lors du démarrage d'élections
    private void send(Message message) throws Exception {
        send(message, null, 0);
    }
    
    // Envoie un message au prochain site, envoie un reçu au site précedent,
    // puis attends un reçu. Si le reçu n'arrive pas à temps, réessaye avec
    // le sur-prochain site
    private void send(Message message, InetAddress ia, int port) throws Exception {
        // Prochain site
        byte nextID = (byte)((name + next) % Config.SITE_COUNT);
        SiteConfig nextSite = Config.SITE_MAP[nextID];
        System.out.println("Sending message to " + nextID + ": " + message);
        
        // Socket utilisé pour l'envoi du message et la récéption du reçu
        DatagramSocket receiptSocket = new DatagramSocket();
        
        byte[] buffer = message.toBytes();
        receiptSocket.send(new DatagramPacket(buffer, buffer.length,
                nextSite.address, nextSite.port));
        
        if (ia != null) {
            // On envoie un reçu
            sendReceipt(ia, port);
        }
            
        buffer = new byte[ReceiptMessage.RECEIPT_SIZE];
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
        
        try {
            // On pose une limite de temps sur la réception du reçu
            receiptSocket.setSoTimeout(Config.RECEIPT_TIMEOUT);
            receiptSocket.receive(packet);
        } catch (SocketTimeoutException e) {
            // Le reçu n'est pas arrivé à temps
            System.out.println("Switching to next site");
            // On change qui est le prochain site dans l'anneau
            next = (byte)((next + 1) % Config.SITE_COUNT);
            if (next == 0) {
                System.out.println("Looped around ring");
                next = 1;
            }
            
            send(message);
        }
        
        if (Message.getType(packet.getData()) == Message.RECEIPT) {
            ReceiptMessage rm = new ReceiptMessage(packet.getData());
            // On vérifie que le reçu vient du bon site
            if (rm.getSender() == nextID) {
                System.out.println("Received receipt from " + rm.getSender());
                receiptSocket.close();
            }
        }
    }
    
    // Envoie un reçu à l'addresse et au port en entrée
    private void sendReceipt(InetAddress ia, int port) throws Exception {
        ReceiptMessage message = new ReceiptMessage(name);
        
        byte[] buffer = message.toBytes();
        socket.send(new DatagramPacket(buffer, buffer.length, ia, port));
    }
    
    // Retourne l'aptitude du site
    public int aptitude() {
        return config.address.getAddress()[3] + config.port;
    }
    
    public static void main(String[] args) {
        // On cherche le nom du site en entrée
        if (args.length < 1) {
            System.out.println("Please enter site name: 0-3");
            System.exit(-1);
        }
        
        Site site = new Site(Byte.parseByte(args[0]));
    }
}

// Classe correspondand au thread applicatif
class Application implements Runnable {
    // Site à contacter pour obtention de l'élu
    private Site site;
    
    public Application(Site s) {
        site = s;
    }
    
    public void run() {
        BufferedReader input = new BufferedReader(new InputStreamReader(System.in));
                
        while (true) {
            try {
                System.out.println("Press enter to display winner");
                input.readLine();
                
                System.out.println("APP THREAD: The winner is site " + site.getWinner());
            } catch (Exception e) {
                System.out.println("EXCEPTION IN APP THREAD");
                e.printStackTrace();
            }
        }
    }
}
