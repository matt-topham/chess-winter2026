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
}
