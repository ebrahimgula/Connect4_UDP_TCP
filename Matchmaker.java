import java.io.IOException;
import java.net.*;

public class Matchmaker {
    private static final String SEPARATOR = Connect4.SEPARATOR;  // Reuse separator
    private static final int TIMEOUT = 30000;

    private String broadcastAddress;
    private int udpPort;
    private int tcpPort;
    private boolean connected = false;
    private DatagramSocket udpSocket = null;
    private ServerSocket serverSocket = null;
    private Socket clientSocket = null;

    public Matchmaker(String broadcastAddress, int udpPort, int tcpPort) {
        this.broadcastAddress = broadcastAddress;
        this.udpPort = udpPort;
        this.tcpPort = tcpPort;
    }

    public void startMatchmaking() {
        System.out.println("Starting matchmaking process...");
        boolean gameFound = false;

        while (!gameFound) {
            try {
                gameFound = listenForUdpMessage();
                
                // If no "NEW GAME" message was found, send one and try again
                if (!gameFound) {
                    System.out.println("No 'NEW GAME' message received, sending my own.");
                    sendUdpBroadcast();
                    Thread.sleep(TIMEOUT); // Wait for 30 seconds before checking again
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void sendUdpBroadcast() {
        if (connected) return;

        try {
            udpSocket = new DatagramSocket();
            InetAddress broadcastAddr = InetAddress.getByName(broadcastAddress);
            udpSocket.setBroadcast(true);

            String message = "NEW GAME:" + tcpPort;
            DatagramPacket packet = new DatagramPacket(message.getBytes(), message.length(), broadcastAddr, udpPort);
            udpSocket.send(packet);

            System.out.println(SEPARATOR);
            System.out.println("Sent 'NEW GAME' message with TCP port " + tcpPort + " to " + broadcastAddress + ":" + tcpPort);
            startTcpServer();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (udpSocket != null && !udpSocket.isClosed()) {
                udpSocket.close();
            }
        }
    }

    public boolean listenForUdpMessage() throws IOException {
        if (connected) return true;

        try {
            udpSocket = new DatagramSocket(udpPort);
            byte[] buffer = new byte[256];
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

            udpSocket.setSoTimeout(TIMEOUT);
            System.out.println(SEPARATOR);
            System.out.println("Listening for 'NEW GAME' messages on UDP port " + udpPort + "...");
            udpSocket.receive(packet);

            String receivedMessage = new String(packet.getData(), 0, packet.getLength());
            System.out.println("Received message: " + receivedMessage);

            if (receivedMessage.startsWith("NEW GAME:")) {
                int opponentTcpPort = Integer.parseInt(receivedMessage.split(":")[1]);
                String opponentIp = packet.getAddress().getHostAddress();

                System.out.println("Connecting to opponent at " + opponentIp + ":" + opponentTcpPort);
                startGameAsClient(opponentIp, opponentTcpPort);
                return true; // Successfully found an opponent
            }
        } catch (SocketTimeoutException e) {
            System.out.println("No 'NEW GAME' message received within " + TIMEOUT / 1000 + " seconds. Broadcasting...");
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (udpSocket != null && !udpSocket.isClosed()) {
                udpSocket.close();
            }
        }
        return false; // No opponent found, continue the process
    }

    public void startGameAsClient(String opponentIp, int tcpPort) {
        if (connected) return;

        try {
            clientSocket = new Socket();
            clientSocket.connect(new InetSocketAddress(opponentIp, tcpPort), 5000);
            System.out.println("Connected to opponent at " + opponentIp + ":" + tcpPort);

            GameLogic gameLogic = new GameLogic(clientSocket, false);
            gameLogic.start();
            connected = true;  // Set connected to true here
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Unable to connect to opponent at " + opponentIp + ":" + tcpPort);
        }
    }

    public void startTcpServer() {
        if (connected) return;

        try {
            serverSocket = new ServerSocket(tcpPort);
            System.out.println("Waiting for opponent to connect on TCP port " + tcpPort + "...");
            clientSocket = serverSocket.accept();
            System.out.println("Opponent connected!");
            System.out.println("\n");

            GameLogic gameLogic = new GameLogic(clientSocket, true);
            gameLogic.start();
            connected = true;  // Set connected to true here
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
