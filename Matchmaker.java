import java.io.IOException;
import java.net.*;

public class Matchmaker {
    private static final String SEPARATOR = "════════════════════════ˋˏ-༻❁༺-ˎˊ════════════════════════";
    private static final int TIMEOUT = 30000;  // 30 seconds timeout to listen for NEW GAME messages

    private String broadcastAddress;
    private int udpPort;  // This is the fixed UDP port specified by the user
    private int tcpPort;  // This is the randomly generated TCP port for game connection
    private boolean connected = false;  // Tracks whether a connection has been established
    private DatagramSocket udpSocket = null;  // UDP Socket for broadcasting
    private ServerSocket serverSocket = null;  // TCP Server Socket
    private Socket clientSocket = null;  // TCP Client Socket

    public Matchmaker(String broadcastAddress, int udpPort, int tcpPort) {
        this.broadcastAddress = broadcastAddress;
        this.udpPort = udpPort;  // UDP port specified by user
        this.tcpPort = tcpPort;  // This is the randomly generated TCP port for game connection
    }

    // Start matchmaking process
    public void startMatchmaking() {
        displayBanner();
        System.out.println("Searching for opponents...");

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

        try {
            udpSocket = new DatagramSocket();
            InetAddress broadcastAddr = InetAddress.getByName(broadcastAddress);
            udpSocket.setBroadcast(true);

            // Send the random TCP port in the UDP message
            String message = "NEW GAME:" + tcpPort;

            // Send the message over UDP to the port specified by user
            DatagramPacket packet = new DatagramPacket(message.getBytes(), message.length(), broadcastAddr, udpPort);
            udpSocket.send(packet);

            // Notify what is being sent
            System.out.println("\n" + SEPARATOR);
            System.out.println("Sent 'NEW GAME' message with TCP port " + tcpPort + " to " + broadcastAddress + ":" + udpPort);

            // Start listening for a connection on the TCP port as Player 1 (the server)
            startTcpServer();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            // Ensure UDP socket is closed
            if (udpSocket != null && !udpSocket.isClosed()) {
                udpSocket.close();
            }
        }
    }

    // Listen for UDP broadcast messages to connect to the opponent
    public void listenForUdpMessage() throws IOException {
        if (connected) return;  // Stop listening after the connection is established

        try {
            udpSocket = new DatagramSocket(udpPort);  // Listen on the UDP port
            byte[] buffer = new byte[256];
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

            udpSocket.setSoTimeout(TIMEOUT);  // Set the socket to listen for 30 seconds
            System.out.println("\n" + SEPARATOR);
            System.out.println("Listening for 'NEW GAME' messages on UDP port " + udpPort + " for " + TIMEOUT / 1000 + " seconds...");
            udpSocket.receive(packet);  // Receive the UDP message

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
                } else {
                    System.out.println("Invalid 'NEW GAME' message format.");
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
        } finally {
            // Ensure UDP socket is closed
            if (udpSocket != null && !udpSocket.isClosed()) {
                udpSocket.close();
            }
        }
    }

    // Method to start the game as the client and connect to the opponent
    public void startGameAsClient(String opponentIp, int tcpPort) {
        if (connected) return;  // Do not try to reconnect if already connected

        try {
            clientSocket = new Socket();
            clientSocket.connect(new InetSocketAddress(opponentIp, tcpPort), 5000);  // 5-second timeout
            System.out.println("Connected to opponent at " + opponentIp + ":" + tcpPort);
            System.out.println("\n" + SEPARATOR);

            // Start the game logic as client (Player 2)
            GameLogic gameLogic = new GameLogic(clientSocket, false);  // False means this is Player 2
            gameLogic.start();
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Unable to connect to opponent at " + opponentIp + ":" + tcpPort);
        } finally {
            // Ensure client socket is closed after the game finishes
            if (clientSocket != null && !clientSocket.isClosed()) {
                try {
                    clientSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    // Method to start a TCP server and listen for incoming connections
    public void startTcpServer() {
        if (connected) return;  // Do not start a server if already connected

        try {
            serverSocket = new ServerSocket(tcpPort);
            System.out.println("Waiting for opponent to connect on TCP port " + tcpPort + "...");
            clientSocket = serverSocket.accept();  // Wait for an opponent to connect
            System.out.println("Opponent connected!");
            System.out.println("\n" + SEPARATOR);

            // Start the game logic as server (Player 1)
            GameLogic gameLogic = new GameLogic(clientSocket, true);  // True means this is Player 1
            gameLogic.start();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            // Ensure server and client sockets are closed after the game finishes
            if (serverSocket != null && !serverSocket.isClosed()) {
                try {
                    serverSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (clientSocket != null && !clientSocket.isClosed()) {
                try {
                    clientSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    // Banner to display when the program starts
    private void displayBanner() {
        System.out.println("                                                                                              ");
        System.out.println("  .g8\"\"\"bgd   .g8\"\"8q. `7MN.   `7MF`7MN.   `7MF`7MM\"\"\"YMM    .g8\"\"\"bgd MMP\"\"MM\"\"YMM      ");
        System.out.println(".dP'     `M .dP'    `YM. MMN.    M   MMN.    M   MM    `7  .dP'     `M P'   MM   `7      ");
        System.out.println("dM'       ` dM'      `MM M YMb   M   M YMb   M   MM   d    dM'       `      MM      ,AM  ");
        System.out.println("MM          MM        MM M  `MN. M   M  `MN. M   MMmmMM    MM               MM     AVMM  ");
        System.out.println("MM.         MM.      ,MP M   `MM.M   M   `MM.M   MM   Y  , MM.              MM   ,W' MM  ");
        System.out.println("`Mb.     ,' `Mb.    ,dP' M     YMM   M     YMM   MM     ,M `Mb.     ,'      MM ,W'   MM  ");
        System.out.println("  `\"bmmmd'    `\"bmmd\"' .JML.    YM .JML.    YM .JMMmmmmMMM   `\"bmmmd'     .JMMLAmmmmmMMmm");
        System.out.println("                                                                                     MM  ");
        System.out.println("                                                                                     MM  ");
        System.out.println(SEPARATOR);
    }
}
