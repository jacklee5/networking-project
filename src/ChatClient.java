import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

public class ChatClient {
    private static Socket socket;
    private static BufferedReader socketIn;
    private static PrintWriter out;
    private static boolean named;
    private static String name;
    
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

        // System.out.print("Chat sessions has started - enter a user name: ");
        // String name = userInput.nextLine().trim();
        // out.println(name); //out.flush();

        String line = userInput.nextLine().trim();
        while(!line.toLowerCase().startsWith("/quit")) {
            String header = "CHAT";
            if (!named) {
                header = "NAME";
                name = line;
            }
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
                    String[] message = incoming.split(" ");
                    String header = message[0];
                    //SUBMITNAME
                    if (header.equals("SUBMITNAME")) {
                        System.out.print("Enter your username: ");
                    }
                    //WELCOME
                    else if (header.equals("WELCOME")) {
                        String newName = message[1];
                        if (!named && newName.equals(name)) {
                            named = true;
                        }
                        System.out.println(newName + " has joined");
                    }
                    //CHAT
                    else if (header.equals("CHAT")) {
                        String username = message[1];
                        String msg = incoming.substring(5 + username.length()).trim();
                        System.out.println(username + ": "  + msg);
                    }
                    //EXIT
                    else if (header.equals("EXIT")) {
                        String username = message[1];
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
}
