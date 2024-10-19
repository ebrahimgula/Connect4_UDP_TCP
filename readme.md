# Connect4 Game - Ebrahim Gulamali 
c3264525

This project implements a network-based Connect4 game with three main Java classes: Connect4.java, GameLogic.java, and Matchmaker.java. The game allows clients to join, and the Matchmaker class monitors network messages to initiate new games.

## How to Compile
Open a terminal in the directory where your .java files (Connect4.java, GameLogic.java, and Matchmaker.java) are located.

Compile all the Java files using the following command:

javac Connect4.java GameLogic.java Matchmaker.java

## How to Execute
Running the Game
Once compiled, you can start the Connect4 game by running the following command, providing the broadcast address and UDP port as parameters:

java Connect4 <broadcast_address> <udp_port>
<broadcast_address>: The broadcast IP address for your network (e.g., 192.168.0.255).
<udp_port>: The UDP port number for communication (e.g., 9876).

## Example:
java Connect4 192.168.0.255 9876

The Matchmaker class will continuously monitor the network for "NEW GAME" messages on the specified broadcast address and UDP port. If no message is received within 30 seconds, it will automatically send a "NEW GAME" message and loop back to monitoring.

## Assumptions

### Network Configuration:

The program assumes that all clients are on the same local network and can communicate via the provided broadcast address and port. It is expected that firewalls and network configurations do not block UDP broadcast or TCP connections on ports 9000-9100.

### UDP Message Transmission:

It is assumed that during the matchmaking phase, UDP messages will be broadcast without significant packet loss or delays. If no message is received within the 30-second window, the program retries as specified.

### Single Code Base for Both Players:

The program assumes that both players are running the same codebase, and the roles of Player 1 and Player 2 are determined based on the receipt of the “NEW GAME” message.

### User Input:

The program assumes the user will enter a valid column number (within the 1-7 range) when prompted to select a column. Invalid input handling is in place for numbers out of range, but it assumes no further malformed input such as letters or symbols.

### Game Display:

The game grid is printed to the terminal after each move. The program assumes a terminal interface where the display of the grid will be readable without any issues, and the user can see the game’s progression.