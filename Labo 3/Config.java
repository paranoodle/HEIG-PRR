import java.util.*;
import java.net.*;

/*
 * Eléonore d'Agostino et Karim Ghozlani
 *
 * Cette classe contient les informations sur les sites, pour éviter de devoir
 * les entrer manuellement pour chaque VM, et pour plus facilement les partager.
 */
public class Config {
    // Nombre de sites de l'anneau
    public static final int SITE_COUNT = 4;
    
    // Temps d'attente maximal d'une confirmation
    public static final int RECEIPT_TIMEOUT = 100;
    // Temps d'attente maximal d'une élection
    public static final int ELECTION_TIMEOUT = 3000;
    
    // Tableau des sites de l'anneau
    // Les noms des sites correspondent aux indices de ce tableau
    public static final SiteConfig[] SITE_MAP = new SiteConfig[]{
        new SiteConfig(4444),
        new SiteConfig(4445),
        new SiteConfig(4446),
        new SiteConfig(4447)
    };
}