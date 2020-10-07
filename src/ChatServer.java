
import java.net.Inet4Address;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
                    BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
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
        public void broadcast(String msg) {
            try {
                System.out.println("Broadcasting -- " + msg);
                synchronized (clientList) {
                    for (ClientConnectionData c : clientList){
                        c.getOut().println(msg);
                        // c.getOut().flush();
                    }
                }
            } catch (Exception ex) {
                System.out.println("broadcast caught exception: " + ex);
                ex.printStackTrace();
            }
            
        }

        public void broadcast(String sender, String recipient, String msg) {
            try {
                System.out.println("Broadcasting -- " + msg);

                synchronized (clientList) {
                    for (ClientConnectionData c : clientList){
                        if (c.getUserName().equals(recipient)) {
                            System.out.println("HI");
                            c.getOut().println(msg);
                            System.out.println("OK");
                            c.getOut().flush();
                        }
                    }
                }
            } catch (Exception ex) {
                System.out.println("broadcast caught exception: " + ex);
                ex.printStackTrace();
            }
            
        }

        @Override
        public void run() {
            try {
                BufferedReader in = client.getInput();
                PrintWriter out = client.getOut();
                //get userName, first message from user
                // String userName = in.readLine().trim();
                // client.setUserName(userName);
                // //notify all that client has joined
                // broadcast(String.format("WELCOME %s", client.getUserName()));

                // request a username
                out.println("SUBMITNAME");

                String incoming = "";
                while( (incoming = in.readLine()) != null) {
                    System.out.println(incoming);
                    String header = incoming.split(" ")[0];
                    if (header.equals("QUIT")) {
                        break;
                    }else if (client.getUserName() == null) {
                        if (header.equals("NAME")) {
                            String name = incoming.substring(4).trim();
                            client.setUserName(name);
                            synchronized (clientList) {
                                clientList.add(client);
                            }
                            System.out.println("added client " + name);
                            broadcast("WELCOME " + name);
                        } else {
                            out.println("SUBMITNAME");
                        }
                    } else {
                        if (header.equals("CHAT")) {
                            String chat = incoming.substring(4).trim();
                            if (chat.length() > 0) {
                                synchronized(logs) {
                                    logs.add(new Log(client.getUserName(), chat, System.currentTimeMillis()));
                                }
                                String msg = String.format("CHAT %s %s", client.getUserName(), chat);
                                broadcast(msg);    
                            }
                        } else if (header.equals("PCHAT")) {
                            System.out.println(incoming);
                            String chat = incoming.substring(12).trim();
                            String recipient = chat.substring(0, chat.indexOf(" ")).trim();
                            String msg = chat.substring(chat.indexOf(" ") + 1).trim();
                            msg = String.format("PCHAT %s %s", client.getUserName(), msg);
                            broadcast(client.getUserName(), recipient, msg);
                        } else if (header.equals("NUKE")) {
                            String nukephrase = incoming.substring(10).trim();
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
                System.out.println(client.getName() + " has left.");
                broadcast(String.format("EXIT %s", client.getUserName()));
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
                    System.out.println(i);

                    System.out.println(logs.get(i).getMessage());
                    System.out.println(System.currentTimeMillis() - logs.get(i).getTime());


                    if (System.currentTimeMillis() - logs.get(i).getTime() > 600000) 
                        break;
                    
                    Matcher matcher = pattern.matcher(logs.get(i).getMessage());
                    if (matcher.find()) {
                        victims.add(logs.get(i).getUser());
                        System.out.println("victim " + logs.get(i).getUser());
                    }
                }
            }
            String msg = String.format("CHAT %s %s", "Bot", "Nuked " + victims.size() + " users for using nuked phrase \"" + nukeprhase + "\"");
            broadcast(msg);

            synchronized(clientList){
                for (int i = clientList.size() - 1; i >=0; i--) {
                    for (int f = 0; f < victims.size(); f++) {
                        if (clientList.get(i).getUserName().equals(victims.get(f))) {
                            String ban_msg = String.format("PCHAT %s %s", "Bot", "Kicked for nuked phrase: \"" + nukeprhase + "\"");
                            broadcast("Bot", victims.get(f), ban_msg);
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
