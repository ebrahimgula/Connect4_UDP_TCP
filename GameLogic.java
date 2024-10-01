import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Arrays;
import java.util.Scanner;

public class GameLogic {
    private static final String SEPARATOR = "════════════════════════ˋˏ-༻❁༺-ˎˊ════════════════════════";
    
    private Socket socket;
    private boolean isPlayerOne;  // Determines if the player is Player 1 (true) or Player 2 (false)
    private char[][] board;
    private BufferedReader in;
    private PrintWriter out;
    private Scanner scanner;

    public GameLogic(Socket socket, boolean isPlayerOne) {
        this.socket = socket;
        this.isPlayerOne = isPlayerOne;
        this.board = new char[6][7];
        // Initialize the game board with empty slots
        for (char[] row : board) {
            Arrays.fill(row, '.');
        }
        this.scanner = new Scanner(System.in);
    }

    public void start() {
        try {
            this.in = new BufferedReader(new InputStreamReader(this.socket.getInputStream()));
            this.out = new PrintWriter(this.socket.getOutputStream(), true);
            boolean gameEnded = false;

            // Game loop
            while (!gameEnded) {
                if (this.isPlayerOne) {
                    playerTurn('X');
                    gameEnded = checkWin('X') || checkDraw();
                    if (gameEnded) {
                        break;
                    }
                    opponentTurn('O');
                    gameEnded = checkWin('O') || checkDraw();
                } else {
                    opponentTurn('X');
                    gameEnded = checkWin('X') || checkDraw();
                    if (gameEnded) {
                        break;
                    }
                    playerTurn('O');
                    gameEnded = checkWin('O') || checkDraw();
                }
            }

            this.socket.close();
        } catch (IOException e) {
            System.out.println("ERROR: Connection issue.");
            this.out.println("ERROR");
            e.printStackTrace();
            System.exit(1);
        }
    }

    // Player's turn to make a move
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

        // Check if the current player won
        if (checkWin(playerChar)) {
            displayBoard();
            System.out.println(SEPARATOR);
            System.out.println("You win!");
            this.out.println("YOU WIN");
            System.out.println(SEPARATOR);
            System.out.println("Thanks for playing!");
            System.out.println(SEPARATOR);
            displayBanner();
            System.exit(0);
        } else {
            this.out.println("INSERT:" + (col + 1));
        }
    }

    // Opponent's turn
    private void opponentTurn(char opponentChar) throws IOException {
        System.out.println(SEPARATOR);
        System.out.println("Waiting for opponent's move...");
        String receivedMessage = this.in.readLine();
        if (receivedMessage == null) {
            System.out.println("Opponent disconnected.");
            System.exit(0);
        }

        if (receivedMessage.startsWith("INSERT:")) {
            int col = Integer.parseInt(receivedMessage.split(":")[1]) - 1;
            if (isValidMove(col)) {
                makeMove(col, opponentChar);
                if (checkWin(opponentChar)) {
                    displayBoard();
                    System.out.println(SEPARATOR);
                    System.out.println("You lose!");
                    this.out.println("YOU LOSE");
                    System.out.println(SEPARATOR);
                    System.out.println("Thanks for playing!");
                    System.out.println(SEPARATOR);
                    displayBanner();
                    System.exit(0);
                }
            } else {
                System.out.println("ERROR");
                this.out.println("ERROR");
                System.exit(1);
            }
        } else if (receivedMessage.equals("YOU WIN")) {
            displayBoard();
            System.out.println(SEPARATOR);
            System.out.println("You lose!");
            System.out.println(SEPARATOR);
            System.out.println("Thanks for playing!");
            System.out.println(SEPARATOR);
            displayBanner();
            System.exit(0);
        } else if (receivedMessage.equals("ERROR")) {
            System.out.println("Received 'ERROR' from opponent. Exiting.");
            System.exit(1);
        } else {
            System.out.println("ERROR");
            this.out.println("ERROR");
            System.exit(1);
        }
    }

    // Check if the move is valid
    private boolean isValidMove(int col) {
        return col >= 0 && col < 7 && board[0][col] == '.';
    }

    // Make a move on the board
    private void makeMove(int col, char playerChar) {
        for (int row = 5; row >= 0; row--) {
            if (board[row][col] == '.') {
                board[row][col] = playerChar;
                break;
            }
        }
    }

    // Check for win conditions
    private boolean checkWin(char playerChar) {
        // Check horizontal, vertical, and diagonal win conditions
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
        // Check for diagonal wins
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

    // Check if the game is a draw
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
        System.out.println(SEPARATOR);
        System.out.println("Thanks for playing!");
        System.out.println(SEPARATOR);
        displayBanner();
        System.exit(0);
        return true;
    }

    // Display the current game board
    private void displayBoard() {
        System.out.println("\nCurrent Board:");
        for (char[] row : board) {
            System.out.print("| ");
            for (char slot : row) {
                System.out.print(slot + " | ");
            }
            System.out.println();
        }
        System.out.println("  1   2   3   4   5   6   7 \n");
    }

    // Banner to display after the game ends
    private void displayBanner() {
        System.out.println("                                                                                              ");
        System.out.println("  .g8\"\"\"bgd   .g8\"\"8q. `7MN.   `7MF`7MN.   `7MF`7MM\"\"\"YMM    .g8\"\"\"bgd MMP\"\"MM\"\"YMM      ");
        System.out.println(".dP'     `M .dP'    `YM. MMN.    M   MMN.    M   MM    `7  .dP'     `M P'   MM   `7      ");
        System.out.println("dM'       ` dM'      `MM M YMb   M   M YMb   M   MM   d    dM'       `      MM      ,AM  ");
        System.out.println("MM          MM        MM M  `MN. M   M  `MN. M   MMmmMM    MM               MM     AVMM  ");
        System.out.println("MM.         MM.      ,MP M   `MM.M   M   `MM.M   MM   Y  , MM.              MM   ,W' MM  ");
        System.out.println("`Mb.     ,' `Mb.    ,dP' M     YMM   M     YMM   MM     ,M `Mb.     ,'      MM ,W'   MM  ");
        System.out.println("  `\"bmmmd'    `\"bmmd\"' .JML.    YM .JML.    YM .JMMmmmmMMM   `\"bmmmd'     .JMMLAmmmmmMMmm");
        System.out.println("                                                                                     MM  ");
        System.out.println("                                                                                     MM  ");
        System.out.println(SEPARATOR);
    }
}
