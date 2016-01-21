import java.rmi.*;

/*
 * Eléonore d'Agostino et Karim Ghozlani
 *
 * Interface utilisée par les applications, nécessaire à l'utilisation de RMI
 * Les détails des méthodes sont décrits dans la classe Application.
 */
public interface IApplication extends Remote {
    void receiveMessage(String message) throws RemoteException;
}