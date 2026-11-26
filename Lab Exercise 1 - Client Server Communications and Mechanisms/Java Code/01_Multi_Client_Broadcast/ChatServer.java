//CH.SC.U4CSE24106
import java.io.*;
import java.net.*;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ChatServer {
    private static final int PORT = 5000;
    private static final Set<ClientHandler> clients = ConcurrentHashMap.newKeySet();

    static void broadcast(String msg, ClientHandler sender) {
        for (ClientHandler c : clients) {
            if (c != sender) c.send(msg);
        }
    }

    public static void main(String[] args) throws IOException {
        try (ServerSocket server = new ServerSocket(PORT)) {
            System.out.println("Broadcast chat server listening on port " + PORT);
            while (true) {
                Socket socket = server.accept();
                ClientHandler handler = new ClientHandler(socket);
                clients.add(handler);
                new Thread(handler).start();
            }
        }
    }

    static class ClientHandler implements Runnable {
        private final Socket socket;
        private PrintWriter out;
        private String name = "Guest";

        ClientHandler(Socket socket) { this.socket = socket; }

        void send(String msg) {
            if (out != null) { out.println(msg); out.flush(); }
        }

        public void run() {
            try {
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);
                out.println("Enter your name:");
                String entered = in.readLine();
                if (entered != null && !entered.trim().isEmpty()) name = entered.trim();
                System.out.println("[+] " + name + " connected");
                broadcast("[SERVER] " + name + " joined.", this);
                String line;
                while ((line = in.readLine()) != null) {
                    if (line.equalsIgnoreCase("/quit")) break;
                    if (!line.trim().isEmpty()) {
                        System.out.println(name + ": " + line);
                        broadcast(name + ": " + line, this);
                    }
                }
            } catch (IOException e) {
                System.out.println("Connection lost: " + name);
            } finally {
                clients.remove(this);
                broadcast("[SERVER] " + name + " left.", this);
                try { socket.close(); } catch (IOException ignored) {}
            }
        }
    }
}
