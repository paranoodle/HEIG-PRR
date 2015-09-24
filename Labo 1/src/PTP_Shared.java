import java.nio.ByteBuffer;

public class PTP_Shared {
    public static final byte SYNC = 0;
    public static final byte FOLLOW_UP = 1;
    public static final byte DELAY_REQUEST = 2;
    public static final byte DELAY_RESPONSE = 3;
    
    public static final int MESSAGE_SIZE = 5;
    public static final int TIME_MESSAGE_SIZE = 13;
    
    public static final int SERVER_SOCKET = 4446;
    public static final int CLIENT_SOCKET = 4445;
    
    public static byte[] makeMessage(byte type, int id) {
        ByteBuffer bb = ByteBuffer.allocate(MESSAGE_SIZE);
        bb.put(type);
        bb.putInt(id);
        return bb.array();
    }
    
    public static byte[] makeTimeMessage(byte type, int id, long time) {
        ByteBuffer bb = ByteBuffer.allocate(TIME_MESSAGE_SIZE);
        bb.put(type);
        bb.putInt(id);
        bb.putLong(time);
        return bb.array();
    }
    
    public static byte getMessageType(byte[] array) {
        return array[0];
    }
    
    public static int getMessageID(byte[] array) {
        return ByteBuffer.wrap(array).getInt(1);
    }
    
    public static long getMessageTime(byte[] array) {
        return ByteBuffer.wrap(array).getLong(5);
    }
}