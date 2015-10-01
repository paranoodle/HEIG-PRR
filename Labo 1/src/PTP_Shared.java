import java.nio.ByteBuffer;

/**
 * Classe utilitaire partagée.
 * Elle contient des constantes ainsi que des méthodes pour créer et
 * extraire des données des messages.
 */
public class PTP_Shared {
    
    // Codes des différents types de messages
    public static final byte SYNC = 0;
    public static final byte FOLLOW_UP = 1;
    public static final byte DELAY_REQUEST = 2;
    public static final byte DELAY_RESPONSE = 3;
    
    // Taille en bytes des messages
    public static final int MESSAGE_SIZE = 5;
    public static final int TIME_MESSAGE_SIZE = 13;
    
    // Ports utilisés par les sockets
    public static final int SERVER_SOCKET = 4446;
    public static final int CLIENT_SOCKET = 4445;
    
    /**
     * Fabrique un tableau de bytes contenant un type de message
     * ansi qu'un identifiant, prêt à l'envoi par datagram.
     */
    public static byte[] makeMessage(byte type, int id) {
        ByteBuffer bb = ByteBuffer.allocate(MESSAGE_SIZE);
        bb.put(type);
        bb.putInt(id);
        return bb.array();
    }
    
    /**
     * Fabrique un tableau de bytes contenant un type de message,
     * un identifiant, ansi qu'un timestamp, prêt à l'envoi par datagram.
     */
    public static byte[] makeTimeMessage(byte type, int id, long time) {
        ByteBuffer bb = ByteBuffer.allocate(TIME_MESSAGE_SIZE);
        bb.put(type);
        bb.putInt(id);
        bb.putLong(time);
        return bb.array();
    }
    
    /**
     * Obtient le type d'un message obtenu par datagram.
     */
    public static byte getMessageType(byte[] array) {
        return array[0];
    }
    
    /**
     * Obtient l'identifiant d'un message obtenu par datagram.
     */
    public static int getMessageID(byte[] array) {
        return ByteBuffer.wrap(array).getInt(1);
    }
    
    /**
     * Obtient le timestamp contenu dans un message obtenu par datagram.
     */
    public static long getMessageTime(byte[] array) {
        if (array.length == 13) return ByteBuffer.wrap(array).getLong(5);
        else {
            System.out.println("Error: Attempted to get message time when unavailable");
            return (long) 0;
        }
    }
}