import java.nio.ByteBuffer;
import java.util.*;

/*
 * Eléonore d'Agostino et Karim Ghozlani
 *
 * Classe décrivant un message de type RESULT.
 * Ce message est utilisé pour partage le nom du site élu par l'élection.
 *
 * Structure d'un message RESULT:
 *      type = RESULT (byte),
 *      gagnant (byte),
 *      de 1 à 4 fois {nom du site (byte)}
 *      -1 (byte) pour marquer la fin du message vu sa taille variable
 */
public class ResultMessage extends Message {
    private byte winner;
    private List<Byte> siteList;
    
    // Création du message à partir du contenu qu'on veut lui donner
    public ResultMessage(byte winner, List<Byte> sites) {
        this.winner = winner;
        siteList = sites;
    }
    
    // Création du message à partir d'un tableau de bytes, pour utilisation
    // lors de l'extraction d'un paquet
    public ResultMessage(byte[] data) {
        winner = data[1];
        
        siteList = new ArrayList<>();
        for (int i=2; data[i] != -1; i++) {
            siteList.add(data[i]);
        }
    }
    
    // Voir classe Message
    @Override
    public byte[] toBytes() {
        int size = siteList.size();
        ByteBuffer bb = ByteBuffer.allocate(1 + 1 + size + 1);
        
        bb.put(RESULT);
        bb.put(winner);
        for (byte site: siteList) { bb.put(site); }
        bb.put((byte)(-1));
        
        return bb.array();
    }
    
    public byte getWinner() { return winner; }
    
    public List getSites() { return siteList; }
    
    public String toString() {
        String s = "RESULT: " + winner + "\n  ";
        for (byte b : siteList) { s += b + " "; }
        return s;
    }
}