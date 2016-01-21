import java.util.*;
import java.rmi.*;
import java.rmi.server.*;
import java.rmi.registry.*;

/*
 * Eléonore d'Agostino et Karim Ghozlani
 *
 * Implémentation des applications de messagerie.
 *
 * Elle communique avec un site pour envoyer et recevoir des messages.
 */
public class Application extends UnicastRemoteObject implements IApplication {
    public Application(int name) throws Exception {
        // On crée le registre s'il n'existe pas dejà
        try {
            LocateRegistry.createRegistry(Config.REGISTRY_PORT);
            System.out.println("Created new RMI registry");
        } catch (RemoteException e) {
            System.out.println("RMI registry already exists");
        }
        
        // On inscrit l'application au registre RMI
        Naming.rebind(Config.getApp(name), this);
    }
    
    // Reçois un message textuel et l'affiche
    public void receiveMessage(String message) throws RemoteException {
        System.out.println(message);
    }
    
    // Crée une application et contient la boucle principale
    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            System.out.println("Please launch with app name (0-" + (Config.SITE_COUNT - 1) + ")");
            System.exit(1);
        }
        
        int name = Integer.parseInt(args[0]);
        Application app = new Application(name);
        
        // On envoie au site associé les messages entrés dans le terminal
        ISite site = (ISite) Naming.lookup(Config.getSite(name));
        Scanner in = new Scanner(System.in);
        while (true) {
            site.sendMessage(in.nextLine());
        }
    }
}