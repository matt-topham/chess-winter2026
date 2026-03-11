package dataaccess;

import chess.ChessGame;
import com.google.gson.Gson;
import model.AuthData;
import model.GameData;
import model.UserData;

import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;

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
                if (!rs.next()) {
                    return null;
                }
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
        String sql = "INSERT INTO auth (auth_token, username) VALUES (?, ?)";

        try (var connection = DatabaseManager.getConnection();
             var statement = connection.prepareStatement(sql)) {

            statement.setString(1, auth.authToken());
            statement.setString(2, auth.username());
            statement.executeUpdate();

        } catch (SQLIntegrityConstraintViolationException e) {
            throw new DataAccessException("auth token already exists", e);
        } catch (Exception e) {
            throw dbError("insertAuth", e);
        }
    }

    @Override
    public AuthData getAuth(String token) throws DataAccessException {
        String sql = "SELECT auth_token, username FROM auth WHERE auth_token = ?";

        try (var connection = DatabaseManager.getConnection();
             var statement = connection.prepareStatement(sql)) {

            statement.setString(1, token);

            try (var rs = statement.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                return new AuthData(rs.getString("auth_token"), rs.getString("username"));
            }

        } catch (Exception e) {
            throw dbError("getAuth", e);
        }
    }

    @Override
    public void deleteAuth(String token) throws DataAccessException {
        String sql = "DELETE FROM auth WHERE auth_token = ?";

        try (var connection = DatabaseManager.getConnection();
             var statement = connection.prepareStatement(sql)) {

            statement.setString(1, token);
            statement.executeUpdate();

        } catch (Exception e) {
            throw dbError("deleteAuth", e);
        }
    }

    @Override
    public int insertGame(GameData game) throws DataAccessException {
        String sql = """
        INSERT INTO game (game_name, white_username, black_username, game_json)
        VALUES (?, ?, ?, ?)
        """;

        try (var connection = DatabaseManager.getConnection();
             var statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, game.gameName());
            statement.setString(2, game.whiteUsername());
            statement.setString(3, game.blackUsername());
            statement.setString(4, gson.toJson(game.game()));

            statement.executeUpdate();

            try (var keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
            throw new DataAccessException("insertGame: no generated key");

        } catch (Exception e) {
            throw dbError("insertGame", e);
        }
    }

    @Override
    public GameData getGame(int gameID) throws DataAccessException {
        String sql = """
        SELECT game_id, game_name, white_username, black_username, game_json
        FROM game
        WHERE game_id = ?
        """;

        try (var connection = DatabaseManager.getConnection();
             var statement = connection.prepareStatement(sql)) {

            statement.setInt(1, gameID);

            try (var rs = statement.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }

                ChessGame state = gson.fromJson(rs.getString("game_json"), ChessGame.class);

                return new GameData(
                        rs.getInt("game_id"),
                        rs.getString("white_username"),
                        rs.getString("black_username"),
                        rs.getString("game_name"),
                        state
                );
            }

        } catch (Exception e) {
            throw dbError("getGame", e);
        }
    }

    @Override
    public Collection<GameData> listGames() throws DataAccessException {
        String sql = "SELECT game_id, game_name, white_username, black_username, game_json FROM game";

        try (var connection = DatabaseManager.getConnection();
             var statement = connection.prepareStatement(sql);
             var rs = statement.executeQuery()) {

            var out = new ArrayList<GameData>();
            while (rs.next()) {
                ChessGame state = gson.fromJson(rs.getString("game_json"), ChessGame.class);
                out.add(new GameData(
                        rs.getInt("game_id"),
                        rs.getString("white_username"),
                        rs.getString("black_username"),
                        rs.getString("game_name"),
                        state
                ));
            }
            return out;

        } catch (Exception e) {
            throw dbError("listGames", e);
        }
    }

    @Override
    public void updateGame(GameData game) throws DataAccessException {
        String sql = """
        UPDATE game
        SET game_name = ?, white_username = ?, black_username = ?, game_json = ?
        WHERE game_id = ?
        """;

        try (var connection = DatabaseManager.getConnection();
             var statement = connection.prepareStatement(sql)) {

            statement.setString(1, game.gameName());
            statement.setString(2, game.whiteUsername());
            statement.setString(3, game.blackUsername());
            statement.setString(4, gson.toJson(game.game()));
            statement.setInt(5, game.gameID());

            int rows = statement.executeUpdate();
            if (rows == 0) {
                throw new DataAccessException("updateGame: game not found");
            }

        } catch (DataAccessException e) {
            throw e;
        } catch (Exception e) {
            throw dbError("updateGame", e);
        }
    }

    private DataAccessException dbError(String where, Exception e) {
        return new DataAccessException(where + ": " + e.getMessage(), e);
    }
}
