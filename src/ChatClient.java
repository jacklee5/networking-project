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
        ServerListener listener = new ServerListener(in, named, name);
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

            if (!listener.getNamed()) {
                header = Message.HEADER_CLIENT_SEND_NAME;
                
                listener.setName(line);

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

            out.writeObject(msg);
            line = userInput.nextLine().trim();
        }
        out.writeObject(new Message(Message.HEADER_CLIENT_SEND_LOGOUT, null));;
        out.close();
        userInput.close();
        socketIn.close();
        socket.close();
        
    }
}
