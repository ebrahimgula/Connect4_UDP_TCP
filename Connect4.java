public class Connect4 {

   // Define the banner and separator as static final fields
   public static final String SEPARATOR = "\n" + "════════════════════════ˋˏ-༻❁༺-ˎˊ════════════════════════";
   public static final String BANNER = 
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

       // Randomly generate a TCP port between 9000 and 9100
       int tcpPort = (int) (Math.random() * 101) + 9000;

       // Create Matchmaker instance and start matchmaking
       Matchmaker matchmaker = new Matchmaker(broadcastAddress, broadcastPort, tcpPort);
       matchmaker.startMatchmaking();
   }
}
