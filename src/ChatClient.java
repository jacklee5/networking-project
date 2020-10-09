import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;
import java.net.Socket;
import java.util.Scanner;
import java.util.ArrayList;

public class ChatClient {
    private static Socket socket;
    private static BufferedReader socketIn;
    private static boolean named;
    private static String name;
    private static ObjectOutputStream out;
    private static ObjectInputStream in;
    
    public static void main(String[] args) throws Exception {
        Scanner userInput = new Scanner(System.in);
        
        System.out.println("What's the server IP? ");
        String serverip = userInput.nextLine();
        // String serverip = "127.0.0.1";
        System.out.println("What's the server port? ");
        int port = userInput.nextInt();
        // int port = 54321;
        userInput.nextLine();

        socket = new Socket(serverip, port);
        socketIn = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        out = new ObjectOutputStream(socket.getOutputStream());
        in = new ObjectInputStream(socket.getInputStream());

        // start a thread to listen for server messages
        ServerListener listener = new ServerListener();
        Thread t = new Thread(listener);
        t.start();

        // System.out.print("Chat sessions has started - enter a user name: ");
        // String name = userInput.nextLine().trim();
        // out.println(name); //out.flush();

        String line = userInput.nextLine().trim();
        while(!line.toLowerCase().startsWith("/quit")) {
            int header;
            ArrayList<String> arguments = new ArrayList<String>();
            ArrayList<Integer> argument_indexes = new ArrayList<Integer>();
            Message msg;

            for (int i = 0; i < line.length(); i++) {
                if (line.charAt(i) == ' ')
                    argument_indexes.add(i);
            }

            if (!named) {
                header = Message.HEADER_CLIENT_SEND_NAME;
                
                name = line;

                arguments.add(line);
            } else if (line.toLowerCase().startsWith("/pchat")) {
                header = Message.HEADER_CLIENT_SEND_PM;

                String recipient = line.substring(argument_indexes.get(0) + 1, argument_indexes.get(1));
                String message = line.substring(argument_indexes.get(1) + 1);

                arguments.add(recipient);
                arguments.add(message);
            } else if (line.toLowerCase().startsWith("/nuke")) {
                header = Message.HEADER_CLIENT_SEND_NUKE;

                String nukephrase = line.substring(argument_indexes.get(0) + 1);
                
                arguments.add(nukephrase);
            } else {
                header = Message.HEADER_CLIENT_SEND_MESSAGE;

                String message = line;
                
                arguments.add(message);
            }

            msg = new Message(header, arguments);
            
            // for (int i = 0; i < msg.getPayload().size(); i++)
            //     System.out.println(msg.getPayload().get(i));

            out.writeObject(msg);
            line = userInput.nextLine().trim();
        }
        out.writeObject(new Message(Message.HEADER_CLIENT_SEND_LOGOUT, null));;
        out.close();
        userInput.close();
        socketIn.close();
        socket.close();
        
    }

    static class ServerListener implements Runnable {

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
                            System.out.println("L_?");
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
}
