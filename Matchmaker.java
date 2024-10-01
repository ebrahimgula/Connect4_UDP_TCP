import java.io.IOException;
import java.net.*;

public class Matchmaker {
    private static final String SEPARATOR = "════════════════════════ˋˏ-༻❁༺-ˎˊ════════════════════════";
    private static final int TIMEOUT = 30000;  // 30 seconds timeout to listen for NEW GAME messages

    private String broadcastAddress;
    private int udpPort;  // This is the fixed UDP port specified by the user
    private int tcpPort;  // This is the randomly generated TCP port for game connection
    private boolean connected = false;  // Tracks whether a connection has been established

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
            udpSocket.setBroadcast(true);

            String message = "NEW GAME:" + tcpPort;

            DatagramPacket packet = new DatagramPacket(message.getBytes(), message.length(), broadcastAddr, udpPort);
            udpSocket.send(packet);

            System.out.println("\n" + SEPARATOR);
            System.out.println("Sent 'NEW GAME' message with TCP port " + tcpPort + " to " + broadcastAddress + ":" + udpPort);

            startTcpServer();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Listen for UDP broadcast messages to connect to the opponent
    public void listenForUdpMessage() throws IOException {
        if (connected) return;

        try (DatagramSocket udpSocket = new DatagramSocket(udpPort)) {  
            byte[] buffer = new byte[256];
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

            udpSocket.setSoTimeout(TIMEOUT);  
            System.out.println("\n" + SEPARATOR);
            System.out.println("Listening for 'NEW GAME' messages on UDP port " + udpPort + " for " + TIMEOUT / 1000 + " seconds...");
            udpSocket.receive(packet);

            String receivedMessage = new String(packet.getData(), 0, packet.getLength());
            System.out.println("Received message: " + receivedMessage);

            if (receivedMessage.startsWith("NEW GAME:")) {
                String[] parts = receivedMessage.split(":");
                if (parts.length == 2) {
                    int opponentTcpPort = Integer.parseInt(parts[1]);  
                    String opponentIp = packet.getAddress().getHostAddress();

                    System.out.println("Connecting to opponent at " + opponentIp + ":" + opponentTcpPort);
                    startGameAsClient(opponentIp, opponentTcpPort);  

                    connected = true;  
                } else {
                    System.out.println("Invalid 'NEW GAME' message format.");
                }
            } else {
                System.out.println("Invalid message received: " + receivedMessage);
            }
        } catch (SocketTimeoutException e) {
            System.out.println("No 'NEW GAME' message received within " + TIMEOUT / 1000 + " seconds. Broadcasting...");
            sendUdpBroadcast();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Method to start the game as the client and connect to the opponent
    public void startGameAsClient(String opponentIp, int tcpPort) {
        if (connected) return;  

        try {
            Socket socket = new Socket();
            socket.connect(new InetSocketAddress(opponentIp, tcpPort), 5000);  
            System.out.println("Connected to opponent at " + opponentIp + ":" + tcpPort);

            // Removed duplicate separator print here
            // Start the game logic as client (Player 2)
            GameLogic gameLogic = new GameLogic(socket, false);  
            gameLogic.start();
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Unable to connect to opponent at " + opponentIp + ":" + tcpPort);
        }
    }

    // Method to start a TCP server and listen for incoming connections
    public void startTcpServer() {
        if (connected) return;  

        try (ServerSocket serverSocket = new ServerSocket(tcpPort)) {
            System.out.println("Waiting for opponent to connect on TCP port " + tcpPort + "...");
            Socket clientSocket = serverSocket.accept();  
            System.out.println("Opponent connected!");

            // Removed duplicate separator print here
            GameLogic gameLogic = new GameLogic(clientSocket, true);  
            gameLogic.start();
        } catch (IOException e) {
            e.printStackTrace();
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
