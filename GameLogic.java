import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketException;
import java.util.Arrays;
import java.util.Scanner;

public class GameLogic {
    // Constants for UI components
    private static final String SEPARATOR = Connect4.SEPARATOR;
    private static final String BANNER = Connect4.BANNER;
    
    private Socket socket; // Socket for communication with opponent
    private boolean isPlayerOne; // Flag to determine if this player is Player One
    private char[][] board; // 6x7 board for Connect 4
    private BufferedReader in; // Input stream to read opponent's messages
    private PrintWriter out; // Output stream to send messages to opponent
    private Scanner scanner; // Scanner to read user input

    // Constructor initializes the board and sets up the player
    public GameLogic(Socket socket, boolean isPlayerOne) {
        this.socket = socket;
        this.isPlayerOne = isPlayerOne;
        this.board = new char[6][7]; // Set up a 6-row, 7-column board
        Arrays.stream(board).forEach(row -> Arrays.fill(row, '.')); // Fill the board with empty slots (.)
        this.scanner = new Scanner(System.in);
    }

    // Starts the game loop
    public void start() {
        try {
            // Set up input and output streams for communication
            this.in = new BufferedReader(new InputStreamReader(this.socket.getInputStream()));
            this.out = new PrintWriter(this.socket.getOutputStream(), true);
            boolean gameEnded = false;

            // Game loop continues until a win or draw occurs
            while (!gameEnded) {
                if (this.isPlayerOne) {
                    // Player One's turn
                    playerTurn('X');
                    gameEnded = checkWin('X') || checkDraw(); // Check for win or draw
                    if (gameEnded) break;
                    
                    // Opponent's turn
                    opponentTurn('O');
                    gameEnded = checkWin('O') || checkDraw();
                } else {
                    // Player Two's turn
                    opponentTurn('X');
                    gameEnded = checkWin('X') || checkDraw();
                    if (gameEnded) break;

                    // Player's turn
                    playerTurn('O');
                    gameEnded = checkWin('O') || checkDraw();
                }
            }
        } catch (IOException e) {
            System.out.println("ERROR: Connection issue."); // Handle any I/O error during the game
            e.printStackTrace();
        } finally {
            cleanup(); // Clean up resources after the game
        }
    }

    // Handles the player's move
    private void playerTurn(char playerChar) throws IOException {
        displayBoard();
        System.out.println(SEPARATOR);
        System.out.print("Your turn. Enter column (1-7): ");
    
        int col;
        while (true) {
            try {
                col = Integer.parseInt(this.scanner.nextLine()) - 1; // Read and convert column input
                if (isValidMove(col)) {
                    makeMove(col, playerChar); // Place move on board
                    break;
                }
                System.out.print("Invalid move. Try again: ");
            } catch (NumberFormatException e) {
                System.out.print("Please enter a valid column number (1-7): ");
            }
        }
    
        // Send the move to the opponent
        this.out.println("INSERT:" + (col + 1));
    
        // Check if the move results in a win; wait for opponent's "YOU WIN" confirmation if true
        if (checkWin(playerChar)) {
            displayBoard();
            System.out.println(SEPARATOR);
            System.out.println("You win the game! Waiting for opponent confirmation...");
    
            try {
                // Wait for "YOU WIN" confirmation from opponent
                String confirmation = this.in.readLine();
                if ("YOU WIN".equals(confirmation)) {
                    System.out.println("Opponent confirmed your win. Game over.");
                    gameOver(); // Close game upon confirmation
                } else {
                    System.out.println("ERROR: Unexpected response from opponent.");
                    gameOver();
                }
            } catch (SocketException e) {
                System.err.println("Connection error: Unable to receive confirmation from opponent.");
                gameOver(); // Handle connection error gracefully
            }
        }
    }

    // Handles the opponent's turn
    private void opponentTurn(char opponentChar) throws IOException {
        System.out.println(SEPARATOR);
        System.out.println("Waiting for opponent's move...");
    
        try {
            String receivedMessage = this.in.readLine();
            if (receivedMessage == null) {
                System.out.println("Opponent disconnected.");
                gameOver(); // End game if opponent disconnects
                return;
            }
    
            if (receivedMessage.startsWith("INSERT:")) {
                // Parse opponent's move
                int col = Integer.parseInt(receivedMessage.split(":")[1]) - 1;
                if (isValidMove(col)) {
                    makeMove(col, opponentChar); // Make move on board
    
                    // If opponent's move results in a win, send "YOU WIN" back and end game
                    if (checkWin(opponentChar)) {
                        displayBoard();
                        System.out.println(SEPARATOR);
                        System.out.println("You lose!");
                        this.out.println("YOU WIN"); // Acknowledge opponent's win
                        gameOver();
                    }
                } else {
                    System.out.println("ERROR: Invalid move from opponent.");
                    this.out.println("ERROR"); // Handle invalid moves gracefully
                    gameOver();
                }
            } else if (receivedMessage.equals("YOU WIN")) {
                // Opponent confirms win
                System.out.println("Opponent confirmed your win. Game over.");
                gameOver();
            } else {
                System.out.println("ERROR: Unexpected message from opponent.");
                this.out.println("ERROR");
                gameOver();
            }
        } catch (SocketException e) {
            System.err.println("Connection error: Unable to receive move from opponent.");
            gameOver(); // Handle connection error gracefully
        }
    }

