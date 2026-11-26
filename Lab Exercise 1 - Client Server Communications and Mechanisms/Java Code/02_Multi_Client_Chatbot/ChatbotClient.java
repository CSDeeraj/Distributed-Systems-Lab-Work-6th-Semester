//CH.SC.U4CSE24106
import java.io.*;
import java.net.*;
import java.util.Scanner;

public class ChatbotClient {
    public static void main(String[] args) {
        String host = args.length > 0 ? args[0] : "127.0.0.1";
        try (Socket socket = new Socket(host, 5001);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             Scanner sc = new Scanner(System.in)) {
            System.out.println(in.readLine());
            out.println(sc.nextLine());
            Thread reader = new Thread(() -> {
                try {
                    String line;
                    while ((line = in.readLine()) != null) System.out.println(line);
                } catch (IOException ignored) {}
            });
            reader.setDaemon(true);
            reader.start();
            while (sc.hasNextLine()) {
                String msg = sc.nextLine();
                out.println(msg);
                if (msg.equalsIgnoreCase("/quit")) break;
            }
        } catch (IOException e) {
            System.out.println("Connection error: " + e.getMessage());
        }
    }
}
