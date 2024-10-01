import java.io.IOException;
import java.net.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class Matchmaker {
    private static final String SEPARATOR = "════════════════════════════════════════════════════════════════════════════════════";
    private static final int TIMEOUT = 30000;  // 30 seconds timeout for UDP listening

    private String broadcastAddress;
    private int udpPort;  // The fixed UDP port for broadcasting
    private int tcpPort;  // Randomly generated TCP port for the game connection
    private AtomicBoolean connected = new AtomicBoolean(false);  // To track if the connection is established

    public Matchmaker(String broadcastAddress, int udpPort, int tcpPort) {
        this.broadcastAddress = broadcastAddress;
        this.udpPort = udpPort;  // Fixed UDP port for broadcast
        this.tcpPort = tcpPort;  // Randomly generated TCP port (9000-9100)
    }

    // Start the matchmaking process
    public void startMatchmaking() {
        displayBanner();
        System.out.println("Searching for opponents...");

        while (!connected.get()) {
            try {
                boolean messageReceived = listenForUdpMessage();

                if (!messageReceived) {
                    sendUdpBroadcast();
                    startTcpServer();  // Start the TCP server after sending "NEW GAME"
                }
            } catch (IOException e) {
                System.out.println("Error occurred during matchmaking: " + e.getMessage());
            }
        }
    }

    // Listen for UDP broadcast messages from other players
    private boolean listenForUdpMessage() throws IOException {
        if (connected.get()) return true;

        try (DatagramSocket udpSocket = new DatagramSocket(udpPort)) {
            udpSocket.setSoTimeout(TIMEOUT);
            byte[] buffer = new byte[256];
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

            System.out.println("\n" + SEPARATOR);
            System.out.println("Listening for 'NEW GAME' messages on UDP port " + udpPort + " for " + TIMEOUT / 1000 + " seconds...");
            udpSocket.receive(packet);  // Listen for incoming messages

            String receivedMessage = new String(packet.getData(), 0, packet.getLength());
            System.out.println("Received message: " + receivedMessage);

            if (receivedMessage.startsWith("NEW GAME:")) {
                int opponentTcpPort = Integer.parseInt(receivedMessage.split(":")[1]);
                String opponentIp = packet.getAddress().getHostAddress();

                System.out.println("\n" + SEPARATOR);
                System.out.println("Opponent found! Connecting to " + opponentIp + ":" + opponentTcpPort + "...");
                startGameAsClient(opponentIp, opponentTcpPort);  // Act as Player 2 (client)
                connected.set(true);
                return true;
            }
        } catch (SocketTimeoutException e) {
            System.out.println("No 'NEW GAME' message received within " + TIMEOUT / 1000 + " seconds.");
            return false;
        }

        return false;
    }

    // Send UDP broadcast with the player's TCP port to invite others to connect
    private void sendUdpBroadcast() {
        if (connected.get()) return;

        try (DatagramSocket udpSocket = new DatagramSocket()) {
            udpSocket.setBroadcast(true);
            InetAddress broadcastAddr = InetAddress.getByName(broadcastAddress);
            String message = "NEW GAME:" + tcpPort;

            DatagramPacket packet = new DatagramPacket(message.getBytes(), message.length(), broadcastAddr, udpPort);
            udpSocket.send(packet);

            System.out.println("\n" + SEPARATOR);
            System.out.println("Sent 'NEW GAME' message with TCP port " + tcpPort);
            System.out.println("Setting up TCP server to accept incoming connections...");

            // After sending, start the TCP server to accept connections
        } catch (IOException e) {
            System.out.println("Error sending UDP broadcast: " + e.getMessage());
        }
    }

    // Start the game as a client (Player 2) and connect to the opponent's TCP port
    private void startGameAsClient(String opponentIp, int opponentTcpPort) {
        if (connected.get()) return;

        try {
            Socket socket = new Socket();
            socket.connect(new InetSocketAddress(opponentIp, opponentTcpPort), 5000);  // 5-second timeout for TCP connection
            System.out.println("Connected to opponent at " + opponentIp + ":" + opponentTcpPort + "!");
            System.out.println("\n" + SEPARATOR);

            GameLogic gameLogic = new GameLogic(socket, false);  // Player 2 (false)
            gameLogic.start();
            connected.set(true);
        } catch (SocketTimeoutException e) {
            System.out.println("Connection to opponent timed out.");
        } catch (IOException e) {
            System.out.println("Unable to connect to opponent at " + opponentIp + ":" + opponentTcpPort + ": " + e.getMessage());
        }
    }

    // Start a TCP server for the opponent to connect (Player 1)
    private void startTcpServer() {
        if (connected.get()) return;

        try (ServerSocket serverSocket = new ServerSocket(tcpPort)) {
            serverSocket.setSoTimeout(TIMEOUT * 2);  // Wait for up to 60 seconds for the opponent to connect
            System.out.println("Waiting for opponent to connect on TCP port " + tcpPort + "...");

            Socket clientSocket = serverSocket.accept();  // Wait for Player 2 to connect
            System.out.println("Opponent connected!");
            System.out.println("\n" + SEPARATOR);

            GameLogic gameLogic = new GameLogic(clientSocket, true);  // Player 1 (true)
            gameLogic.start();
            connected.set(true);
        } catch (SocketTimeoutException e) {
            System.out.println("No opponent connected within the time limit.");
        } catch (IOException e) {
            System.out.println("Error setting up TCP server: " + e.getMessage());
        }
    }

    // Banner to display when the program starts
    private void displayBanner() {
        System.out.println("Welcome to Connect4 Multiplayer!");
        System.out.println(SEPARATOR);
    }
}
