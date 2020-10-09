import java.util.ArrayList;
import java.net.SocketException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.IOException;

public class ClientHandler implements Runnable {
    // Maintain data about the client serviced by this thread
    ClientConnectionData client;
    ArrayList<ClientConnectionData> clientList;
    ArrayList<Log> logs;

    public ClientHandler(ClientConnectionData client, ArrayList<ClientConnectionData> clientList, ArrayList<Log> logs) {
        this.client = client;
        this.clientList  = clientList;
        this.logs =  logs;
    }

    /**
     * Broadcasts a message to all clients connected to the server.
     */
    public void broadcast(Message msg) {
        try {
            System.out.println("Broadcasting -- " + msg);

            synchronized (clientList) {
                for (ClientConnectionData c : clientList){
                    System.out.println(c.getUserName());
                    c.getOut().writeObject(msg);
                    // c.getOut().flush();
                }
            }
        } catch (Exception ex) {
            System.out.println("broadcast caught exception: " + ex);
            ex.printStackTrace();
        }
    }

    public void broadcast(Message msg, ArrayList<String> recipient) {
        try {
            System.out.println("Broadcasting -- " + msg);

            synchronized (clientList) {
                for (ClientConnectionData c : clientList){
                    System.out.println(c.getUserName());
                    for (int i = 0; i < recipient.size(); i++) {
                        if (c.getUserName().equals(recipient.get(i))) {
                            c.getOut().writeObject(msg);
                        }
                    }
                }
            }
        } catch (Exception ex) {
            System.out.println("broadcast caught exception: " + ex);
            ex.printStackTrace();
        }
    }

    public void broadcast(Message msg, String recipient) {
        try {
            System.out.println("Broadcasting -- " + msg);

            synchronized (clientList) {
                for (ClientConnectionData c : clientList){
                    System.out.println(c.getUserName());
                    if (c.getUserName().equals(recipient)) {
                        c.getOut().writeObject(msg);
                    }
                }
            }
        } catch (Exception ex) {
            System.out.println("broadcast caught exception: " + ex);
            ex.printStackTrace();
        }
    }

    public void broadcastOnline() {
        ArrayList<String> online = new ArrayList<String>();
        for (int i = 0; i < clientList.size(); i++) {
            online.add(clientList.get(i).getUserName());
        }

        broadcast(new Message(Message.HEADER_SERVER_SEND_ONLINE, online));
    }
        
    public boolean nameIsValid(String name) {
        if (name.contains(" ") || !name.matches("^[a-zA-Z0-9]*$")) 
            return false;

        synchronized (clientList) {
            for (ClientConnectionData c : clientList) {
                if (c.getUserName().equals(name))
                    return false;
            }
        }
        return true;
    }

