import java.net.ServerSocket;
import java.io.IOException;

public class Connect4 {

   // Define the banner and separator as static final fields
   public static final String SEPARATOR = "\n" + "════════════════════════ˋˏ-༻❁༺-ˎˊ════════════════════════";
   
   // ANSI escape code for green
   public static final String GREEN_TEXT = "\u001B[32m";
   public static final String RESET_COLOR = "\u001B[0m";
   
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
         RESET_COLOR + // Reset to default color before separator
         SEPARATOR;

   public static void main(String[] args) {
       if (args.length != 2) {
           System.out.println("Usage: java Connect4 <broadcast_address> <broadcast_port>");
           System.exit(1);
       }

       // Display the banner
       System.out.println(BANNER);

       // Parse broadcast address and port from the command line
       String broadcastAddress = args[0];
       int broadcastPort = Integer.parseInt(args[1]);

       // Generate an available TCP port between 9000 and 9100
       int tcpPort = generateAvailablePort(9000, 9100);
       System.out.println("Using TCP Port: " + tcpPort);

       // Create Matchmaker instance and start matchmaking
       Matchmaker matchmaker = new Matchmaker(broadcastAddress, broadcastPort, tcpPort);
       matchmaker.startMatchmaking();
   }

   // Generates an available TCP port between the specified range.

   public static int generateAvailablePort(int minPort, int maxPort) {
       int port;
       while (true) {
           // Randomly generate a port within the range
           port = (int) (Math.random() * (maxPort - minPort + 1)) + minPort;
           if (isPortAvailable(port)) {
               return port;
           }
       }
   }

 
    //Checks if the given TCP port is available.
   public static boolean isPortAvailable(int port) {
       try (ServerSocket serverSocket = new ServerSocket(port)) {
           return true;  // Port is available
       } catch (IOException e) {
           return false;  // Port is already in use
       }
   }
}
