import java.net.*;

/*
 * Eléonore d'Agostino et Karim Ghozlani
 *
 * Cette classe est utilisée pour définir de manière plus accessible
 * les ports et adresses IP des sites de l'anneau.
 */
class SiteConfig {
    public InetAddress address;
    public int port;
    
    // On laisse ceci pour donner la possibilité de plus facilement modifier
    // le code pour l'utilisation hors d'un réseau local.
    public SiteConfig(InetAddress ia, int i) {
        port = i;
        try {
            address = (ia == null) ? InetAddress.getLocalHost() : ia;
        } catch (Exception e) {
            address = null;
        }
    }
    
    public SiteConfig(int i) {
        this(null, i);
    }
}