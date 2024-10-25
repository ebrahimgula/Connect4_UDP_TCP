import java.io.IOException;
import java.net.*;

public class Matchmaker {
    private static final String SEPARATOR = Connect4.SEPARATOR;  // Reuse separator
    private static final int TIMEOUT = 30000;  // Adjusted timeout to 30 seconds for retrying

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

                    // Wait for some time for a connection to be established
                    Thread.sleep(TIMEOUT);

                    // If still no connection, go back to listening on UDP
                    if (!connected) {
                        System.out.println("No connection established. Returning to UDP listening...");
                    }
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

            // Start the TCP server and wait for a connection
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
                return true;  // Successfully found an opponent
            }
        } catch (SocketTimeoutException e) {
            System.out.println("No 'NEW GAME' message received within " + TIMEOUT / 1000 + " seconds.");
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (udpSocket != null && !udpSocket.isClosed()) {
                udpSocket.close();
            }
        }
        return false;  // No opponent found, continue the process
    }

    public void startGameAsClient(String opponentIp, int tcpPort) {
        if (connected) return;
    
        try {
            clientSocket = new Socket();
            System.out.println("Attempting to connect to opponent at " + opponentIp + ":" + tcpPort + "...");
            // Attempt to connect with a timeout
            clientSocket.connect(new InetSocketAddress(opponentIp, tcpPort), 5000);
            System.out.println("Connected to opponent at " + opponentIp + ":" + tcpPort);
    
            GameLogic gameLogic = new GameLogic(clientSocket, false);
            gameLogic.start();
            connected = true;  // Set connected to true here
        } catch (SocketTimeoutException e) {
            // Handle the connection timeout gracefully
            System.err.println("Connection to opponent at " + opponentIp + ":" + tcpPort + " timed out.");
            System.out.println("Retrying matchmaking process...");
            // Return to the matchmaking process
            startMatchmaking();  // You can retry matchmaking here or handle retries with a count
        } catch (IOException e) {
            // Handle other IO exceptions (such as unreachable host)
            e.printStackTrace();
            System.err.println("Unable to connect to opponent at " + opponentIp + ":" + tcpPort);
            System.out.println("Retrying matchmaking process...");
            // Return to the matchmaking process
            startMatchmaking();  // Retry matchmaking or handle appropriately
        } finally {
            // Ensure the socket is closed if it wasn't connected
            if (clientSocket != null && !clientSocket.isClosed()) {
                try {
                    clientSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
    

    public void startTcpServer() {
        if (connected) return;
    
        try {
            serverSocket = new ServerSocket(tcpPort);
            System.out.println("Waiting for opponent to connect on TCP port " + tcpPort + "...");
    
            // Listen for connections, but only for the specified timeout period
            serverSocket.setSoTimeout(TIMEOUT); // You can set it to 30 seconds or more
            try {
                clientSocket = serverSocket.accept();
                System.out.println("Opponent connected!");
    
                GameLogic gameLogic = new GameLogic(clientSocket, true);
                gameLogic.start();
                connected = true;  // Set connected to true here
            } catch (SocketTimeoutException e) {
                System.out.println("Timeout: No opponent connected within " + TIMEOUT / 1000 + " seconds.");
                System.out.println("Will retry matchmaking.");
                // Close the server socket and return to matchmaking
            }
    
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (serverSocket != null && !serverSocket.isClosed()) {
                try {
                    serverSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
    
}
