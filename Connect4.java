import java.net.ServerSocket;
import java.io.IOException;

public class Connect4 {

   // Define a separator for console output formatting
   public static final String SEPARATOR = "\n" + "════════════════════════ˋˏ-༻❁༺-ˎˊ════════════════════════";
   
   // ANSI escape codes for styling console text in green
   public static final String GREEN_TEXT = "\u001B[32m";  // Green text start code
   public static final String RESET_COLOR = "\u001B[0m";  // Reset text color to default
   
   // Banner for the game introduction, displayed in green
   public static final String BANNER = 
         GREEN_TEXT + // Start green text
         "                                                                                              \n" +
         "  .g8\"\"\"bgd   .g8\"\"8q. `7MN.   `7MF`7MN.   `7MF`7MM\"\"\"YMM    .g8\"\"\"bgd MMP\"\"MM\"\"YMM      \n" +
         ".dP'     `M .dP'    `YM. MMN.    M   MMN.    M   MM    `7  .dP'     `M P'   MM   `7      \n" +
         "dM'       ` dM'      `MM M YMb   M   M YMb   M   MM   d    dM'       `      MM      ,AM  \n" +
         "MM          MM        MM M  `MN. M   M  `MN. M   MMmmMM    MM               MM     AVMM  \n" +
         "MM.         MM.      ,MP M   `MM.M   M   `MM.M   MM   Y  , MM.              MM   ,W' MM  \n" +
         "`Mb.     ,' `Mb.    ,dP' M     YMM   M     YMM   MM     ,M `Mb.     ,'      MM ,W'   MM  \n" +
         "  `\"bmmmd'    `\"bmmd\"' .JML.    YM .JML.    YM .JMMmmmmMMM   `\"bmmmd'     .JMMLAmmmmmMMmm\n" +
         "                                                                                     MM  \n" +
         "                                                                                     MM  \n" +
         RESET_COLOR + // Reset text color after banner
         SEPARATOR; // End with separator for formatting

   // Entry point for the Connect4 program
   public static void main(String[] args) {
       // Check if correct number of arguments provided
       if (args.length != 2) {
           System.out.println("Usage: java Connect4 <broadcast_address> <broadcast_port>");
           System.exit(1); // Exit if incorrect arguments
       }

       // Display the game banner
       System.out.println(BANNER);

       // Parse command-line arguments for broadcast address and port
       String broadcastAddress = args[0];
       int broadcastPort = Integer.parseInt(args[1]);

       // Generate a random, available TCP port within the specified range
       int tcpPort = generateAvailablePort(9000, 9100);
       System.out.println("Using TCP Port: " + tcpPort);  // Display selected port

       // Create a Matchmaker instance to handle matchmaking and start the process
       Matchmaker matchmaker = new Matchmaker(broadcastAddress, broadcastPort, tcpPort);
       matchmaker.startMatchmaking();  // Begin matchmaking process
   }

   // Generates an available TCP port between a specified range
   public static int generateAvailablePort(int minPort, int maxPort) {
       int port;
       while (true) {
           // Randomly select a port within the specified range
           port = (int) (Math.random() * (maxPort - minPort + 1)) + minPort;
           if (isPortAvailable(port)) {  // Check if the port is free
               return port;  // Return the port if available
           }
       }
   }

   // Checks if a specific TCP port is available for use
   public static boolean isPortAvailable(int port) {
       // Try to open a ServerSocket on the specified port
       try (ServerSocket serverSocket = new ServerSocket(port)) {
           return true;  // Port is available if no exception occurs
       } catch (IOException e) {
           return false;  // Port is in use if exception occurs
       }
   }
}
