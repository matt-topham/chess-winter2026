package dataaccess;

import model.UserData;
import model.GameData;
import model.AuthData;

import java.util.Collection;

public interface DataAccess {
    //clear
    void clear() throws DataAccessException;
    //users
    void insertUser(UserData user) throws DataAccessException;
    UserData getUser(String username) throws DataAccessException;
    //auth
    void insertAuth(AuthData auth) throws DataAccessException;
    AuthData getAuth(String authToken) throws DataAccessException;
    void deleteAuth(String authToken) throws DataAccessException;
    //game
    int insertGame(GameData data) throws DataAccessException;
    GameData getGame(int gameID) throws DataAccessException;
    Collection<GameData> listGames() throws DataAccessException;
    void updateGame(GameData game) throws DataAccessException;
}
