package chess.pieces;

import chess.*;
import java.util.Collection;
import java.util.Set;

public class Bishop {
    private static final int[][] DIAGONALS = {
            {-1,  1}, {-1, -1}, { 1,  1}, { 1, -1}};
    public static Collection<ChessMove> getBishopMoves(ChessBoard board, ChessPosition start, ChessGame.TeamColor color) {
        Set<ChessMove> moves = PieceMover.getLegalMove(board, start, color, DIAGONALS, true);
        return moves;
    }
}