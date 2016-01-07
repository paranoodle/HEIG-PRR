/*
 * Eléonore d'Agostino et Karim Ghozlani
 *
 * Classe décrivant un message de type RECEIPT.
 * Ce message est utilisé pour confirmer la récéption d'un des autres messages.
 *
 * Structure d'un message RECEIPT:
 *      type = RECEIPT (byte),
 *      nom du site (byte)
 */
public class ReceiptMessage extends Message {
    // Taille du message pour la récéption de paquets
    public static final int RECEIPT_SIZE = 2;
    
    // Nom du site envoyant le reçu, pour vérification
    private byte sender;
    
    // Création du message à partir du contenu qu'on veut lui donner
    public ReceiptMessage(byte site) {
        this.sender = sender;
    }
    
    // Création du message à partir d'un tableau de bytes, pour utilisation
    // lors de l'extraction d'un paquet
    public ReceiptMessage(byte[] data) {
        sender = data[1];
    }
    
    // Voir classe Message
    @Override
    public byte[] toBytes() {
        return new byte[]{RECEIPT, sender};
    }
    
    public byte getSender() { return sender; }
    
    public String toString() {
        return "RECEIPT: " + sender;
    }
}