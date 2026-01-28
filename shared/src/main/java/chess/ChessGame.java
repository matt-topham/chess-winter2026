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

        ChessPiece.PieceType finalType;

        if (move.getPromotionPiece() != null) {
            finalType = move.getPromotionPiece();
        }

        else {
            finalType = mover.getPieceType();
        }

        board.addPiece(from, null);
        board.addPiece(move.getEndPosition(), new ChessPiece(mover.getTeamColor(), finalType));

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
