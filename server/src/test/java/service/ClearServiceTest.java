package service;

import dataaccess.DataAccess;
import dataaccess.MemoryDataAccess;
import model.UserData;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ClearServiceTest {

    @Test
    void clear_positive_clearsAllData() throws Exception {
        DataAccess data = new MemoryDataAccess();

        data.insertUser(new UserData("matt", "pw", "matt@email.com"));
        assertNotNull(data.getUser("matt"));

        ClearService service = new ClearService(data);
        service.clear();

        assertNull(data.getUser("matt"));
        assertTrue(data.listGames().isEmpty());
    }
}
