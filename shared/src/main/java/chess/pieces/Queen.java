package chess.pieces;

import chess.*;
import java.util.Collection;
import java.util.Set;

public class Queen {
    private static final int[][] DIAGONALS = {
            {-1,  1}, {-1, -1}, { 1,  1}, { 1, -1}, { 1, 0}, {-1, 0}, {0, 1}, {0, -1}
    };
    public static Collection<ChessMove> getQueenMoves(ChessBoard board, ChessPosition start, ChessGame.TeamColor color) {
        Set<ChessMove> moves = PieceMover.getLegalMove(board, start, color, DIAGONALS, true);
        return moves;
    }
}