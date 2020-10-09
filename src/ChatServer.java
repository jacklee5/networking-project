
import java.net.Inet4Address;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.io.IOException;
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
                    pool.execute(new ClientHandler(client, clientList, logs));
                } catch (IOException ex) {
                    System.out.println(ex.getMessage());
                }

            }
        } 
    }

    // Inner class 
}
