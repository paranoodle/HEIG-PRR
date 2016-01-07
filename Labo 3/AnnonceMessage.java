import java.nio.ByteBuffer;
import java.util.*;

/*
 * Eléonore d'Agostino et Karim Ghozlani
 *
 * Classe décrivant un message de type ANNONCE.
 * Ce message est utilisé pour partager les aptitudes des sites pour pouvoir
 * les comparer.
 *
 * Structure d'un message ANNONCE:
 *      type ANNONCE (byte),
 *      de 1 à 4 fois {nom de site (byte), aptitude du site (int)}
 *      -1 (byte) pour marquer la fin du message vu sa taille variable
 */
public class AnnonceMessage extends Message {
    // Mapping (site, site.aptitude) des sites ayant déjà vu l'annonce
    private Map<Byte, Integer> aptitudes;
    
    // Création du message à partir du contenu qu'on veut lui donner
    public AnnonceMessage(Map map) {
        aptitudes = map;
    }
    
    // Création du message à partir d'un tableau de bytes, pour utilisation
    // lors de l'extraction d'un paquet
    public AnnonceMessage(byte[] data) {
        ByteBuffer bb = ByteBuffer.wrap(data);
        
        aptitudes = new HashMap<>();
        for (int i=1; data[i] != -1; i+=5) {
            aptitudes.put(data[i], bb.getInt(i+1));
        }
    }
    
    // Voir classe Message
    @Override
    public byte[] toBytes() {
        int size = (1 + 4) * aptitudes.size();
        ByteBuffer bb = ByteBuffer.allocate(1 + size + 1);
        
        bb.put(ANNONCE);
        for (Map.Entry e: aptitudes.entrySet()) {
            bb.put((byte) e.getKey());
            bb.putInt((int) e.getValue());
        }
        bb.put((byte)(-1));
        
        return bb.array();
    }
    
    public Map getAptitudes() { return aptitudes; }
    
    public String toString() {
        String s = "ANNONCE:";
        for (Map.Entry e : aptitudes.entrySet()) {
            s += "\n  " + e.getKey() + ":" + e.getValue();
        }
        return s;
    }
}