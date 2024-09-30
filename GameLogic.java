import java.io.*;
import java.net.Socket;
import java.util.Arrays;
import java.util.Scanner;

public class GameLogic {
    private static final String SEPARATOR = "════════════════════════ˋˏ-༻❁༺-ˎˊ════════════════════════";

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
        this.scanner = new Scanner(System.in);
        initializeBoard();
    }

    private void initializeBoard() {
        for (int i = 0; i < board.length; i++) {
            Arrays.fill(board[i], '.');
        }
    }

    public void start() {
        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            boolean gameOver = false;

            if (isPlayerOne) {
                System.out.println(SEPARATOR);
                System.out.println("You are Player 1 (X). You go first.");
                System.out.println(SEPARATOR);

                while (!gameOver) {
                    playerTurn('X');
                    gameOver = checkWin('X') || checkDraw();
                    if (gameOver) break;

                    opponentTurn('O');
                    gameOver = checkWin('O') || checkDraw();
                }
            } else {
                System.out.println(SEPARATOR);
                System.out.println("You are Player 2 (O). Waiting for Player 1 to make the first move.");
                System.out.println(SEPARATOR);

                while (!gameOver) {
                    opponentTurn('X');
                    gameOver = checkWin('X') || checkDraw();
                    if (gameOver) break;

                    playerTurn('O');
                    gameOver = checkWin('O') || checkDraw();
                }
            }

            socket.close();  // Close the socket once the game is over
        } catch (IOException e) {
            System.out.println("Network error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void playerTurn(char playerSymbol) throws IOException {
        displayBoard();
        System.out.println(SEPARATOR);
        System.out.print("Your turn (Player " + (playerSymbol == 'X' ? "1" : "2") + "). Enter column (1-7): ");

        int column;
        while (true) {
            try {
                column = Integer.parseInt(scanner.nextLine()) - 1;  // Column input (1-7)
                if (isValidMove(column)) {
                    makeMove(column, playerSymbol);
                    break;
                }
                System.out.print("Invalid move. Try again: ");
            } catch (NumberFormatException e) {
                System.out.print("Invalid input. Enter a valid column number (1-7): ");
            }
        }

        displayBoard();
        out.println("INSERT:" + (column + 1));  // Send the column to the opponent

        if (checkWin(playerSymbol)) {
            out.println("YOU WIN");
            System.out.println(SEPARATOR);
            System.out.println("Congratulations, Player " + (playerSymbol == 'X' ? "1" : "2") + ", you win!");
            System.out.println(SEPARATOR);
            System.exit(0);
        }
    }

    private void opponentTurn(char opponentSymbol) throws IOException {
        System.out.println("Waiting for opponent's move...");
        String message = in.readLine();

        if (message == null) {
            System.out.println("Opponent disconnected. Ending game.");
            System.exit(0);
        }

        if (message.startsWith("INSERT:")) {
            try {
                int column = Integer.parseInt(message.split(":")[1]) - 1;
                if (isValidMove(column)) {
                    makeMove(column, opponentSymbol);
                    displayBoard();
                } else {
                    System.out.println("Received invalid move from opponent.");
                    out.println("ERROR");
                    System.exit(1);
                }
            } catch (NumberFormatException e) {
                System.out.println("Corrupted move received. Exiting.");
                System.exit(1);
            }
        } else if (message.equals("YOU WIN")) {
            displayBoard();
            System.out.println(SEPARATOR);
            System.out.println("You lose! Opponent won.");
            System.out.println(SEPARATOR);
            System.exit(0);
        } else if (message.equals("ERROR")) {
            System.out.println("Opponent encountered an error. Exiting game.");
            System.exit(1);
        } else {
            System.out.println("Invalid message received. Exiting.");
            System.exit(1);
        }
    }

    private boolean isValidMove(int column) {
        return column >= 0 && column <= 6 && board[0][column] == '.';
    }

    private void makeMove(int column, char symbol) {
        for (int i = 5; i >= 0; i--) {
            if (board[i][column] == '.') {
                board[i][column] = symbol;
                break;
            }
        }
    }

    private boolean checkWin(char symbol) {
        return checkHorizontal(symbol) || checkVertical(symbol) || checkDiagonal(symbol);
    }

    private boolean checkHorizontal(char symbol) {
        for (int row = 0; row < 6; row++) {
            for (int col = 0; col < 4; col++) {
                if (board[row][col] == symbol && board[row][col + 1] == symbol
                        && board[row][col + 2] == symbol && board[row][col + 3] == symbol) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean checkVertical(char symbol) {
        for (int col = 0; col < 7; col++) {
            for (int row = 0; row < 3; row++) {
                if (board[row][col] == symbol && board[row + 1][col] == symbol
                        && board[row + 2][col] == symbol && board[row + 3][col] == symbol) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean checkDiagonal(char symbol) {
        // Check downward diagonal (\)
        for (int row = 3; row < 6; row++) {
            for (int col = 0; col < 4; col++) {
                if (board[row][col] == symbol && board[row - 1][col + 1] == symbol
                        && board[row - 2][col + 2] == symbol && board[row - 3][col + 3] == symbol) {
                    return true;
                }
            }
        }
        // Check upward diagonal (/)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 4; col++) {
                if (board[row][col] == symbol && board[row + 1][col + 1] == symbol
                        && board[row + 2][col + 2] == symbol && board[row + 3][col + 3] == symbol) {
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
        System.out.println(SEPARATOR);
        out.println("DRAW");
        return true;
    }

    private void displayBoard() {
        System.out.println("\nCurrent Board:");
        for (int row = 0; row < 6; row++) {
            System.out.print("| ");
            for (int col = 0; col < 7; col++) {
                System.out.print(board[row][col] + " | ");
            }
            System.out.println();
        }
        System.out.println("  1   2   3   4   5   6   7 \n");
    }
}
