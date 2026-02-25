package dataaccess;

import model.AuthData;
import model.UserData;
import model.GameData;
import chess.ChessGame;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class MemoryDataAccess implements DataAccess {

    private final Map<String, UserData> usersByUsername = new ConcurrentHashMap<>();
    private final Map<String, AuthData> authByToken = new ConcurrentHashMap<>();
    private final Map<Integer, GameData> gamesById = new ConcurrentHashMap<>();
    private final AtomicInteger nextGameId = new AtomicInteger(1);

    @Override
    public void clear() throws DataAccessException {
        usersByUsername.clear();
        authByToken.clear();
        gamesById.clear();
        nextGameId.set(1);
    }

    @Override
    public void insertUser(UserData user) throws DataAccessException {
        if (user == null || user.username() == null) {
            throw new DataAccessException("insertUser: user/username is null");
        }
        usersByUsername.put(user.username(), user);
    }

    @Override
    public UserData getUser(String username) throws DataAccessException {
        if (username == null) {
            return null;
        }
        return usersByUsername.get(username);
    }

    @Override
    public void insertAuth(AuthData auth) throws DataAccessException {
        if (auth == null || auth.authToken() == null) {
            throw new DataAccessException("insertAuth: auth/authToken is null");
        }
        authByToken.put(auth.authToken(), auth);
    }

    @Override
    public AuthData getAuth(String authToken) throws DataAccessException {
        if (authToken == null) {
            return null;
        }
        return authByToken.get(authToken);
    }

    @Override
    public void deleteAuth(String authToken) throws DataAccessException {
        if (authToken == null) {
            return;
        }
        authByToken.remove(authToken);
    }

    @Override
    public int insertGame(GameData data) throws DataAccessException {
        if (data == null) {
            throw new DataAccessException("insertGame: data is null");
        }

        int id = nextGameId.getAndIncrement();
        ChessGame game = (data.game() != null) ? data.game() : new ChessGame();

        GameData stored = new GameData(
                id,
                data.whiteUsername(),
                data.blackUsername(),
                data.gameName(),
                game
        );

        gamesById.put(id, stored);
        return id;
    }

    @Override
    public GameData getGame(int gameID) throws DataAccessException {
        return null;
    }

    @Override
    public Collection<GameData> listGames() throws DataAccessException {
        return List.of();
    }

    @Override
    public void updateGame(GameData game) throws DataAccessException {

    }

}
