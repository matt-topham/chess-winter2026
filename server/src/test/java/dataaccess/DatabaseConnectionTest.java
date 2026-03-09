package dataaccess;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class DatabaseConnectionTest {

    @Test
    void canConnect() throws Exception {
        try (var conn = DatabaseManager.getConnection();
             var stmt = conn.prepareStatement("SELECT 1+1");
             var rs = stmt.executeQuery()) {

            rs.next();
            assertEquals(2, rs.getInt(1));
        }
    }
}