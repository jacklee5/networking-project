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
    private static ArrayList<String> online;
    
    public static void main(String[] args) throws Exception {
        Scanner userInput = new Scanner(System.in);
        
        System.out.println("What's the server IP? ");
        String serverip = userInput.nextLine();
        System.out.println("What's the server port? ");
        int port = userInput.nextInt();

        // String serverip = "127.0.0.1";
        // int port = 54321;

        userInput.nextLine();

        socket = new Socket(serverip, port);
        socketIn = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        out = new ObjectOutputStream(socket.getOutputStream());
        in = new ObjectInputStream(socket.getInputStream());

        // start a thread to listen for server messages
        ServerListener listener = new ServerListener(in, named, name, online);
        Thread t = new Thread(listener);
        t.start();

        // System.out.print("Chat sessions has started - enter a user name: ");
        // String name = userInput.nextLine().trim();
        // out.println(name); //out.flush();

        String line = userInput.nextLine().trim();

        while(!line.toLowerCase().startsWith("/quit")) {
            ArrayList<String> arguments = new ArrayList<String>();
            ArrayList<Integer> argument_indexes = new ArrayList<Integer>();
            int header = -1;

            for (int i = 0; i < line.length(); i++) {
                if (line.charAt(i) == ' ')
                    argument_indexes.add(i);
            }

            if (!listener.getNamed()) {
                listener.setName(line);

                header = Message.HEADER_CLIENT_SEND_NAME;
                arguments.add(line);
            } else if (line.toLowerCase().startsWith("/pchat")) {
                int i;
                String[] words = line.split(" ");

                for (i = 1; i < words.length; i++) {
                    if (words[i].charAt(0) == '@') 
                        arguments.add(words[i].strip().substring(1));
                    else
                        break;
                }
                
                String message = line.substring(argument_indexes.get(i - 1) + 1);

                header = Message.HEADER_CLIENT_SEND_PM;
                arguments.add(message);
            } else if (line.toLowerCase().startsWith("/nuke")) {
                String nukephrase = line.substring(argument_indexes.get(0) + 1);

                header = Message.HEADER_CLIENT_SEND_NUKE;
                arguments.add(nukephrase);
            } else if (line.toLowerCase().startsWith("/whoishere")) {
                System.out.println("These users are online: ");
                for (int i = 0; i < listener.getOnline().size(); i++)
                    System.out.println(listener.getOnline().get(i));
            } else {
                String message = line;

                header = Message.HEADER_CLIENT_SEND_MESSAGE;
                arguments.add(message);
            }

            //only send to the server if needed
            if (header != -1)
                out.writeObject(new Message(header, arguments));
            line = userInput.nextLine().trim();
        }

        out.writeObject(new Message(Message.HEADER_CLIENT_SEND_LOGOUT, null));;
        out.close();
        userInput.close();
        socketIn.close();
        socket.close();
        
    }
}