    @Override
    public void run() {
        try {
            ObjectInputStream in = client.getInput();
            ObjectOutputStream out = client.getOut();
            //get userName, first message from user
            // String userName = in.readLine().trim();
            // client.setUserName(userName);
            // //notify all that client has joined
            // broadcast(String.format("WELCOME %s", client.getUserName()));

            // request a username
            out.writeObject(new Message(Message.HEADER_SERVER_REQ_NAME, null));

            Message incoming;

            while( (incoming = (Message)in.readObject()) != null) {
                System.out.println("Incoming Packet -- ");
                System.out.println(incoming);

                int header = incoming.getHeader();

                if (header == Message.HEADER_CLIENT_SEND_LOGOUT) {
                    break;
                } else if (client.getUserName() == null) {
                    if (header == Message.HEADER_CLIENT_SEND_NAME) {
                        String name = incoming.getPayload().get(0);
                        // check that name is valid
                        if (nameIsValid(name)) {
                            client.setUserName(name);
                            synchronized (clientList) {
                                clientList.add(client);
                            }

                            System.out.println("added client " + name);

                            ArrayList<String> payload = new ArrayList<String>();
                            payload.add(name);

                            broadcastOnline();
                            broadcast(new Message(Message.HEADER_SERVER_SEND_WELCOME, payload));
                        } else {
                            out.writeObject(new Message(Message.HEADER_SERVER_REQ_NAME, null));
                        }
                    } else {
                        out.writeObject(new Message(Message.HEADER_SERVER_REQ_NAME, null));
                    }
                } else {
                    if (header == Message.HEADER_CLIENT_SEND_MESSAGE) {
                        String chat = incoming.getPayload().get(0);

                        if (chat.length() > 0) {
                            synchronized(logs) {
                                logs.add(new Log(client.getUserName(), chat, System.currentTimeMillis()));
                            }
                            
                            ArrayList<String> payload = new ArrayList<String>();
                            payload.add(client.getUserName());
                            payload.add(chat);

                            broadcast(new Message(Message.HEADER_SERVER_SEND_MESSAGE, payload));    
                        }
                    } else if (header == Message.HEADER_CLIENT_SEND_PM) {
                        ArrayList<String> recipient = new ArrayList<String>();
                        ArrayList<String> payload = new ArrayList<String>();

                        String msg = incoming.getPayload().get(incoming.getPayload().size() - 1);

                        payload.add(client.getUserName());
                        payload.add(msg);

                        for (int i = 0; i < incoming.getPayload().size() - 1; i++)
                            recipient.add(incoming.getPayload().get(i));

                        broadcast(new Message(Message.HEADER_SERVER_SEND_PM, payload), recipient);
                    } else if (header == Message.HEADER_CLIENT_SEND_NUKE) {
                        String nukephrase = incoming.getPayload().get(0);

                        nuke(client.getUserName(), nukephrase);
                    }
                }
            }
        } catch (Exception ex) {
            if (ex instanceof SocketException) {
                System.out.println("Caught socket ex for " + 
                    client.getName());
            } else {
                System.out.println(ex);
                ex.printStackTrace();
            }
        } finally {
            //Remove client from clientList, notify all
            synchronized (clientList) {
                clientList.remove(client); 
            }

            ArrayList<String> payload = new ArrayList<String>();
            payload.add(client.getUserName());

            broadcastOnline();
            broadcast(new Message(Message.HEADER_SERVER_SEND_LEAVE, payload));
            try {
                client.getSocket().close();
            } catch (IOException ex) {}

        }
    }
    
    private void nuke(String user, String nukeprhase) {
        ArrayList<String> victims = new ArrayList<String>();
        Pattern pattern = Pattern.compile(nukeprhase, Pattern.CASE_INSENSITIVE);


        synchronized(logs) {
            for (int i = logs.size() - 1; i >= 0; i--) {
                if (System.currentTimeMillis() - logs.get(i).getTime() > 600000) 
                    break;
                
                Matcher matcher = pattern.matcher(logs.get(i).getMessage());

                if (matcher.find()) {
                    victims.add(logs.get(i).getUser());
                }
            }
        }
        ArrayList<String> payload = new ArrayList<String>();
        payload.add("Bot");
        payload.add("Nuked " + victims.size() + " users for using nuked phrase \"" + nukeprhase + "\"");
        broadcast(new Message(Message.HEADER_SERVER_SEND_MESSAGE, payload));

        synchronized(clientList){
            for (int i = clientList.size() - 1; i >=0; i--) {
                for (int f = 0; f < victims.size(); f++) {
                    if (clientList.get(i).getUserName().equals(victims.get(f))) {
                        ArrayList<String> payload_pm = new ArrayList<String>();
                        payload_pm.add("Bot");
                        payload_pm.add("Kicked for nuked phrase: \"" + nukeprhase + "\"");

                        broadcast(new Message(Message.HEADER_SERVER_SEND_PM, payload_pm), victims.get(f));

                        try {
                            clientList.get(i).getSocket().close();
                        } catch (Exception ex) {}

                        clientList.remove(i);
                        break;
                    }
                }
            }
        }
    }
}