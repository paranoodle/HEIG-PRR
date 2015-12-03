import java.nio.ByteBuffer;

/*
* Structure d'un message SITE_REQUEST:
*       type SITE_REQUEST (byte), estampille logique (int), nom du site (int)
*
* Structure d'un message SITE_REPLY:
*       type SITE_REPLY (byte), estampille logique (int),
*       nom dur site (int), valeur courante de la variable partagée (int)
*
* Structure d'un message READ_REQUEST:
*       type READ_REQUEST (byte)
*
* Structure d'un message READ_REPLY:
*       type READ_REPLY (byte), valeur courante de la variable partagée (int)
*
* Structure d'un message WRITE_REQUEST:
*       type WRITE_REQUEST (byte), nouvelle valeur pour la variable partagée (int)
*
* Structure d'un message WRITE_REPLY:
*       type WRITE_REPLY (byte), valeur courante de la variable partagée pour vérification (int)
*/

public class Shared {
    // les deux types de messages pour les sites
    public static final byte SITE_REQUEST = 0;
    public static final byte SITE_REPLY = 1;
    
    // les quatres types de messages pour les applications
    public static final byte READ_REQUEST = 2;
    public static final byte READ_REPLY = 3;
    public static final byte WRITE_REQUEST = 4;
    public static final byte WRITE_REPLY = 5;
    
    // type + time + name =
    // byte + int +  int  = 9 bytes
    public static final int SITE_REQUEST_SIZE = 9;
    // type + time + name + value =
    // byte + int +  int  + int   = 13 bytes
    public static final int SITE_REPLY_SIZE = 13;
    // type + value =
    // byte + int   = 5 bytes
    public static final int APP_MESSAGE_SIZE = 5;
    
    // Génère un byte[] contenant un message SITE_REQUEST
    public static byte[] makeSiteRequest(int time, int name) {
        ByteBuffer bb = ByteBuffer.allocate(SITE_REQUEST_SIZE);
        bb.put(SITE_REQUEST);
        bb.putInt(time);
        bb.putInt(name);
        return bb.array();
    }
    
    // Génère un byte[] contenant un message SITE_REPLY
    public static byte[] makeSiteReply(int time, int name, int value) {
        ByteBuffer bb = ByteBuffer.allocate(SITE_REPLY_SIZE);
        bb.put(SITE_REPLY);
        bb.putInt(time);
        bb.putInt(name);
        bb.putInt(value);
        return bb.array();
    }
    
    // Génère un byte[] contenant la variable partagée, pour utilisation avec les messages
    // READ_REPLY, WRITE_REQUEST et WRITE_REPLY
    public static byte[] makeAppMessage(byte type, int value) {
        ByteBuffer bb = ByteBuffer.allocate(APP_MESSAGE_SIZE);
        bb.put(type);
        bb.putInt(value);
        return bb.array();
    }
    
    // Obtient le type d'un message depuis son tableau de bytes
    public static byte getMessageType(byte[] array) {
        return array[0];
    }
    
    // Obtient l'estampille logique d'un message depuis son tableau de bytes,
    // ou -1 si on tente d'ouvrir le mauvais type de message
    public static int getMessageTime(byte[] array) {
        if (array[0] == SITE_REQUEST || array[0] == SITE_REPLY) {
            return ByteBuffer.wrap(array).getInt(1);
        } else {
            System.out.println("Error: message does not contain timestamp");
            return -1;
        }
    }
    
    // Obtient le nom du site ayant envoyé le message, depuis son tableau de bytes,
    // ou -1 si on tente d'ouvrir le mauvais type de message
    public static int getMessageSender(byte[] array) {
        if (array[0] == SITE_REQUEST || array[0] == SITE_REPLY) {
            return ByteBuffer.wrap(array).getInt(1 + 4);
        } else {
            System.out.println("Error: message does not contain sender");
            return -1;
        }
    }
    
    // Obtient la valeur de la variable partagée d'un message, depuis son tableau de bytes,
    // ou -1 si on tente d'ouvrir le mauvais type de message
    public static int getMessageValue(byte[] array) {
        if (array[0] == SITE_REPLY) {
            return ByteBuffer.wrap(array).getInt(1 + 4 + 4);
        } else if (array[0] >= READ_REPLY && array[0] <= WRITE_REPLY) {
            return ByteBuffer.wrap(array).getInt(1);
        } else {
            System.out.println("Error: Message does not contain value");
            return -1;
        }
    }
}