package dataaccess;

import chess.ChessGame;
import model.AuthData;
import model.GameData;
import model.UserData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;


import java.util.Collection;

import static org.junit.jupiter.api.Assertions.*;

public class MySqlDataAccessTest {
    private DataAccess dao;

    @BeforeEach
    void setUp() throws Exception {
        DatabaseManager.initialize();

        dao = new MySqlDataAccess();
        dao.clear();
    }

    @Test
    void clearPositive() throws Exception {
        dao.insertUser(new UserData("u", "hash", "e"));
        dao.insertAuth(new AuthData("t", "u"));
        int id = dao.insertGame(
                new GameData(0, null, null, "g", new ChessGame()));

        dao.clear();

        assertNull(dao.getUser("u"));
        assertNull(dao.getAuth("t"));
        assertNull(dao.getGame(id));
        assertTrue(dao.listGames().isEmpty());
    }

    @Test
    void insertUserPositive() throws Exception {
        dao.insertUser(new UserData("u1", "hash", "u1@mail.com"));

        UserData u = dao.getUser("u1");
        assertNotNull(u);
        assertEquals("u1", u.username());
        assertEquals("u1@mail.com", u.email());
        assertEquals("hash", u.password());
    }

    @Test
    void insertUserNegative() throws Exception {
        dao.insertUser(new UserData("u1", "hash", "u1@mail.com"));

        assertThrows(DataAccessException.class, () ->
                dao.insertUser(new UserData("u1", "hash2", "u2@mail.com")));
    }

    @Test
    void getUserNegative() throws Exception {
        assertNull(dao.getUser("does_not_exist"));
    }

    @Test
    void insertAuthPositive() throws Exception {
        dao.insertUser(new UserData("u1", "hash", "u1@mail.com"));
        dao.insertAuth(new AuthData("token123", "u1"));

        AuthData a = dao.getAuth("token123");
        assertNotNull(a);
        assertEquals("token123", a.authToken());
        assertEquals("u1", a.username());
    }

    @Test
    void insertAuthNegative() throws Exception {
        dao.insertUser(new UserData("u1", "hash", "u1@mail.com"));
        dao.insertAuth(new AuthData("token123", "u1"));

        assertThrows(DataAccessException.class, () ->
                dao.insertAuth(new AuthData("token123", "u1")));
    }

    @Test
    void getAuthNegative() throws Exception {
        assertNull(dao.getAuth("missing_token"));
    }

    @Test
    void deleteAuthPositive() throws Exception {
        dao.insertUser(new UserData("u1", "hash", "u1@mail.com"));
        dao.insertAuth(new AuthData("token123", "u1"));

        dao.deleteAuth("token123");
        assertNull(dao.getAuth("token123"));
    }

    @Test
    void deleteAuthNegative() throws Exception {
        dao.deleteAuth("missing_token");
        assertNull(dao.getAuth("missing_token"));
    }

    @Test
    void insertGamePositive() throws Exception {
        int id = dao.insertGame(new GameData(0, null, null, "Game1", new ChessGame()));

        GameData g = dao.getGame(id);
        assertNotNull(g);
        assertEquals(id, g.gameID());
        assertEquals("Game1", g.gameName());
        assertNull(g.whiteUsername());
        assertNull(g.blackUsername());
        assertNotNull(g.game());
    }

    @Test
    void getGameNegative() throws Exception {
        assertNull(dao.getGame(9999999));
    }

    @Test
    void listGamesPositive() throws Exception {
        dao.insertGame(new GameData(0, null, null, "A", new ChessGame()));
        dao.insertGame(new GameData(0, null, null, "B", new ChessGame()));

        Collection<GameData> games = dao.listGames();
        assertNotNull(games);
        assertEquals(2, games.size());
    }

    @Test
    void listGamesNegative() throws Exception {
        dao.insertGame(new GameData(0, null, null, "A", new ChessGame()));
        dao.clear();

        assertTrue(dao.listGames().isEmpty());
    }

    @Test
    void updateGamePositive() throws Exception {
        int id = dao.insertGame(new GameData(0, null, null, "Game1", new ChessGame()));
        GameData g = dao.getGame(id);

        GameData updated = new GameData(
                id,
                "whitePlayer",
                "blackPlayer",
                g.gameName(),
                g.game()
        );

        dao.updateGame(updated);

        GameData after = dao.getGame(id);
        assertNotNull(after);
        assertEquals("whitePlayer", after.whiteUsername());
        assertEquals("blackPlayer", after.blackUsername());
        assertEquals("Game1", after.gameName());
        assertNotNull(after.game());
    }

    @Test
    void updateGameNegative() {
        assertThrows(DataAccessException.class, () ->
                dao.updateGame(new GameData(1234567, null, null, "Nope", new ChessGame())));
    }
}
