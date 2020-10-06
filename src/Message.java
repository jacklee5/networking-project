import java.io.Serializable;
import java.util.ArrayList;

public class Message implements Serializable{
    public static final long serialVersionUID = 1L;

    protected int header;
    protected ArrayList<String> payload;

    public Message(int t_header, String payload) {
        this.header = t_header;
    }

    public int getHeader() {
        return header;
    }

    public ArrayList<String> getPayload() {
        return payload;
    }
}
