import java.rmi.*;

/*
 * Eléonore d'Agostino et Karim Ghozlani
 *
 * Interface utilisée par les sites (gestionnaires), nécessaire à l'utilisation de RMI
 * Les détails des méthodes sont décrits dans la classe Site.
 */
interface ISite extends Remote {
    void sendMessage(String s) throws RemoteException;
    void receiveProbe(int sender, String message) throws RemoteException;
    void receiveEcho(int sender, long id) throws RemoteException;
}