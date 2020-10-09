
import java.net.Inet4Address;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public class ChatServer {
    public static final int PORT = 54321;
    private static final ArrayList<ClientConnectionData> clientList = new ArrayList<>();
    private static final ArrayList<Log> logs =  new ArrayList<Log>();

    public static void main(String[] args) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(100);

        try (ServerSocket serverSocket = new ServerSocket(PORT)){
            System.out.println("Chat Server started.");
            System.out.println("Local IP: "
                    + Inet4Address.getLocalHost().getHostAddress());
            System.out.println("Local Port: " + serverSocket.getLocalPort());
        
            while (true) {
                try {
                    Socket socket = serverSocket.accept();
                    System.out.printf("Connected to %s:%d on local port %d\n",
                        socket.getInetAddress(), socket.getPort(), socket.getLocalPort());
                    
                    // This code should really be done in the separate thread
                    ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
                    ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
                    String name = socket.getInetAddress().getHostName();

                    ClientConnectionData client = new ClientConnectionData(socket, in, out, name);

                    //handle client business in another thread
                    pool.execute(new ClientHandler(client));
                } catch (IOException ex) {
                    System.out.println(ex.getMessage());
                }

            }
        } 
    }

    // Inner class 
    static class ClientHandler implements Runnable {
        // Maintain data about the client serviced by this thread
        ClientConnectionData client;

        public ClientHandler(ClientConnectionData client) {
            this.client = client;
        }

        /**
		 * Broadcasts a message to all clients connected to the server.
		 */
        public void broadcast(Message msg) {
            try {
                System.out.println("Broadcasting -- " + msg);

                synchronized (clientList) {
                    for (ClientConnectionData c : clientList){
                        if (c.getUserName() != client.getUserName() || !(msg.getHeader() == Message.HEADER_SERVER_SEND_MESSAGE))
                            c.getOut().writeObject(msg);
                        // c.getOut().flush();
                    }
                }
            } catch (Exception ex) {
                System.out.println("broadcast caught exception: " + ex);
                ex.printStackTrace();
            }
            
        }

        public void broadcast(boolean always_send, Message msg) {
            try {
                System.out.println("Broadcasting -- " + msg);

                synchronized (clientList) {
                    for (ClientConnectionData c : clientList){
                        c.getOut().writeObject(msg);
                    }
                }

            } catch (Exception ex) {
                System.out.println("broadcast caught exception: " + ex);
                ex.printStackTrace();
            }
            
        }

        public void broadcast(String recipient, Message msg) {
            try {
                System.out.println("Broadcasting -- " + msg);

                synchronized (clientList) {
                    for (ClientConnectionData c : clientList){
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
                // while (true) {
                //     incoming = (Message) in.readObject();

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
                            String recipient = incoming.getPayload().get(0);
                            String msg = incoming.getPayload().get(1);

                            ArrayList<String> payload = new ArrayList<String>();
                            payload.add(client.getUserName());
                            payload.add(msg);

                            broadcast(recipient, new Message(Message.HEADER_SERVER_SEND_PM, payload));
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
            broadcast(true, new Message(Message.HEADER_SERVER_SEND_MESSAGE, payload));

            synchronized(clientList){
                for (int i = clientList.size() - 1; i >=0; i--) {
                    for (int f = 0; f < victims.size(); f++) {
                        if (clientList.get(i).getUserName().equals(victims.get(f))) {
                            ArrayList<String> payload_pm = new ArrayList<String>();
                            payload_pm.add("Bot");
                            payload_pm.add("Kicked for nuked phrase: \"" + nukeprhase + "\"");
                            broadcast(victims.get(f), new Message(Message.HEADER_SERVER_SEND_PM, payload_pm));
                            try {
                                clientList.get(i).getSocket().close();
                            } catch (Exception ex) {

                            }
                            clientList.remove(i);
                            break;
                        }
                    }
                }
            }
        }
    }
}
