public class Connect4 {
   public static void main(String[] var0) {
      if (var0.length != 2) {
         System.out.println("Usage: java Connect4 <broadcast_address> <broadcast_port>");
         System.exit(1);
      }

      // Parse broadcast address and port from the command line
      String broadcastAddress = var0[0];
      int broadcastPort = Integer.parseInt(var0[1]);

      // Randomly generate a TCP port between 9000 and 9100
      int tcpPort = (int)(Math.random() * 101) + 9000;

      // Create Matchmaker instance and start matchmaking
      Matchmaker matchmaker = new Matchmaker(broadcastAddress, broadcastPort, tcpPort);
      matchmaker.startMatchmaking();
   }
}
