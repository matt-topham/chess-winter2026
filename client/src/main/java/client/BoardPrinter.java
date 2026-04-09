package client;

import chess.ChessBoard;
import chess.ChessGame;
import chess.ChessPiece;
import chess.ChessPosition;
import ui.EscapeSequences;

import java.util.Set;

public class BoardPrinter {

    public static void printBoard(ChessBoard board, ChessGame.TeamColor perspective) {
        printBoard(board, perspective, null, Set.of());
    }

    public static void printBoard(ChessBoard board,
                                  ChessGame.TeamColor perspective,
                                  ChessPosition selected,
                                  Set<ChessPosition> highlights) {

        boolean whiteView = (perspective == ChessGame.TeamColor.WHITE);
        if (highlights == null) {
            highlights = Set.of();
        }

        char[] files = new char[]{'a','b','c','d','e','f','g','h'};

        System.out.print(EscapeSequences.RESET_TEXT_COLOR);
        System.out.print(EscapeSequences.RESET_BG_COLOR);

        System.out.print("    ");
        if (whiteView) {
            for (char f : files) {
                System.out.print(" " + f + " ");
            }
        } else {
            for (int i = files.length - 1; i >= 0; i--) {
                System.out.print(" " + files[i] + " ");
            }
        }
        System.out.println();

        for (int printedRow = 8; printedRow >= 1; printedRow--) {
            int rank = whiteView ? printedRow : (9 - printedRow);

            System.out.print(" " + rank + " ");

            for (int printedCol = 1; printedCol <= 8; printedCol++) {
                int file = whiteView ? printedCol : (9 - printedCol);

                ChessPosition pos = new ChessPosition(rank, file);
                boolean light = isLightSquare(file, rank);

                if (selected != null && selected.equals(pos)) {
                    System.out.print(EscapeSequences.SET_BG_COLOR_YELLOW);
                } else if (highlights.contains(pos)) {
                    System.out.print(light ? EscapeSequences.SET_BG_COLOR_GREEN
                            : EscapeSequences.SET_BG_COLOR_DARK_GREEN);
                } else {
                    System.out.print(light ? EscapeSequences.SET_BG_COLOR_LIGHT_GREY
                            : EscapeSequences.SET_BG_COLOR_DARK_GREY);
                }

                ChessPiece piece = board.getPiece(pos);
                System.out.print(pieceString(piece));
                System.out.print(EscapeSequences.RESET_BG_COLOR);
            }

            System.out.println(" " + rank);
        }


        System.out.print("    ");
        if (whiteView) {
            for (char f : files) {
                System.out.print(" " + f + " ");
            }
        } else {
            for (int i = files.length - 1; i >= 0; i--) {
                System.out.print(" " + files[i] + " ");
            }
        }
        System.out.println(EscapeSequences.RESET_BG_COLOR);
    }

    private static boolean isLightSquare(int file, int rank) {
        return ((file + rank) % 2 == 1);
    }

    private static String pieceString(ChessPiece p) {
        if (p == null) {
            return EscapeSequences.EMPTY;
        }

        boolean white = (p.getTeamColor() == ChessGame.TeamColor.WHITE);
        return switch (p.getPieceType()) {
            case KING -> white ? EscapeSequences.WHITE_KING : EscapeSequences.BLACK_KING;
            case QUEEN -> white ? EscapeSequences.WHITE_QUEEN : EscapeSequences.BLACK_QUEEN;
            case BISHOP -> white ? EscapeSequences.WHITE_BISHOP : EscapeSequences.BLACK_BISHOP;
            case KNIGHT -> white ? EscapeSequences.WHITE_KNIGHT : EscapeSequences.BLACK_KNIGHT;
            case ROOK -> white ? EscapeSequences.WHITE_ROOK : EscapeSequences.BLACK_ROOK;
            case PAWN -> white ? EscapeSequences.WHITE_PAWN : EscapeSequences.BLACK_PAWN;
        };
    }
}