
package chess.pieces;

import chess.*;

import java.util.HashSet;
import java.util.Set;

public class PieceMover {
    public static Set<ChessMove> getLegalMove(ChessBoard board,
                                              ChessPosition start,
                                              ChessGame.TeamColor color,
                                              int[][] dir,
                                              boolean isMoving) {
        Set<ChessMove> moves = new HashSet<>();

        for (int[] vector : dir) {

            int nextRow = start.getRow() + vector[0];
            int nextCol = start.getColumn() + vector[1];

            while(isInside(nextRow, nextCol)) {
                ChessPosition target = new ChessPosition(nextRow, nextCol);
                ChessPiece occupant = board.getPiece(target);

                if (occupant == null) {
                    moves.add(new ChessMove(start, target, null));
                }

                else {
                    if (occupant.getTeamColor() != color) {
                        moves.add(new ChessMove(start, target, null));
                    }
                    break;
                }

                if (!isMoving) {
                    break;
                }

                nextRow += vector[0];
                nextCol += vector[1];

            }
        }
        return moves;
    }

    public static boolean isInside(int row, int col) {
        return row >= 1 && row <= 8 && col >= 1 && col <= 8;
    }
}
