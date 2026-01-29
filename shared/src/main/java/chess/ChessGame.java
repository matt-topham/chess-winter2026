package chess;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * For a class that can manage a chess game, making moves on a board
 * <p>
 * Note: You can add to this class, but you may not alter
 * signature of the existing methods.
 */
public class ChessGame {

    private TeamColor teamTurn = TeamColor.WHITE;
    private ChessBoard board;
    private boolean gameOver =  false;

    // trackers
    private boolean wKingMoved = false;
    private boolean bKingMoved = false;
    private boolean wRookAMoved = false;
    private boolean wRookHMoved = false;
    private boolean bRookAMoved = false;
    private boolean bRookHMoved = false;



    public ChessGame() {
        this.board = new ChessBoard();
        this.board.resetBoard();

    }

    /**
     * @return Which team's turn it is
     */
    public TeamColor getTeamTurn() {
        return teamTurn;
    }

    /**
     * Set's which teams turn it is
     *
     * @param team the team whose turn it is
     */
    public void setTeamTurn(TeamColor team) {
        this.teamTurn = team;
    }

    /**
     * Enum identifying the 2 possible teams in a chess game
     */
    public enum TeamColor {
        WHITE,
        BLACK
    }

    /**
     * Gets a valid moves for a piece at the given location
     *
     * @param startPosition the piece to get valid moves for
     * @return Set of valid moves for requested piece, or null if no piece at
     * startPosition
     */
    public Collection<ChessMove> validMoves(ChessPosition startPosition) {
        ChessPiece piece = board.getPiece(startPosition);
        if (piece == null) {
            return null;
        }
        Collection<ChessMove> potential = piece.pieceMoves(board, startPosition);
        List<ChessMove> legalMoves = new ArrayList<>();
        if (potential == null) {
            return legalMoves;
        }

        if (piece.getPieceType() == ChessPiece.PieceType.KING) {
            potential.addAll(castleMoves(startPosition, piece.getTeamColor()));
        }

        for(ChessMove move : potential) {
            if (safeMove(startPosition, piece, move)){
                legalMoves.add(move);
            }
        }
        return legalMoves;
    }

    /**
     * Makes a move in a chess game
     *
     * @param move chess move to perform
     * @throws InvalidMoveException if move is invalid
     */
    public void makeMove(ChessMove move) throws InvalidMoveException {
        ChessPosition from = move.getStartPosition();
        ChessPiece mover = board.getPiece(from);

        if (mover == null) {
            throw new InvalidMoveException("No piece at: " + from);
        }

        ChessGame.TeamColor color = mover.getTeamColor();

        if (color != teamTurn) {
            throw new InvalidMoveException("It is " + teamTurn + "'s turn");
        }

        Collection<ChessMove> legalMoves = validMoves(from);

        if (legalMoves == null || !legalMoves.contains(move)) {
            throw new InvalidMoveException("Illegal move: " + move);
        }

        ChessPosition to = move.getEndPosition();
        ChessPiece captured = board.getPiece(to);
        boolean castle = isCastleMove(mover, move);

        updateCastlingPermissionsOnCapture(captured, to);

        ChessPiece.PieceType finalType;

        if (move.getPromotionPiece() != null) {
            finalType = move.getPromotionPiece();
        }

        else {
            finalType = mover.getPieceType();
        }

        updateCastlingPermissionsOnMove(mover, from);

        board.addPiece(from, null);
        board.addPiece(move.getEndPosition(), new ChessPiece(mover.getTeamColor(), finalType));

        if (castle) {
            performCastleRookMove(from, to, mover.getTeamColor(), board);
            markRookMovedByCastle(mover.getTeamColor(), to);
        }

        teamTurn = opponent(teamTurn);
    }

