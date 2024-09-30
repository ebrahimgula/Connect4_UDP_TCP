import java.io.IOException;
import java.net.*;

public class Matchmaker {
    private static final String SEPARATOR = "════════════════════════ˋˏ-༻❁༺-ˎˊ════════════════════════";
    private static final int TIMEOUT = 30000;  // 30 seconds timeout for UDP listening

    private String broadcastAddress;
    private int udpPort;  // The fixed UDP port for broadcasting
    private int tcpPort;  // Randomly generated TCP port for the game connection
    private boolean connected = false;  // To track if the connection is established

    public Matchmaker(String broadcastAddress, int udpPort, int tcpPort) {
        this.broadcastAddress = broadcastAddress;
        this.udpPort = udpPort;  // Fixed UDP port for broadcast (e.g., 50001)
        this.tcpPort = tcpPort;  // Randomly generated TCP port (9000-9100)
    }

    // Banner to display when the program starts and ends
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

    // Start the matchmaking process
    public void startMatchmaking() {
        displayBanner();
        System.out.println("Searching for opponents...");

        try {
            listenForUdpMessage();
        } catch (SocketTimeoutException e) {
            System.out.println("Timeout: No 'NEW GAME' message received within " + TIMEOUT / 1000 + " seconds.");
            System.out.println("\n" + SEPARATOR);  // Insert separator here
            System.out.println("Broadcasting 'NEW GAME' message...");
            sendUdpBroadcast();
        } catch (IOException e) {
            System.out.println("Error occurred during matchmaking: " + e.getMessage());
        }
    }

    // Listen for UDP broadcast messages from other players
    private void listenForUdpMessage() throws IOException {
        if (connected) return;

        try (DatagramSocket udpSocket = new DatagramSocket(udpPort)) {
            udpSocket.setSoTimeout(TIMEOUT);
            byte[] buffer = new byte[256];
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

            System.out.println("\n" + SEPARATOR);
            System.out.println("Listening for 'NEW GAME' messages on UDP port " + udpPort + "...");
            udpSocket.receive(packet);  // Listen for incoming messages

            String receivedMessage = new String(packet.getData(), 0, packet.getLength());
            System.out.println("Received message: " + receivedMessage);

            if (receivedMessage.startsWith("NEW GAME:")) {
                int opponentTcpPort = Integer.parseInt(receivedMessage.split(":")[1]);
                String opponentIp = packet.getAddress().getHostAddress();

                System.out.println("\n" + SEPARATOR);
                System.out.println("Opponent found! Connecting to " + opponentIp + ":" + opponentTcpPort + "...");
                startGameAsClient(opponentIp, opponentTcpPort);  // Act as Player 2 (client)
                connected = true;
            }
        } catch (SocketTimeoutException e) {
            throw new SocketTimeoutException("Timeout waiting for UDP messages.");
        }
    }

    // Send UDP broadcast with the player's TCP port to invite others to connect
    private void sendUdpBroadcast() {
        if (connected) return;

        try (DatagramSocket udpSocket = new DatagramSocket()) {
            InetAddress broadcastAddr = InetAddress.getByName(broadcastAddress);
            String message = "NEW GAME:" + tcpPort;

            DatagramPacket packet = new DatagramPacket(message.getBytes(), message.length(), broadcastAddr, udpPort);
            udpSocket.send(packet);

            System.out.println("Sent 'NEW GAME' message with TCP port " + tcpPort);
            System.out.println("Waiting for an opponent to connect on TCP port " + tcpPort + "...");
            startTcpServer();
        } catch (IOException e) {
            System.out.println("Error sending UDP broadcast: " + e.getMessage());
        }
    }

    // Start the game as a client (Player 2) and connect to the opponent's TCP port
    private void startGameAsClient(String opponentIp, int opponentTcpPort) {
        if (connected) return;

        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(opponentIp, opponentTcpPort), 5000);  // 5-second timeout for TCP connection
            System.out.println("Connected to opponent at " + opponentIp + ":" + opponentTcpPort + "!");
            System.out.println("\n" + SEPARATOR);

            GameLogic gameLogic = new GameLogic(socket, false);  // Player 2 (false)
            gameLogic.start();
        } catch (SocketTimeoutException e) {
            System.out.println("Connection to opponent timed out.");
        } catch (IOException e) {
            System.out.println("Unable to connect to opponent at " + opponentIp + ":" + opponentTcpPort + ": " + e.getMessage());
        }
    }

    // Start a TCP server for the opponent to connect (Player 1)
    private void startTcpServer() {
        if (connected) return;

        try (ServerSocket serverSocket = new ServerSocket(tcpPort)) {
            System.out.println("Waiting for opponent to connect on TCP port " + tcpPort + "...");
            Socket clientSocket = serverSocket.accept();  // Wait for Player 2 to connect
            System.out.println("Opponent connected!");
            System.out.println("\n" + SEPARATOR);

            GameLogic gameLogic = new GameLogic(clientSocket, true);  // Player 1 (true)
            gameLogic.start();
            connected = true;
        } catch (IOException e) {
            System.out.println("Error setting up TCP server: " + e.getMessage());
        }
    }

    // Call this when the program ends
    public void endProgram() {
        System.out.println("\n" + SEPARATOR);
        System.out.println("Game over. Thanks for playing!");
        displayBanner();
    }
}
