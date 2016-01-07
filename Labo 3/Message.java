import java.nio.ByteBuffer;
import java.util.*;

/*
 * Eléonore d'Agostino et Karim Ghozlani
 *
 * Classe abstraite dont héritent tout les messages, et qui contient des
 * constantes utilisées par ceux-ci.
 */
public abstract class Message {
    // Les trois types de messages
    public static final byte ANNONCE = 0;
    public static final byte RESULT = 1;
    public static final byte RECEIPT = 2;
    
    // La taille maximale d'un message, correspondant à
    // un message de type ANNONCE contenant 4 aptitudes
    public static final int MAX_SIZE = 2 + 5 * Config.SITE_COUNT;
    
    // Converti un message en tableau de bytes, pour envoi par paquet
    public abstract byte[] toBytes();
    
    // Retourne le type du message
    public static byte getType(byte[] data) { return data[0]; }
}