    /**
     * Determines if the given team is in check
     *
     * @param teamColor which team to check for check
     * @return True if the specified team is in check
     */
    public boolean isInCheck(TeamColor teamColor) {
        ChessPosition king = findKing(teamColor);
        if (king == null) {
            return false;
        }

        TeamColor opp = opponent(teamColor);

        for (int r = 1; r <= 8; r++) {
            for (int c = 1; c <= 8; c++) {
                ChessPosition pos = new ChessPosition(r, c);
                ChessPiece piece = board.getPiece(pos);

                if (piece == null || piece.getTeamColor() != opp) {
                    continue;
                }

                Collection<ChessMove> fake = piece.pieceMoves(board, pos);

                if (fake == null) {
                    continue;
                }

                for (ChessMove move : fake) {
                    if (king.equals(move.getEndPosition())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Determines if the given team is in checkmate
     *
     * @param teamColor which team to check for checkmate
     * @return True if the specified team is in checkmate
     */
    public boolean isInCheckmate(TeamColor teamColor) {
        return isInCheck(teamColor) && !anyLegalMove(teamColor);
    }

    /**
     * Determines if the given team is in stalemate, which here is defined as having
     * no valid moves while not in check.
     *
     * @param teamColor which team to check for stalemate
     * @return True if the specified team is in stalemate, otherwise false
     */
    public boolean isInStalemate(TeamColor teamColor) {
        return !isInCheck(teamColor) && !anyLegalMove(teamColor);
    }

    /**
     * Sets this game's chessboard with a given board
     *
     * @param board the new board to use
     */
    public void setBoard(ChessBoard board) {
        this.board = board;
    }

    /**
     * Gets the current chessboard
     *
     * @return the chessboard
     */
    public ChessBoard getBoard() {
        return board;
    }

    private TeamColor opponent(TeamColor color) {
        if (color == TeamColor.WHITE) {
            return TeamColor.BLACK;
        }
        else {
            return TeamColor.WHITE;
        }
    }

    private ChessBoard copyBoard() {
        ChessBoard copy = new ChessBoard();
        for (int r = 1; r <= 8; r++) {
            for (int c = 1; c <= 8; c++) {
                ChessPosition pos = new ChessPosition(r, c);
                ChessPiece piece = board.getPiece(pos);
                if (piece != null) {
                    copy.addPiece(pos, new ChessPiece(piece.getTeamColor(), piece.getPieceType()));
                }
                else {
                    copy.addPiece(pos, null);
                }
            }
        }
        return copy;
    }

    private boolean safeMove (ChessPosition from, ChessPiece mover, ChessMove move) {
        ChessBoard sim = copyBoard();

        ChessPiece.PieceType promotion = move.getPromotionPiece();
        ChessPiece.PieceType finalType;

        if(promotion != null) {
            finalType = promotion;
        }

        else {
            finalType = mover.getPieceType();
        }

        sim.addPiece(from, null);
        ChessPiece newPiece =new ChessPiece(mover.getTeamColor(), finalType);
        sim.addPiece(move.getEndPosition(), newPiece);

        ChessGame checkGame = new ChessGame();
        checkGame.setBoard(sim);
        return !checkGame.isInCheck(mover.getTeamColor());
    }

    private ChessPosition findKing (TeamColor color) {
        for (int r = 1; r <= 8; r++) {
            for (int c = 1; c <= 8; c++) {
                ChessPosition pos = new ChessPosition(r, c);
                ChessPiece piece = board.getPiece(pos);
                if (piece != null
                        && piece.getTeamColor() == color
                        && piece.getPieceType() == ChessPiece.PieceType.KING) {
                    return pos;
                }
            }
        }
        return null;
    }

    private boolean anyLegalMove (TeamColor color) {
        for (int r = 1; r <= 8; r++) {
            for (int c = 1; c <= 8; c++) {
                ChessPosition pos = new ChessPosition(r, c);
                ChessPiece piece = board.getPiece(pos);
                if (piece == null || piece.getTeamColor() != color) {
                    continue;
                }
                Collection<ChessMove> legalMove = validMoves(pos);
                if (legalMove != null && !legalMove.isEmpty()){
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isCastleMove(ChessPiece mover, ChessMove move) {
        if (mover == null || mover.getPieceType() != ChessPiece.PieceType.KING) {
            return false;
        }
        ChessPosition from = move.getStartPosition();
        ChessPosition to = move.getEndPosition();

        return from.getRow() == to.getRow()
                && (to.getColumn() - from.getColumn() == 2 || to.getColumn() - from.getColumn() == -2);
    }

    private Collection<ChessMove> castleMoves (ChessPosition kingPos, TeamColor color) {
        List<ChessMove> moves = new ArrayList<>();

        int homeRow = (color == TeamColor.WHITE) ? 1 : 8;
        if (kingPos.getRow() != homeRow || kingPos.getColumn() != 5) {
            return moves;
        }

        if (wKingMoved && color == TeamColor.WHITE) {
            return moves;
        }
        if (bKingMoved && color == TeamColor.BLACK) {
            return moves;
        }

        if(isInCheck(color)) {
            return moves;
        }

        if(canCastle(color, true)) {
            moves.add(new ChessMove(kingPos, new ChessPosition(homeRow, 7), null));
        }

        if(canCastle(color, false)) {
            moves.add(new ChessMove(kingPos, new ChessPosition(homeRow, 3), null));
        }

        return moves;
    }

    private boolean canCastle(TeamColor color, boolean kingSide) {
        int row = (color == TeamColor.WHITE) ? 1 : 8;
        TeamColor opp = opponent(color);

        if (kingSide) {
            if ((color == TeamColor.WHITE && wRookHMoved) || (color == TeamColor.BLACK && bRookHMoved)) return false;
            ChessPiece rook = board.getPiece(new ChessPosition(row, 8));
            if (rook == null || rook.getTeamColor() != color || rook.getPieceType() != ChessPiece.PieceType.ROOK) return false;

            if (board.getPiece(new ChessPosition(row, 6)) != null) return false;
            if (board.getPiece(new ChessPosition(row, 7)) != null) return false;

            if (isSquareAttacked(new ChessPosition(row, 6), opp)) return false;
            if (isSquareAttacked(new ChessPosition(row, 7), opp)) return false;

            return true;
        }
        else {
            if ((color == TeamColor.WHITE && wRookAMoved) || (color == TeamColor.BLACK && bRookAMoved)) return false;
            ChessPiece rook = board.getPiece(new ChessPosition(row, 1));
            if (rook == null || rook.getTeamColor() != color || rook.getPieceType() != ChessPiece.PieceType.ROOK) return false;

            if (board.getPiece(new ChessPosition(row, 2)) != null) return false;
            if (board.getPiece(new ChessPosition(row, 3)) != null) return false;
            if (board.getPiece(new ChessPosition(row, 4)) != null) return false;

            if (isSquareAttacked(new ChessPosition(row, 4), opp)) return false;
            if (isSquareAttacked(new ChessPosition(row, 3), opp)) return false;

            return true;
        }
    }

    private boolean isSquareAttacked(ChessPosition target, TeamColor attacker) {
        for (int r = 1; r <= 8; r++) {
            for (int c = 1; c <= 8; c++) {
                ChessPosition from = new ChessPosition(r, c);
                ChessPiece p = board.getPiece(from);
                if (p == null || p.getTeamColor() != attacker) continue;

                switch (p.getPieceType()) {
                    case PAWN -> {
                        int dir = (attacker == TeamColor.WHITE) ? 1 : -1;
                        int tr = r + dir;
                        if (tr >= 1 && tr <= 8) {
                            if (target.getRow() == tr && (target.getColumn() == c - 1 || target.getColumn() == c + 1)) {
                                return true;
                            }
                        }
                    }
                    case KING -> {
                        int dr = Math.abs(target.getRow() - r);
                        int dc = Math.abs(target.getColumn() - c);
                        if (Math.max(dr, dc) == 1) return true;
                    }
                    default -> {
                        Collection<ChessMove> pseudo = p.pieceMoves(board, from);
                        if (pseudo == null) continue;
                        for (ChessMove m : pseudo) {
                            if (target.equals(m.getEndPosition())) return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    private void performCastleRookMove(ChessPosition kingFrom,
                                       ChessPosition kingTo,
                                       ChessGame.TeamColor color,
                                       ChessBoard board) {
        int r = kingFrom.getRow();
        int toCol = kingTo.getColumn();

        if (toCol == 7){
            board.addPiece(new ChessPosition(r, 8), null);
            board.addPiece(new ChessPosition(r, 6), new ChessPiece(color, ChessPiece.PieceType.ROOK));
        }

        if (toCol == 3){
            board.addPiece(new ChessPosition(r, 1), null);
            board.addPiece(new ChessPosition(r, 4), new ChessPiece(color, ChessPiece.PieceType.ROOK));
        }

    }

    private void updateCastlingPermissionsOnMove(ChessPiece mover, ChessPosition from) {
        if (mover.getPieceType() == ChessPiece.PieceType.KING) {
            if (mover.getTeamColor() == TeamColor.WHITE) {
                wKingMoved = true;
            }
            else {
                bKingMoved = true;
            }
        }
        if (mover.getPieceType() == ChessPiece.PieceType.ROOK) {
            if (mover.getTeamColor() == TeamColor.WHITE) {
                if (from.getRow() == 1 && from.getColumn() == 1) {
                    wRookAMoved = true;
                }
                if (from.getRow() == 1 && from.getColumn() == 8) {
                    wRookHMoved = true;
                }
            }
            else {
                if (from.getRow() == 8 && from.getColumn() == 1) {
                    bRookAMoved = true;
                }
                if (from.getRow() == 8 && from.getColumn() == 8) {
                    bRookHMoved = true;
                }
            }
        }
    }

    private void updateCastlingPermissionsOnCapture(ChessPiece captured, ChessPosition at) {
        if (captured == null || captured.getPieceType() != ChessPiece.PieceType.ROOK) {
            return;
        }
        if (captured.getTeamColor() == TeamColor.WHITE) {
            if (at.getRow() == 1 && at.getColumn() == 1) {
                wRookAMoved = true;
            }
            if (at.getRow() == 1 && at.getColumn() == 8) {
                wRookHMoved = true;
            }
        }
        else {
            if (at.getRow() == 8 && at.getColumn() == 1) {
                bRookAMoved = true;
            }
            if (at.getRow() == 8 && at.getColumn() == 8) {
                bRookHMoved = true;
            }
        }
    }

    private void markRookMovedByCastle(TeamColor color, ChessPosition kingTo) {
        boolean kingSide = (kingTo.getColumn() == 7);
        if(color == TeamColor.WHITE){
            if (kingSide) {
                wRookHMoved = true;
            }
            else {
                wRookAMoved = true;
            }
        }
        else {
            if (kingSide) {
                bRookHMoved = true;
            }
            else {
                bRookAMoved = true;
            }
        }
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ChessGame chessGame = (ChessGame) o;
        return gameOver == chessGame.gameOver && teamTurn == chessGame.teamTurn && Objects.equals(board, chessGame.board);
    }

    @Override
    public int hashCode() {
        return Objects.hash(teamTurn, board, gameOver);
    }
}
