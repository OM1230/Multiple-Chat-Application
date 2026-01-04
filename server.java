import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class Server {

    private static final int PORT = 12345; // you can change this if needed
    // Thread-safe list for clients
    private static List<ClientHandler> clients = new CopyOnWriteArrayList<>();

    public static void main(String[] args) {
        System.out.println("Chat server started on port " + PORT);

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {

            while (true) {
                Socket socket = serverSocket.accept();
                System.out.println("New client connected: " + socket.getRemoteSocketAddress());

                ClientHandler handler = new ClientHandler(socket);
                clients.add(handler);
                handler.start(); // start new thread for this client
            }

        } catch (IOException e) {
            System.out.println("Server error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Broadcast message to all clients except the sender
    static void broadcast(String message, ClientHandler excludeClient) {
        for (ClientHandler client : clients) {
            if (client != excludeClient) {
                client.sendMessage(message);
            }
        }
    }

    // Remove client from list when they disconnect
    static void removeClient(ClientHandler client) {
        clients.remove(client);
    }

    // Inner class: one thread per client
    private static class ClientHandler extends Thread {
        private Socket socket;
        private BufferedReader in;
        private PrintWriter out;
        private String name = "Unknown";

        ClientHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try {
                in  = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);

                // Ask for name
                out.println("Enter your name: ");
                name = in.readLine();

                if (name == null || name.trim().isEmpty()) {
                    name = "User-" + getId(); // fallback name
                }

                out.println("Welcome, " + name + "! Type '/quit' to exit.");
                broadcast("[Server] " + name + " joined the chat.", this);
                System.out.println(name + " joined the chat.");

                String message;
                while ((message = in.readLine()) != null) {
                    if (message.equalsIgnoreCase("/quit")) {
                        out.println("Goodbye " + name + "!");
                        break;
                    }

                    String formatted = name + ": " + message;
                    System.out.println(formatted); // log on server console
                    broadcast(formatted, this);    // send to others
                }

            } catch (IOException e) {
                System.out.println("Connection error with " + name + ": " + e.getMessage());
            } finally {
                // Clean up
                try {
                    if (in != null) in.close();
                } catch (IOException ignored) {}

                if (out != null) out.close();

                try {
                    if (socket != null && !socket.isClosed()) socket.close();
                } catch (IOException ignored) {}

                broadcast("[Server] " + name + " left the chat.", this);
                removeClient(this);
                System.out.println(name + " disconnected.");
            }
        }

        void sendMessage(String message) {
            if (out != null) {
                out.println(message);
            }
        }
    }
}
