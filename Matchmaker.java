import java.io.IOException;
import java.net.*;

public class Matchmaker {
    private static final String SEPARATOR = Connect4.SEPARATOR;  // Separator for console output formatting
    private static final int TIMEOUT = 30000;  // Timeout set to 30 seconds for retrying connections

    private String broadcastAddress;  // Broadcast address for UDP communication
    private int udpPort;  // Port number for sending/receiving UDP messages
    private int tcpPort;  // TCP port used for direct connections
    private boolean connected = false;  // Flag to check if a connection has been established
    private DatagramSocket udpSocket = null;  // UDP socket for broadcasting/listening
    private ServerSocket serverSocket = null;  // TCP server socket for accepting connections
    private Socket clientSocket = null;  // TCP client socket for initiating connections

    // Constructor to initialize Matchmaker with broadcast address and ports
    public Matchmaker(String broadcastAddress, int udpPort, int tcpPort) {
        this.broadcastAddress = broadcastAddress;
        this.udpPort = udpPort;
        this.tcpPort = tcpPort;
    }

    // Main matchmaking process that alternates between broadcasting and listening for a connection
    public void startMatchmaking() {
        System.out.println("Starting matchmaking process...");
        boolean gameFound = false;

        while (!gameFound) {
            try {
                gameFound = listenForUdpMessage();  // Listen for "NEW GAME" UDP messages

                // If no "NEW GAME" message received, send own broadcast message
                if (!gameFound) {
                    System.out.println("No 'NEW GAME' message received, sending my own.");
                    sendUdpBroadcast();  // Send broadcast to announce game

                    // Wait for a while to allow other clients to respond
                    Thread.sleep(TIMEOUT);

                    // Check if connection was made, otherwise retry listening
                    if (!connected) {
                        System.out.println("No connection established. Returning to UDP listening...");
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();  // Handle unexpected errors gracefully
            }
        }
    }

    // Sends a UDP broadcast to announce that a new game is available
    public void sendUdpBroadcast() {
        if (connected) return;  // Exit if already connected

        try {
            udpSocket = new DatagramSocket();  // Create UDP socket
            InetAddress broadcastAddr = InetAddress.getByName(broadcastAddress);  // Resolve broadcast address
            udpSocket.setBroadcast(true);  // Enable broadcasting

            String message = "NEW GAME:" + tcpPort;  // Format message with TCP port info
            DatagramPacket packet = new DatagramPacket(message.getBytes(), message.length(), broadcastAddr, udpPort);
            udpSocket.send(packet);  // Send the broadcast packet

            System.out.println(SEPARATOR);
            System.out.println("Sent 'NEW GAME' message with TCP port " + tcpPort + " to " + broadcastAddress + ":" + udpPort);

            // Start TCP server to wait for incoming connections
            startTcpServer();
        } catch (IOException e) {
            e.printStackTrace();  // Handle I/O errors
        } finally {
            if (udpSocket != null && !udpSocket.isClosed()) {
                udpSocket.close();  // Ensure UDP socket is closed
            }
        }
    }

    // Listens for UDP messages to detect other players
    public boolean listenForUdpMessage() throws IOException {
        if (connected) return true;  // Return true if already connected

        try {
            udpSocket = new DatagramSocket(udpPort);  // Bind to specified UDP port
            byte[] buffer = new byte[256];  // Buffer for incoming data
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

            udpSocket.setSoTimeout(TIMEOUT);  // Set timeout for receiving packets
            System.out.println(SEPARATOR);
            System.out.println("Listening for 'NEW GAME' messages on UDP port " + udpPort + "...");
            udpSocket.receive(packet);  // Wait for incoming UDP packet

            String receivedMessage = new String(packet.getData(), 0, packet.getLength());
            System.out.println("Received message: " + receivedMessage);

            // If message starts with "NEW GAME:", parse and attempt to connect to opponent
            if (receivedMessage.startsWith("NEW GAME:")) {
                int opponentTcpPort = Integer.parseInt(receivedMessage.split(":")[1]);
                String opponentIp = packet.getAddress().getHostAddress();

                System.out.println("Connecting to opponent at " + opponentIp + ":" + opponentTcpPort);
                startGameAsClient(opponentIp, opponentTcpPort);  // Start as client to connect to opponent
                return true;  // Opponent found
            }
        } catch (SocketTimeoutException e) {
            System.out.println("No 'NEW GAME' message received within " + TIMEOUT / 1000 + " seconds.");
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (udpSocket != null && !udpSocket.isClosed()) {
                udpSocket.close();  // Ensure UDP socket is closed after use
            }
        }
        return false;  // Continue matchmaking if no opponent found
    }

    // Starts as a client to connect to the opponent's TCP server
    public void startGameAsClient(String opponentIp, int tcpPort) {
        if (connected) return;  // Exit if already connected
    
        try {
            clientSocket = new Socket();
            System.out.println("Attempting to connect to opponent at " + opponentIp + ":" + tcpPort + "...");
            clientSocket.connect(new InetSocketAddress(opponentIp, tcpPort), 5000);  // Attempt connection with timeout
            System.out.println("Connected to opponent at " + opponentIp + ":" + tcpPort);
    
            GameLogic gameLogic = new GameLogic(clientSocket, false);  // Initialize game as Player 2
            gameLogic.start();  // Start the game
            connected = true;  // Update connection status
        } catch (SocketTimeoutException e) {
            System.err.println("Connection to opponent at " + opponentIp + ":" + tcpPort + " timed out.");
            System.out.println("Retrying matchmaking process...");
            startMatchmaking();  // Retry matchmaking if connection times out
        } catch (IOException e) {
            System.err.println("Unable to connect to opponent at " + opponentIp + ":" + tcpPort);
            System.out.println("Retrying matchmaking process...");
            startMatchmaking();  // Retry matchmaking if connection fails
        } finally {
            // Ensure client socket is closed if connection wasn't successful
            if (clientSocket != null && !clientSocket.isClosed()) {
                try {
                    clientSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    // Starts a TCP server to wait for incoming connections from an opponent
    public void startTcpServer() {
        if (connected) return;  // Exit if already connected
    
        try {
            serverSocket = new ServerSocket(tcpPort);  // Bind to the specified TCP port
            System.out.println("Waiting for opponent to connect on TCP port " + tcpPort + "...");
    
            serverSocket.setSoTimeout(TIMEOUT);  // Set timeout for accepting connections
            try {
                clientSocket = serverSocket.accept();  // Wait for incoming connection
                System.out.println("Opponent connected!");
    
                GameLogic gameLogic = new GameLogic(clientSocket, true);  // Initialize game as Player 1
                gameLogic.start();  // Start the game
                connected = true;  // Update connection status
            } catch (SocketTimeoutException e) {
                System.out.println("Timeout: No opponent connected within " + TIMEOUT / 1000 + " seconds.");
                System.out.println("Will retry matchmaking.");
            }
    
        } catch (IOException e) {
            e.printStackTrace();  // Handle any server socket issues
        } finally {
            if (serverSocket != null && !serverSocket.isClosed()) {
                try {
                    serverSocket.close();  // Ensure server socket is closed after use
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
