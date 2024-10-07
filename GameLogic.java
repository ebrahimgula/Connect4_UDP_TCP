import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Arrays;
import java.util.Scanner;

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

            if (isPlayerOne) {
                System.out.println("You are Player 1. You go first.");
                playerTurn();
            } else {
                System.out.println("You are Player 2. Waiting for Player 1...");
                opponentTurn();
            }
        } catch (IOException e) {
            System.out.println("ERROR: " + e.getMessage());
            sendError();
        }
    }

    private void playerTurn() {
        printBoard();
        System.out.println("Enter column (0-6): ");
        int column = scanner.nextInt();

        if (column < 0 || column > 6) {
            System.out.println("Invalid column. Please enter a value between 0 and 6.");
            playerTurn();
            return;
        }

        if (!insertDisc(column, isPlayerOne ? 'X' : 'O')) {
            System.out.println("Column is full. Try another column.");
            playerTurn();
            return;
        }

        out.println("INSERT:" + column);
        if (checkWin()) {
            System.out.println("Congratulations! You win!");
            out.println("YOU WIN");
            closeConnection();
            return;
        }

        opponentTurn();
    }

    private void opponentTurn() {
        try {
            String response = in.readLine();
            if (response == null) {
                System.out.println("Opponent disconnected.");
                closeConnection();
                return;
            }

            String[] parts = response.split(":");
            switch (parts[0]) {
                case "INSERT":
                    int column = Integer.parseInt(parts[1]);
                    insertDisc(column, isPlayerOne ? 'O' : 'X');
                    if (checkWin()) {
                        System.out.println("Opponent wins!");
                        closeConnection();
                        return;
                    }
                    playerTurn();
                    break;
                case "YOU WIN":
                    System.out.println("You lose. Opponent wins.");
                    closeConnection();
                    break;
                case "ERROR":
                    System.out.println("Error received from opponent. Terminating game.");
                    closeConnection();
                    break;
                default:
                    System.out.println("Unknown message received: " + response);
                    sendError();
                    break;
            }
        } catch (IOException e) {
            System.out.println("ERROR: " + e.getMessage());
            sendError();
        }
    }

    private boolean insertDisc(int column, char disc) {
        for (int row = board.length - 1; row >= 0; row--) {
            if (board[row][column] == '.') {
                board[row][column] = disc;
                return true;
            }
        }
        return false;
    }

    private boolean checkWin() {
        // Check horizontal, vertical, and diagonal win conditions
        return checkHorizontalWin() || checkVerticalWin() || checkDiagonalWin();
    }

    private boolean checkHorizontalWin() {
        for (int row = 0; row < board.length; row++) {
            for (int col = 0; col < board[row].length - 3; col++) {
                if (board[row][col] != '.' &&
                    board[row][col] == board[row][col + 1] &&
                    board[row][col] == board[row][col + 2] &&
                    board[row][col] == board[row][col + 3]) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean checkVerticalWin() {
        for (int col = 0; col < board[0].length; col++) {
            for (int row = 0; row < board.length - 3; row++) {
                if (board[row][col] != '.' &&
                    board[row][col] == board[row + 1][col] &&
                    board[row][col] == board[row + 2][col] &&
                    board[row][col] == board[row + 3][col]) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean checkDiagonalWin() {
        // Check for diagonals (both directions)
        for (int row = 0; row < board.length - 3; row++) {
            for (int col = 0; col < board[row].length - 3; col++) {
                if (board[row][col] != '.' &&
                    board[row][col] == board[row + 1][col + 1] &&
                    board[row][col] == board[row + 2][col + 2] &&
                    board[row][col] == board[row + 3][col + 3]) {
                    return true;
                }
            }
            for (int col = 3; col < board[row].length; col++) {
                if (board[row][col] != '.' &&
                    board[row][col] == board[row + 1][col - 1] &&
                    board[row][col] == board[row + 2][col - 2] &&
                    board[row][col] == board[row + 3][col - 3]) {
                    return true;
                }
            }
        }
        return false;
    }

    private void printBoard() {
        for (char[] row : board) {
            for (char cell : row) {
                System.out.print(cell + " ");
            }
            System.out.println();
        }
        System.out.println(SEPARATOR);
    }

    private void sendError() {
        out.println("ERROR");
        closeConnection();
    }

    private void closeConnection() {
        try {
            if (socket != null) socket.close();
        } catch (IOException e) {
            System.out.println("Failed to close socket: " + e.getMessage());
        }
    }
}
