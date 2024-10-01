import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;

public class Matchmaker {
    private String broadcastAddress;
    private int udpPort;  // This is the fixed UDP port (50001)
    private int tcpPort;  // This is the randomly generated TCP port for game connection
    private static final int TIMEOUT = 30000;  // 30 seconds timeout to listen for NEW GAME messages
    private boolean connected = false;  // Tracks whether a connection has been established
    
    public Matchmaker(String broadcastAddress, int udpPort, int tcpPort) {
        this.broadcastAddress = broadcastAddress;
        this.udpPort = udpPort;  // UDP port that is specified by user
        this.tcpPort = tcpPort;  // This is the randomly generated TCP port for game connection
    }

    // Start matchmaking process
    public void startMatchmaking() {
        System.out.println("Looking for 'NEW GAME' messages...");

        try {
            // Listen for incoming "NEW GAME" messages for 30 seconds, or send a "NEW GAME" message if none are received
            listenForUdpMessage();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Send UDP broadcast with TCP port to inform others that a new game is available
    public void sendUdpBroadcast() {
        if (connected) return;  // Stop broadcasting after the connection is established
        
        try (DatagramSocket udpSocket = new DatagramSocket()) {
            InetAddress broadcastAddr = InetAddress.getByName(broadcastAddress);

            // Send the random TCP port in the UDP message
            String message = "NEW GAME:" + tcpPort;  // Add the random TCP port in the message

            // Send the message over UDP to the port specified by user
            DatagramPacket packet = new DatagramPacket(message.getBytes(), message.length(), broadcastAddr, udpPort);
            udpSocket.send(packet);

            // Notify what is being sent
            System.out.println("Sent 'NEW GAME' message with TCP port " + tcpPort + " to " + broadcastAddress + ":" + udpPort);

            // Start listening for a connection on the TCP port as Player 1 (the server)
            startTcpServer();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Listen for UDP broadcast messages to connect to the opponent
    public void listenForUdpMessage() throws IOException {
        if (connected) return;  // Stop listening after the connection is established
        
        try (DatagramSocket udpSocket = new DatagramSocket(udpPort)) {  // Listen on the UDP port 
            byte[] buffer = new byte[256];
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

            udpSocket.setSoTimeout(TIMEOUT);  // Set the socket to listen for 30 seconds
            System.out.println("Listening for 'NEW GAME' messages on UDP port " + udpPort + "...");
            udpSocket.receive(packet);

            // Convert the message into a string and parse the TCP port
            String receivedMessage = new String(packet.getData(), 0, packet.getLength());
            System.out.println("Received message: " + receivedMessage);

            if (receivedMessage.startsWith("NEW GAME:")) {
                String[] parts = receivedMessage.split(":");
                if (parts.length == 2) {
                    int opponentTcpPort = Integer.parseInt(parts[1]);  // Extract the TCP port from the message
                    String opponentIp = packet.getAddress().getHostAddress();

                    System.out.println("Connecting to opponent at " + opponentIp + ":" + opponentTcpPort);
                    startGameAsClient(opponentIp, opponentTcpPort);  // Act as client and connect to the opponent

                    connected = true;  // Mark the connection as established
                }
            } else {
                System.out.println("Invalid message received: " + receivedMessage);
            }
        } catch (SocketTimeoutException e) {
            // No "NEW GAME" message received within the timeout, so Player 1 broadcasts the game
            System.out.println("No 'NEW GAME' message received within " + TIMEOUT / 1000 + " seconds. Broadcasting...");
            sendUdpBroadcast();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Method to start the game as the client and connect to the opponent
    public void startGameAsClient(String opponentIp, int tcpPort) {
        if (connected) return;  // Do not try to reconnect if already connected
        
        try {
            Socket socket = new Socket(opponentIp, tcpPort);  // Establish a TCP connection
            System.out.println("Connected to opponent at " + opponentIp + ":" + tcpPort);

            // Start the game logic as client (Player 2)
            GameLogic gameLogic = new GameLogic(socket, false);  // False means this is Player 2
            gameLogic.start();
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Unable to connect to opponent at " + opponentIp + ":" + tcpPort);
        }
    }

    // Method to start a TCP server and listen for incoming connections
    public void startTcpServer() {
        if (connected) return;  // Do not start a server if already connected
        
        try (ServerSocket serverSocket = new ServerSocket(tcpPort)) {
            System.out.println("Waiting for opponent to connect on TCP port " + tcpPort + "...");
            
            // Wait for an opponent to connect
            Socket clientSocket = serverSocket.accept();
            System.out.println("Opponent connected!");

            // Start the game logic as server (Player 1)
            GameLogic gameLogic = new GameLogic(clientSocket, true);  // True means this is Player 1
            gameLogic.start();
            connected = true;  // Mark the connection as established
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
