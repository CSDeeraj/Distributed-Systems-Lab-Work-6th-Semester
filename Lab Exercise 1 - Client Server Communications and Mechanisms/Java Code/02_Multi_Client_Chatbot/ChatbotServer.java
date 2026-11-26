//CH.SC.U4CSE24106
import java.io.*;
import java.net.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ChatbotServer {
    private static final int PORT = 5001;
    private static final int MAX_CLIENTS = 30;
    private static final Set<Socket> clients = ConcurrentHashMap.newKeySet();

    static String reply(String text) {
        String m = text.toLowerCase();
        if (m.contains("hello") || m.contains("hi") || m.contains("hey")) return "Hello! How can I help you?";
        if (m.contains("your name") || m.contains("who are you")) return "I'm JavaBot, a simple server-side chatbot.";
        if (m.contains("time")) return "Server time: " + LocalTime.now().withNano(0);
        if (m.contains("date")) return "Server date: " + LocalDate.now();
        if (m.contains("help")) return "Try: hello, your name, time, date, or bye.";
        if (m.equals("bye") || m.equals("goodbye")) return "Goodbye! Use /quit to disconnect.";
        if (m.contains("thank")) return "You're welcome!";
        return "I am a simple rule-based bot. Type 'help' to see what I understand.";
    }

    public static void main(String[] args) throws IOException {
        try (ServerSocket server = new ServerSocket(PORT)) {
            System.out.println("JavaBot listening on port " + PORT + " (maximum " + MAX_CLIENTS + " clients)");
            while (true) {
                Socket socket = server.accept();
                if (clients.size() >= MAX_CLIENTS) {
                    new PrintWriter(socket.getOutputStream(), true).println("Server full. Try later.");
                    socket.close();
                    continue;
                }
                clients.add(socket);
                new Thread(() -> handle(socket)).start();
            }
        }
    }

    private static void handle(Socket socket) {
        String name = socket.getRemoteSocketAddress().toString();
        try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {
            out.println("Welcome to JavaBot. Enter your name:");
            String n = in.readLine();
            if (n != null && !n.trim().isEmpty()) name = n.trim();
            System.out.println("[+] " + name + " connected");
            out.println("JavaBot: Hello! Type 'help' for commands. Use /quit to exit.");
            String line;
            while ((line = in.readLine()) != null) {
                if (line.equalsIgnoreCase("/quit")) {
                    out.println("JavaBot: Session ended. Goodbye!");
                    break;
                }
                if (!line.trim().isEmpty()) {
                    System.out.println(name + ": " + line);
                    out.println("JavaBot: " + reply(line));
                }
            }
        } catch (IOException e) {
            System.out.println("Connection lost: " + name);
        } finally {
            clients.remove(socket);
            try { socket.close(); } catch (IOException ignored) {}
            System.out.println("[-] " + name + " disconnected");
        }
    }
}
