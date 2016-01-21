import java.util.*;
import java.rmi.*;
import java.rmi.server.*;
import java.rmi.registry.*;

/*
 * Eléonore d'Agostino et Karim Ghozlani
 *
 * Implémentation des sites (gestionnaires) de messagerie.
 *
 * Elle implémente l'algorithme de diffusion de messages par sondes et échos.
 */
public class Site extends UnicastRemoteObject implements ISite {
    
    // Nom du site (0-4)
    private int name;
    // Tableau des voisins du site, obtenus depuis la classe Config
    private int[] neighbors;
    
    // Dernier ID envoyé
    private long currentId;
    // Map des ID connus du site
    private Map<Long,Integer> known_ids = new HashMap<>();
    
    public Site(int i) throws Exception {
        name = i;
        neighbors = Config.TOPOLOGY[i];
        currentId = name;
        
        // On crée le registre s'il n'existe pas dejà
        try {
            LocateRegistry.createRegistry(Config.REGISTRY_PORT);
            System.out.println("Created new RMI registry");
        } catch (RemoteException e) {
            System.out.println("RMI registry already exists");
        }
        
        // On inscrit le site au registre RMI
        Naming.rebind(Config.getSite(name), this);
    }
    
    // Envoie un message aux autres sites après lui avoir rajouté un ID unique.
    public void sendMessage(String s) throws RemoteException {
        // On incrémente currentId de 10 pour qu'il se termine toujours par
        // le numéro du site, pour que les ID soient tous uniques
        currentId += 10;
        String message = currentId +  ": " + s;
        System.out.println("Sending message...\n    " + message);
        
        if (message.length() > Config.MAX_MESSAGE_LENGTH) {
            // Le message est trop long, on ne l'envoie pas
            System.out.println("Error: Message too long");
            try {
                // On renvoie un message d'erreur à l'application
                IApplication app = (IApplication) Naming.lookup(Config.getApp(name));
                app.receiveMessage("Error: Could not send message; too long");
            } catch (Exception e) {
                System.out.println("Error sending error to app");
                e.printStackTrace();
            }
        } else {
            // On ajoute l'ID à la liste des ID connus
            known_ids.put(currentId, neighbors.length);
            
            // On envoie une sonde contenant le message à tous les sites voisins
            for (int n: neighbors) {
                try {
                    ISite site = (ISite) Naming.lookup(Config.getSite(n));
                    site.receiveProbe(name, message);
                    System.out.println("Sent message " + currentId + " to " + n);
                } catch (Exception e) {
                    System.out.println("Error sending probe to " + n);
                    e.printStackTrace();
                }
            }
        }
    }
    
    // Reçois une sonde contenant le nom du site émetteur et le message
    public void receiveProbe(int sender, String message) throws RemoteException {
        // On sépare l'ID du contenu du message
        int index = message.indexOf(':');
        long id = Long.parseLong(message.substring(0, index));
        System.out.println("Received probe from " + sender + " with id " + id);
        
        if (known_ids.containsKey(id)) {
            decrement(id);
        } else {
            // On ajoute l'ID à la liste des ID connus
            known_ids.put(id, neighbors.length - 1);
            
            // On envoie une sonde contenant le message à tous les sites
            // voisins, sauf le site émetteur
            for (int n: neighbors) {
                if (n != sender) {
                    try {
                        ISite site = (ISite) Naming.lookup(Config.getSite(n));
                        site.receiveProbe(name, message);
                        System.out.println("Sent message " + id + " to " + n);
                    } catch (Exception e) {
                        System.out.println("Error sending probe to " + n);
                        e.printStackTrace();
                    }
                }
            }
            
            // On envoie un écho au site émetteur
            try {
                ISite site = (ISite) Naming.lookup(Config.getSite(sender));
                site.receiveEcho(name, id);
                System.out.println("Sent echo " + id + " to " + sender);
            } catch (Exception e) {
                System.out.println("Error sending echo to " + sender);
                e.printStackTrace();
            }
            
            // On transmet le message à l'application
            try {
                IApplication app = (IApplication) Naming.lookup(Config.getApp(name));
                app.receiveMessage(message);
                System.out.println("Sent message " + id + " to app");
            } catch (Exception e) {
                System.out.println("Error sending message to app");
                e.printStackTrace();
            }
        }
    }
    
    // Reçois un écho contenant le site émetteur et l'ID du message
    public void receiveEcho(int sender, long id) throws RemoteException {
        System.out.println("Received echo from " + sender + " for " + id);
        decrement(id);
    }
    
    private void decrement(long id) {
        // On décrémente/enlève l'ID
        int count = known_ids.remove(id);
        if (count > 1) {
            known_ids.put(id, count - 1);
        }
    }
    
    // Crée un site
    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            System.out.println("Please launch with site name (0-" + (Config.SITE_COUNT - 1) + ")");
            System.exit(1);
        }
        
        int name = Integer.parseInt(args[0]);
        Site site = new Site(name);
    }
}