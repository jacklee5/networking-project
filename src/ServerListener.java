import java.util.ArrayList;
import java.io.ObjectInputStream;

public class ServerListener implements Runnable {
    
    private boolean named;
    private String name;
    private ObjectInputStream in;

    public ServerListener (ObjectInputStream in, boolean named, String name) {
        this.in = in;
        this.named = named;
        this.name = name;
    }

    public boolean getNamed() {
        return named;
    }

    public void setNamed(boolean named) {
        this.named = named;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ObjectInputStream in() {
        return in;
    }

    public void setIn(ObjectInputStream in) {
        this.in = in;
    }

    @Override
    public void run() {
        try {
            Message incoming;

            while( (incoming = (Message)in.readObject()) != null) {
                //handle different headers
                int header = incoming.getHeader();
                ArrayList<String> arguments = incoming.getPayload();
                
                //SUBMITNAME
                if (header == Message.HEADER_SERVER_REQ_NAME) {
                    System.out.print("Enter your username: ");
                }
                //WELCOME
                else if (header == Message.HEADER_SERVER_SEND_WELCOME) {
                    String newName = arguments.get(0);
                    if (!named && newName.equals(name)) {
                        named = true;
                    }
                    System.out.println(newName + " has joined");
                }
                //CHAT
                else if (header == Message.HEADER_SERVER_SEND_MESSAGE) {
                    String username = arguments.get(0);
                    String msg = arguments.get(1);
                    System.out.println(username + ": "  + msg);
                }
                //PM
                else if (header == Message.HEADER_SERVER_SEND_PM) {
                    String username = arguments.get(0);
                    String msg = arguments.get(1);
                    System.out.printf("%s whispers to you: %s\n", username, msg);
                }
                //EXIT
                else if (header == Message.HEADER_SERVER_SEND_LEAVE) {
                    String username = arguments.get(0);
                    System.out.println(username + " has left.");
                }
            }
        } catch (Exception ex) {
            System.out.println("Exception caught in listener - " + ex);
        } finally{
            System.out.println("Client Listener exiting");
        }
    }
}