
package chess.pieces;

import chess.*;
import java.util.HashSet;
import java.util.Set;
import java.util.Collection;

//pawn does not use the pieceMover helper directly. Due to more complex movements it helps itself move while using
// one of the helper functions of pieceMover

public class Pawn {

    public static Collection<ChessMove> getPawnMoves(ChessBoard board,
                                                     ChessPosition start,
                                                     ChessGame.TeamColor color) {
        Set<ChessMove> moves = new HashSet<>();

        int r = start.getRow();
        int c = start.getColumn();

        //dir based on color will move the piece up or down the board
        int dir;
        if (color == ChessGame.TeamColor.WHITE){
            dir = 1;
        }
        else {
            dir = -1;
        }
        //start and promo Rank are similarly bases off of color
        int startRank;
        if (color == ChessGame.TeamColor.WHITE) {
            startRank = 2;
        }
        else {
            startRank = 7;
        }
        int promoRank;
        if (color == ChessGame.TeamColor.WHITE) {
            promoRank = 8;
        }
        else {
            promoRank = 1;
        }

        //checks to see if the square directly in front is clear
        ChessPosition one = pos(r + dir, c); // r+dir moves it up or down
        if (PieceMover.isInside(one.getRow(), one.getColumn()) && board.getPiece(one) == null){
            addMoveOrPromotion(moves, start, one, promoRank);
            // if the piece mover is on the starting rank it will check the second square in front of it
            if (r == startRank) {
                ChessPosition two = pos(r + 2 * dir, c);
                if (PieceMover.isInside(two.getRow(), two.getColumn()) && board.getPiece(two) == null) {
                    moves.add(new ChessMove(start, two, null));
                }
            }
        }
        // this will check the diagonal positions to see if there is a piece that can be captured or not
        for (int dc : new int[]{-1, 1}) {
            ChessPosition diag = pos(r + dir, c + dc);
            if (!PieceMover.isInside(diag.getRow(), diag.getColumn())) {
                continue;
            }
            ChessPiece target = board.getPiece(diag);
            if (target != null && target.getTeamColor() != color) {
                addMoveOrPromotion(moves, start, diag, promoRank);
            }
        }

        return moves;
    }

    // --- helpers ---
    //helps with the checkers
    private static ChessPosition pos(int row, int col) {
        return new ChessPosition(row, col);
    }

    // main moving function for the pawn
    private static void addMoveOrPromotion(Set<ChessMove> moves,
                                           ChessPosition from,
                                           ChessPosition to,
                                           int promoRank) {
        if (to.getRow() == promoRank) {
            moves.add(new ChessMove(from, to, ChessPiece.PieceType.QUEEN));
            moves.add(new ChessMove(from, to, ChessPiece.PieceType.ROOK));
            moves.add(new ChessMove(from, to, ChessPiece.PieceType.BISHOP));
            moves.add(new ChessMove(from, to, ChessPiece.PieceType.KNIGHT));
        } else {
            moves.add(new ChessMove(from, to, null));
        }
    }
}
