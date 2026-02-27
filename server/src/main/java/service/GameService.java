package service;

import dataaccess.DataAccess;
import dataaccess.DataAccessException;
import dataaccess.UnauthorizedException;
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
