package service;

import chess.ChessGame;
import dataaccess.*;
import model.GameData;

import java.util.Collection;

public class GameService {
    private final DataAccess data;

    public GameService(DataAccess data) {
        this.data = data;
    }

    public ListGameResult listGames(String authToken)
            throws UnauthorizedException, DataAccessException {
        requireValidAuth(authToken);
        Collection<GameData> games = data.listGames();
        return new ListGameResult(games);
    }

    public CreateGameResult createGame(CreateGameRequest req)
            throws BadRequestException, UnauthorizedException, DataAccessException {
        if (req == null || isBlank(req.gameName())) {
            throw new BadRequestException("400 Error: Bad request");
        }
        String username = requireValidAuth(req.authToken());

        GameData game = new GameData(0, null, null, req.gameName(), new ChessGame());
        int id = data.insertGame(game);
        return new CreateGameResult(id);
    }

    public void joinGame(JoinGameRequest request)
            throws BadRequestException, UnauthorizedException, AlreadyTakenException, DataAccessException {
        if (request == null) {
            throw new BadRequestException("400 Error: Bad request");
        }

        String username = requireValidAuth(request.authToken());

        if (request.gameID() <= 0 || isBlank(request.playerColor())) {
            throw new BadRequestException("400 Error: Bad request");
        }

        String color = request.playerColor().trim().toUpperCase();
        if (!color.equals("WHITE") && !color.equals("BLACK")) {
            throw new BadRequestException("400 Error: Bad request");
        }

        GameData game = data.getGame(request.gameID());
        if (game == null) {
            throw new BadRequestException("400 Error: Bad request");
        }

        if (color.equals("WHITE")) {
            if (game.whiteUsername() != null) {
                throw new AlreadyTakenException("403 Error: Already taken");
            }
            game = new GameData(game.gameID(), username, game.blackUsername(), game.gameName(), game.game());
        }
        else {
            if (game.blackUsername() != null) {
                throw new AlreadyTakenException("403 Error: Already taken");
            }
            game = new GameData(game.gameID(), game.whiteUsername(), username, game.gameName(), game.game());
        }

        data.updateGame(game);
    }

    private String requireValidAuth(String token)
            throws UnauthorizedException, DataAccessException {
        if (isBlank(token)) {
            throw new UnauthorizedException("401 Error: Unauthorized");
        }
        var auth = data.getAuth(token);
        if (auth == null) {
            throw new UnauthorizedException("401 Error: Unauthorized");
        }
        return auth.username();
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
