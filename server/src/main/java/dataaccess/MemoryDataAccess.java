package dataaccess;

import model.AuthData;
import model.UserData;
import model.GameData;
import chess.ChessGame;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class MemoryDataAccess {

    private final Map<String, UserData> usersByUsername = new ConcurrentHashMap<>();
    private final Map<String, AuthData> authByToken = new ConcurrentHashMap<>();
    private final Map<Integer, GameData> gamesById = new ConcurrentHashMap<>();
    private final AtomicInteger nextGameId = new AtomicInteger(1);

    public void clear() throws DataAccessException {
        usersByUsername.clear();
        authByToken.clear();
        gamesById.clear();
        nextGameId.set(1);
    }

}
