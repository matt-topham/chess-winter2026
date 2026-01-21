package chess.pieces;

import chess.*;
import java.util.Collection;
import java.util.Set;

public class Knight {
    private static final int[][] DIAGONALS = {
            {1,2},{1,-2},{-1,2},{-1,-2},
            {2,1},{2,-1},{-2,1},{-2,-1}
    };
    public static Collection<ChessMove> getKnightMoves(ChessBoard board, ChessPosition start, ChessGame.TeamColor color) {
        Set<ChessMove> moves = PieceMover.getLegalMove(board, start, color, DIAGONALS, false);
        return moves;
    }
}