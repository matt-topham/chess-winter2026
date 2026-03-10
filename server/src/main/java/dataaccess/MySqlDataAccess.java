package dataaccess;

import com.google.gson.Gson;
import model.AuthData;
import model.GameData;
import model.UserData;

import java.sql.SQLIntegrityConstraintViolationException;
import java.util.Collection;
import java.util.List;

public class MySqlDataAccess implements DataAccess{

    private final Gson gson = new Gson();

    @Override
    public void clear() throws DataAccessException {
        try (var connection = DatabaseManager.getConnection();
             var statement = connection.createStatement()) {

            statement.executeUpdate("DELETE FROM auth");
            statement.executeUpdate("DELETE FROM game");
            statement.executeUpdate("DELETE FROM user");

        } catch (Exception e) {
            throw dbError("clear", e);
        }
    }

    @Override
    public void insertUser(UserData user) throws DataAccessException {
        String sql = "INSERT INTO user (username, password_hash, email) VALUES (?, ?, ?)";

        try (var connection = DatabaseManager.getConnection();
             var statement = connection.prepareStatement(sql)) {

            statement.setString(1, user.username());
            statement.setString(2, user.password());
            statement.setString(3, user.email());
            statement.executeUpdate();

        } catch (SQLIntegrityConstraintViolationException e) {
            throw new DataAccessException("user already exists", e);
        } catch (Exception e) {
            throw dbError("insertUser", e);
        }
    }

    @Override
    public UserData getUser(String username) throws DataAccessException {
        String sql = "SELECT username, password_hash, email FROM user WHERE username = ?";

        try (var connection = DatabaseManager.getConnection();
             var statement = connection.prepareStatement(sql)) {

            statement.setString(1, username);

            try (var rs = statement.executeQuery()) {
                if (!rs.next()) return null;
                return new UserData(
                        rs.getString("username"),
                        rs.getString("password_hash"),
                        rs.getString("email")
                );
            }

        } catch (Exception e) {
            throw dbError("getUser", e);
        }
    }

    @Override
    public void insertAuth(AuthData auth) throws DataAccessException {

    }

    @Override
    public AuthData getAuth(String authToken) throws DataAccessException {
        return null;
    }

    @Override
    public void deleteAuth(String authToken) throws DataAccessException {

    }

    @Override
    public int insertGame(GameData data) throws DataAccessException {
        return 0;
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

    private DataAccessException dbError(String where, Exception e) {
        return new DataAccessException(where + ": " + e.getMessage(), e);
    }
}