    // Ends the game and performs cleanup
    private void gameOver() {
        System.out.println("Thanks for playing!");
        System.out.println(SEPARATOR);
        System.out.println(BANNER);
        cleanup();
        System.exit(0); // Exit the program after cleanup
    }

    // Closes the socket and scanner resources
    private void cleanup() {
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close(); // Close the socket connection
            }
            scanner.close(); // Close the input scanner
        } catch (IOException e) {
            e.printStackTrace(); // Handle any cleanup errors
        }
    }

    // Validates whether the chosen column is within bounds and not full
    private boolean isValidMove(int col) {
        return col >= 0 && col < 7 && board[0][col] == '.'; // Check if column is open
    }

    // Makes a move for a player by placing their symbol in the chosen column
    private void makeMove(int col, char playerChar) {
        for (int row = 5; row >= 0; row--) {
            if (board[row][col] == '.') {
                board[row][col] = playerChar; // Place player's symbol in first available row
                break;
            }
        }
    }

    // Checks if a player has won by checking horizontal, vertical, and diagonal lines
    private boolean checkWin(char playerChar) {
        return checkHorizontalWin(playerChar) || checkVerticalWin(playerChar) || checkDiagonalWin(playerChar);
    }

    // Checks for a horizontal win
    private boolean checkHorizontalWin(char playerChar) {
        for (int row = 0; row < 6; row++) {
            for (int col = 0; col < 4; col++) {
                if (board[row][col] == playerChar && board[row][col + 1] == playerChar &&
                    board[row][col + 2] == playerChar && board[row][col + 3] == playerChar) {
                    return true;
                }
            }
        }
        return false;
    }

    // Checks for a vertical win
    private boolean checkVerticalWin(char playerChar) {
        for (int col = 0; col < 7; col++) {
            for (int row = 0; row < 3; row++) {
                if (board[row][col] == playerChar && board[row + 1][col] == playerChar &&
                    board[row + 2][col] == playerChar && board[row + 3][col] == playerChar) {
                    return true;
                }
            }
        }
        return false;
    }

    // Checks for a diagonal win
    private boolean checkDiagonalWin(char playerChar) {
        // Check for descending diagonals
        for (int row = 3; row < 6; row++) {
            for (int col = 0; col < 4; col++) {
                if (board[row][col] == playerChar && board[row - 1][col + 1] == playerChar &&
                    board[row - 2][col + 2] == playerChar && board[row - 3][col + 3] == playerChar) {
                    return true;
                }
            }
        }
        // Check for ascending diagonals
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 4; col++) {
                if (board[row][col] == playerChar && board[row + 1][col + 1] == playerChar &&
                    board[row + 2][col + 2] == playerChar && board[row + 3][col + 3] == playerChar) {
                    return true;
                }
            }
        }
        return false;
    }

    // Checks if the board is full, indicating a draw
    private boolean checkDraw() {
        for (int col = 0; col < 7; col++) {
            if (board[0][col] == '.') {
                return false; // Not a draw if any top cell is empty
            }
        }
        displayBoard();
        System.out.println(SEPARATOR);
        System.out.println("The game is a draw!");
        this.out.println("DRAW"); // Inform opponent of the draw
        gameOver();
        return true;
    }

    // Displays the current state of the board with color coding
    private void displayBoard() {
        final String RESET = "\u001B[0m";
        final String RED = "\u001B[31m";   
        final String YELLOW = "\u001B[33m"; 

        System.out.println("\nCurrent Board:\n");
        System.out.println("  1   2   3   4   5   6   7 ");
        System.out.println("❁═══❁═══❁═══❁═══❁═══❁═══❁═══❁");
    
        for (char[] row : board) {
            System.out.print("| ");
            for (char slot : row) {
                if (slot == 'X') {
                    System.out.print(RED + slot + RESET + " | ");
                } else if (slot == 'O') {
                    System.out.print(YELLOW + slot + RESET + " | ");
                } else {
                    System.out.print(slot + " | ");
                }
            }
            System.out.println();
            System.out.println("❁═══❁═══❁═══❁═══❁═══❁═══❁═══❁");
        }
        System.out.println();
    }
}
