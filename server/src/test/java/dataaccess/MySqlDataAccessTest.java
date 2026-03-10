package dataaccess;

import chess.ChessGame;
import model.AuthData;
import model.GameData;
import model.UserData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;


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
}
