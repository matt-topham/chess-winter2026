package client;

import chess.ChessBoard;
import chess.ChessGame;

public class BoardPrinter {
    public static void printInitialBoard(ChessGame.TeamColor perspective) {
        ChessGame game = new ChessGame();
        game.getBoard().resetBoard();
        printBoard(game.getBoard(), perspective);
    }

    public static void printBoard(ChessBoard board, ChessGame.TeamColor perspective) {}
}
