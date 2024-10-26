import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Arrays;
import java.util.Scanner;
import java.net.SocketException;

public class GameLogic {
    private static final String SEPARATOR = Connect4.SEPARATOR;  // Reuse separator
    private static final String BANNER = Connect4.BANNER;  // Reuse banner
    
    private Socket socket;
    private boolean isPlayerOne;
    private char[][] board;
    private BufferedReader in;
    private PrintWriter out;
    private Scanner scanner;

    public GameLogic(Socket socket, boolean isPlayerOne) {
        this.socket = socket;
        this.isPlayerOne = isPlayerOne;
        this.board = new char[6][7];
        Arrays.stream(board).forEach(row -> Arrays.fill(row, '.'));
        this.scanner = new Scanner(System.in);
    }

    public void start() {
        try {
            this.in = new BufferedReader(new InputStreamReader(this.socket.getInputStream()));
            this.out = new PrintWriter(this.socket.getOutputStream(), true);
            boolean gameEnded = false;

            while (!gameEnded) {
                if (this.isPlayerOne) {
                    playerTurn('X');
                    gameEnded = checkWin('X') || checkDraw();
                    if (gameEnded) break;
                    opponentTurn('O');
                    gameEnded = checkWin('O') || checkDraw();
                } else {
                    opponentTurn('X');
                    gameEnded = checkWin('X') || checkDraw();
                    if (gameEnded) break;
                    playerTurn('O');
                    gameEnded = checkWin('O') || checkDraw();
                }
            }
        } catch (IOException e) {
            System.out.println("ERROR: Connection issue.");
            e.printStackTrace();
        } finally {
            cleanup();
        }
    }

    private void playerTurn(char playerChar) throws IOException {
        displayBoard();
        System.out.println(SEPARATOR);
        System.out.print("Your turn. Enter column (1-7): ");
    
        int col;
        while (true) {
            try {
                col = Integer.parseInt(this.scanner.nextLine()) - 1;
                if (isValidMove(col)) {
                    makeMove(col, playerChar);
                    break;
                }
                System.out.print("Invalid move. Try again: ");
            } catch (NumberFormatException e) {
                System.out.print("Please enter a valid column number (1-7): ");
            }
        }
    
        // Send the move to the opponent
        this.out.println("INSERT:" + (col + 1));
    
        // If this move results in a win, just display the board and wait for the opponent's "YOU WIN"
        if (checkWin(playerChar)) {
            displayBoard();
            System.out.println(SEPARATOR);
            System.out.println("You win the game! Waiting for opponent confirmation...");
        }
    }
    
    private void opponentTurn(char opponentChar) throws IOException {
        System.out.println(SEPARATOR);
        System.out.println("Waiting for opponent's move...");
        String receivedMessage = this.in.readLine();
        if (receivedMessage == null) {
            System.out.println("Opponent disconnected.");
            gameOver();
            return;
        }
        if (receivedMessage.startsWith("INSERT:")) {
            int col = Integer.parseInt(receivedMessage.split(":")[1]) - 1;
            if (isValidMove(col)) {
                makeMove(col, opponentChar);
    
                // If the opponent's move results in their win, send "YOU WIN" back and end the game
                if (checkWin(opponentChar)) {
                    displayBoard();
                    System.out.println(SEPARATOR);
                    System.out.println("You lose!");
                    this.out.println("YOU WIN");  // Acknowledge opponent's win
                    gameOver();
                }
            } else {
                System.out.println("ERROR: Invalid move from opponent.");
                this.out.println("ERROR");
                gameOver();
            }
        } else if (receivedMessage.equals("YOU WIN")) {
            // Opponent has received confirmation of their win
            System.out.println("Opponent confirmed your win. Game over.");
            gameOver();
        } else {
            System.out.println("ERROR: Unexpected message from opponent.");
            this.out.println("ERROR");
            gameOver();
        }
    }
    
    private void gameOver() {
        System.out.println("Thanks for playing!");
        System.out.println(SEPARATOR);
        System.out.println(BANNER);
        cleanup();
        System.exit(0);
    }

    private void cleanup() {
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
            scanner.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private boolean isValidMove(int col) {
        return col >= 0 && col < 7 && board[0][col] == '.';
    }

    private void makeMove(int col, char playerChar) {
        for (int row = 5; row >= 0; row--) {
            if (board[row][col] == '.') {
                board[row][col] = playerChar;
                break;
            }
        }
    }

    private boolean checkWin(char playerChar) {
        return checkHorizontalWin(playerChar) || checkVerticalWin(playerChar) || checkDiagonalWin(playerChar);
    }

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

    private boolean checkDiagonalWin(char playerChar) {
        for (int row = 3; row < 6; row++) {
            for (int col = 0; col < 4; col++) {
                if (board[row][col] == playerChar && board[row - 1][col + 1] == playerChar &&
                    board[row - 2][col + 2] == playerChar && board[row - 3][col + 3] == playerChar) {
                    return true;
                }
            }
        }
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

    private boolean checkDraw() {
        for (int col = 0; col < 7; col++) {
            if (board[0][col] == '.') {
                return false;
            }
        }
        displayBoard();
        System.out.println(SEPARATOR);
        System.out.println("The game is a draw!");
        this.out.println("DRAW");
        gameOver();
        return true;
    }

    private void displayBoard() {
        final String RESET = "\u001B[0m";  // ANSI reset code
        final String RED = "\u001B[31m";   // ANSI red color for Player 1
        final String YELLOW = "\u001B[33m"; // ANSI yellow color for Player 2
    
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
    