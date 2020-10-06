package day5_bca;

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
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public class ChatServer {
    public static final int PORT = 54321;
    private static final ArrayList<ClientConnectionData> clientList = new ArrayList<>();

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
                    // synchronized (clientList) {
                    //     clientList.add(client);
                    // }
                    
                    System.out.println("added client " + name);

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
                        //TODO: Brodcast messages
                        c.getOut().writeObject(new Message(Constants.HEADER_SERVER_SEND_MESSAGE, msg));;
                        // c.getOut().flush();
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
                ObjectInputStream in = client.getInput();
                //get userName, first message from user
                //TODO: Recieve username
                Message userName = (Message)in.readObject();
                client.setUserName(userName.payload.get(0));
                //notify all that client has joined
                //TODO: Send message that person joined
                broadcast(String.format("WELCOME %s", client.getUserName()));

                
                String incoming = "";

                while( (incoming = in.readLine()) != null) {
                    //TODO: Check message type and do things
                    if (incoming.startsWith("CHAT") || true) {
                        String chat = incoming.substring(4).trim();
                        if (chat.length() > 0) {
                            String msg = String.format("CHAT %s %s", client.getUserName(), chat);
                            broadcast(msg);    
                        }
                    }
                }
            } catch (Exception e) {
                System.out.println(e);
            } finally {
                //Remove client from clientList, notify all
                synchronized (clientList) {
                    clientList.remove(client); 
                }
                System.out.println(client.getName() + " has left.");
                //TODO: Send message that person left
                broadcast(String.format("EXIT %s", client.getUserName()));
                try {
                    client.getSocket().close();
                } catch (IOException ex) {}

            }
        }
        
    }

}
