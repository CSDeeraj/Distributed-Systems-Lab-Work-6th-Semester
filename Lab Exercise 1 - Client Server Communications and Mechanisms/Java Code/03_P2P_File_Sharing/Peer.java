//CH.SC.U4CSE24106
import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

public class Peer {
    private static final int BUFFER = 65536;
    private final int port;
    private final Path shareDir;
    private final Path downloadDir;

    Peer(int port, Path shareDir, Path downloadDir) {
        this.port = port;
        this.shareDir = shareDir;
        this.downloadDir = downloadDir;
    }

    private void startServer() {
        Thread t = new Thread(() -> {
            try (ServerSocket server = new ServerSocket(port)) {
                System.out.println("[P2P] Sharing " + shareDir.toAbsolutePath() + " on port " + port);
                while (true) {
                    Socket s = server.accept();
                    new Thread(() -> serve(s)).start();
                }
            } catch (IOException e) {
                System.out.println("[P2P server error] " + e.getMessage());
            }
        });
        t.setDaemon(true);
        t.start();
    }

    private void serve(Socket socket) {
        try (Socket s = socket;
             BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream()));
             OutputStream out = s.getOutputStream()) {
            String command = in.readLine();
            if ("LIST".equals(command)) {
                try (DirectoryStream<Path> files = Files.newDirectoryStream(shareDir, "*.txt")) {
                    StringBuilder result = new StringBuilder();
                    for (Path p : files) result.append(p.getFileName()).append('\n');
                    out.write(result.toString().getBytes());
                }
            } else if (command != null && command.startsWith("GET ")) {
                String name = Paths.get(command.substring(4)).getFileName().toString();
                Path file = shareDir.resolve(name).normalize();
                if (!name.toLowerCase().endsWith(".txt") || !file.getParent().equals(shareDir.toAbsolutePath().normalize()) ||
                    !Files.isRegularFile(file)) {
                    out.write("ERROR\n".getBytes());
                } else {
                    long size = Files.size(file);
                    out.write(("OK " + size + "\n").getBytes());
                    try (InputStream fin = Files.newInputStream(file)) {
                        byte[] buf = new byte[BUFFER];
                        int n;
                        while ((n = fin.read(buf)) != -1) out.write(buf, 0, n);
                    }
                }
            } else {
                out.write("ERROR\n".getBytes());
            }
            out.flush();
        } catch (IOException ignored) {}
    }

    private static List<String> list(String host, int port) throws IOException {
        try (Socket s = new Socket(host, port);
             BufferedWriter out = new BufferedWriter(new OutputStreamWriter(s.getOutputStream()));
             BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream()))) {
            out.write("LIST\n"); out.flush();
            List<String> files = new ArrayList<>();
            String line;
            while ((line = in.readLine()) != null) files.add(line);
            return files;
        }
    }

    private Path get(String host, int port, String name) throws IOException {
        name = Paths.get(name).getFileName().toString();
        if (!name.toLowerCase().endsWith(".txt")) throw new IOException("Only .txt files are allowed.");
        try (Socket s = new Socket(host, port);
             OutputStream out = s.getOutputStream();
             InputStream in = s.getInputStream()) {
            out.write(("GET " + name + "\n").getBytes()); out.flush();
            ByteArrayOutputStream header = new ByteArrayOutputStream();
            int b;
            while ((b = in.read()) != -1 && b != '\n') header.write(b);
            String[] fields = header.toString().trim().split(" ");
            if (fields.length != 2 || !fields[0].equals("OK")) throw new FileNotFoundException("Peer does not have that file.");
            long remaining = Long.parseLong(fields[1]);
            Path target = downloadDir.resolve(name);
            try (OutputStream file = Files.newOutputStream(target)) {
                byte[] buf = new byte[BUFFER];
                while (remaining > 0) {
                    int n = in.read(buf, 0, (int)Math.min(buf.length, remaining));
                    if (n == -1) throw new EOFException("Transfer interrupted.");
                    file.write(buf, 0, n);
                    remaining -= n;
                }
            }
            return target;
        }
    }

    public static void main(String[] args) throws Exception {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 6001;
        Path share = Paths.get(args.length > 1 ? args[1] : "shared").toAbsolutePath().normalize();
        Path downloads = Paths.get(args.length > 2 ? args[2] : "downloads").toAbsolutePath().normalize();
        Files.createDirectories(share);
        Files.createDirectories(downloads);
        Peer peer = new Peer(port, share, downloads);
        peer.startServer();
        System.out.println("Commands: list HOST PORT | get HOST PORT FILENAME | mine | quit");
        System.out.println("Other peers can connect to your IP and port " + port);
        try (Scanner sc = new Scanner(System.in)) {
            while (true) {
                System.out.print("p2p> ");
                String[] p = sc.nextLine().trim().split("\\s+");
                if (p.length == 0 || p[0].isEmpty()) continue;
                try {
                    if (p[0].equalsIgnoreCase("quit")) break;
                    if (p[0].equalsIgnoreCase("mine")) {
                        try (DirectoryStream<Path> ds = Files.newDirectoryStream(share, "*.txt")) {
                            for (Path f : ds) System.out.println(f.getFileName());
                        }
                    } else if (p[0].equalsIgnoreCase("list") && p.length == 3) {
                        List<String> files = list(p[1], Integer.parseInt(p[2]));
                        files.forEach(System.out::println);
                    } else if (p[0].equalsIgnoreCase("get") && p.length == 4) {
                        System.out.println("Downloaded: " + peer.get(p[1], Integer.parseInt(p[2]), p[3]));
                    } else {
                        System.out.println("Usage: list HOST PORT | get HOST PORT FILENAME | mine | quit");
                    }
                } catch (Exception e) {
                    System.out.println("Error: " + e.getMessage());
                }
            }
        }
        System.out.println("Peer stopped.");
    }
}
