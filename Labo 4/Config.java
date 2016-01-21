import java.util.*;

/*
 * Eléonore d'Agostino et Karim Ghozlani
 *
 * Contient les informations sur la topologie du réseau, ainsi que les
 * constantes partagées par les sites et les applications
 */
public class Config {
    // Port utilisé par le registre RMI
    public static final int REGISTRY_PORT = 1099;
    
    // Nombre de sites du réseau
    public static final int SITE_COUNT = 4;
    // Taille maximale d'un message
    public static final int MAX_MESSAGE_LENGTH = 256;
    
    // Topologie du réseau
    public static final int[][] TOPOLOGY = new int[][] {
        {1, 2, 3},
        {0, 2},
        {0, 1, 3},
        {0, 2}
    };
    
    // Addresse RMI d'un site suivant son nom
    public static String getSite(int i) { return "rmi://localhost/site" + i; }
    
    // Addresse RMI d'une application suivant son nom
    public static String getApp(int i) { return "rmi://localhost/app" + i; }
}