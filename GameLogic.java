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
        System.out.println("Player " + (playerChar == 'X' ? "1" : "2") + "'s turn: ");
        System.out.print("Enter column number (1-7): ");
        int column = scanner.nextInt() - 1;
        dropDisc(playerChar, column);
        out.println(column);  // Send move to opponent
    }

    private void opponentTurn(char playerChar) throws IOException {
        displayBoard();
        System.out.println("Waiting for opponent's move...");
        int column = Integer.parseInt(in.readLine());  // Get move from opponent
        dropDisc(playerChar, column);
    }

    private void dropDisc(char playerChar, int column) {
        for (int row = 5; row >= 0; row--) {
            if (board[row][column] == '.') {
                board[row][column] = playerChar;
                break;
            }
        }
    }

    private boolean checkWin(char playerChar) {
        // Check horizontal, vertical, and diagonal wins
        if (checkHorizontalWin(playerChar) || checkVerticalWin(playerChar) || checkDiagonalWin(playerChar)) {
            displayBoard();
            System.out.println("Player " + (playerChar == 'X' ? "1" : "2") + " connected 4 in a row.");
            System.out.println("Player " + (playerChar == 'X' ? "2" : "1") + " loses.");
            gameOver();
            return true;
        }
        return false;
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
        // Check \ diagonal (bottom-left to top-right)
        for (int row = 3; row < 6; row++) {
            for (int col = 0; col < 4; col++) {
                if (board[row][col] == playerChar && board[row - 1][col + 1] == playerChar &&
                    board[row - 2][col + 2] == playerChar && board[row - 3][col + 3] == playerChar) {
                    return true;
                }
            }
        }

        // Check / diagonal (top-left to bottom-right)
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
        System.out.println("\nCurrent Board:");
        System.out.println("  1   2   3   4   5   6   7 ");
        System.out.println("❁═══❁═══❁═══❁═══❁═══❁═══❁═══❁");
    
        for (char[] row : board) {
            System.out.print("| ");
            for (char slot : row) {
                System.out.print(slot + " | ");
            }
            System.out.println();
            System.out.println("❁═══❁═══❁═══❁═══❁═══❁═══❁═══❁");
        }
        System.out.println();
    }

    private void gameOver() {
        System.out.println("Game Over!");
        this.out.println("GAME_OVER");
        cleanup();
    }

    private void cleanup() {
        try {
            if (this.socket != null && !this.socket.isClosed()) {
                this.socket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
