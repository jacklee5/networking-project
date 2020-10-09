import java.io.Serializable;
import java.util.ArrayList;

public class Message implements Serializable {
    public static final int HEADER_CLIENT_SEND_NAME = 0;
    public static final int HEADER_CLIENT_SEND_MESSAGE = 1;
    public static final int HEADER_CLIENT_SEND_PM = 2;
    public static final int HEADER_CLIENT_SEND_LOGOUT = 3;
    public static final int HEADER_CLIENT_SEND_NUKE = 4;
    public static final int HEADER_SERVER_REQ_NAME = 5;
    public static final int HEADER_SERVER_SEND_USERS = 6;
    public static final int HEADER_SERVER_SEND_MESSAGE = 7;
    public static final int HEADER_SERVER_SEND_WELCOME = 8;
    public static final int HEADER_SERVER_SEND_PM = 9;
    public static final int HEADER_SERVER_SEND_LEAVE = 10;

    public static final long serialVersionUID = 1L;

    private int header;
    private ArrayList<String> payload;

    public Message(int t_header, ArrayList<String> payload) {
        this.header = t_header;
        this.payload = payload;
    }

    public int getHeader() {
        return header;
    }

    public ArrayList<String> getPayload() {
        return payload;
    }

    public String toString() {
        String ret = header + ":\n";
        for (int i = 0; i < payload.size(); i++) {
            ret += payload.get(i) + "\n";
        }

        return ret;
    }
}
