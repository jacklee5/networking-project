import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

public class ChatClient {
    private static Socket socket;
    private static BufferedReader socketIn;
    private static PrintWriter out;
    private static boolean named = false;
    
    public static void main(String[] args) throws Exception {
        Scanner userInput = new Scanner(System.in);
        
        System.out.println("What's the server IP? ");
        String serverip = userInput.nextLine();
        System.out.println("What's the server port? ");
        int port = userInput.nextInt();
        userInput.nextLine();

        socket = new Socket(serverip, port);
        socketIn = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        out = new PrintWriter(socket.getOutputStream(), true);

        // start a thread to listen for server messages
        ServerListener listener = new ServerListener();
        Thread t = new Thread(listener);
        t.start();

        String line = userInput.nextLine().trim();
        while(!line.toLowerCase().startsWith("/quit")) {
            String header = named ? "CHAT" : "NAME";
            String msg = String.format("%s %s", header, line); 
            out.println(msg);
            line = userInput.nextLine().trim();
        }
        out.println("QUIT");
        out.close();
        userInput.close();
        socketIn.close();
        socket.close();
        
    }

    static class ServerListener implements Runnable {

        @Override
        public void run() {
            try {
                String incoming = "";

                while( (incoming = socketIn.readLine()) != null) {
                    //handle different headers
                    String header = incoming.split(" ")[0];
                    if (header.equals("SUBMITNAME")) {
                        System.out.print("Enter your username: ");
                    }
                    //WELCOME
                    //CHAT
                    //EXIT
                    System.out.println(incoming);
                }
            } catch (Exception ex) {
                System.out.println("Exception caught in listener - " + ex);
            } finally{
                System.out.println("Client Listener exiting");
            }
        }
    }
}
