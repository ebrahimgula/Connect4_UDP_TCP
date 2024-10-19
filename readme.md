Connect4 Game - Ebrahim Gulamali 
c3264525

This project implements a network-based Connect4 game with three main Java classes: Connect4.java, GameLogic.java, and Matchmaker.java. The game allows clients to join, and the Matchmaker class monitors network messages to initiate new games.

How to Compile
Open a terminal in the directory where your .java files (Connect4.java, GameLogic.java, and Matchmaker.java) are located.

Compile all the Java files using the following command:

javac Connect4.java GameLogic.java Matchmaker.java

How to Execute
Running the Game
Once compiled, you can start the Connect4 game by running the following command, providing the broadcast address and UDP port as parameters:

java Connect4 <broadcast_address> <udp_port>
<broadcast_address>: The broadcast IP address for your network (e.g., 192.168.0.255).
<udp_port>: The UDP port number for communication (e.g., 9876).
Example:
java Connect4 192.168.0.255 9876

The Matchmaker class will continuously monitor the network for "NEW GAME" messages on the specified broadcast address and UDP port. If no message is received within 30 seconds, it will automatically send a "NEW GAME" message and loop back to monitoring.