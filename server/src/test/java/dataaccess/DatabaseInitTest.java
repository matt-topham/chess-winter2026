package dataaccess;

import org.junit.jupiter.api.Test;

import java.sql.ResultSet;

import static org.junit.jupiter.api.Assertions.*;

public class DatabaseInitTest {

    @Test
    void initialize_createsDatabaseAndTables() throws Exception {
        try (var conn = DatabaseManager.getConnection();
             var stmt = conn.createStatement()) {
            stmt.executeUpdate("DROP DATABASE IF EXISTS chess");
        } catch (Exception ignored) {
        }

        DatabaseManager.initialize();

        try (var conn = DatabaseManager.getConnection();
             var stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SHOW TABLES")) {

            boolean hasUser = false, hasAuth = false, hasGame = false;

            while (rs.next()) {
                String name = rs.getString(1);
                if (name.equalsIgnoreCase("user")) hasUser = true;
                if (name.equalsIgnoreCase("auth")) hasAuth = true;
                if (name.equalsIgnoreCase("game")) hasGame = true;
            }

            assertTrue(hasUser, "Missing user table");
            assertTrue(hasAuth, "Missing auth table");
            assertTrue(hasGame, "Missing game table");
        }
    }
}