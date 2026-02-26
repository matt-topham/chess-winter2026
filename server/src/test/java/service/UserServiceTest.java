package service;

import dataaccess.AlreadyTakenException;
import dataaccess.DataAccess;
import dataaccess.MemoryDataAccess;
import model.AuthData;
import model.UserData;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class UserServiceTest {

    @Test
    void registerPositiveCreateUsersAndAuth() throws Exception {
        DataAccess data = new MemoryDataAccess();
        UserService service = new UserService(data);

        RegisterRequest request = new RegisterRequest("matt", "pw", "matt@email.com");
        RegisterResult result = service.register(request);

        assertEquals("matt", result.username());
        assertNotNull(result.authToken());
        assertFalse(result.authToken().isBlank());

        UserData user = data.getUser("matt");
        assertNotNull(user);
        assertEquals("matt@email.com", user.email());

        AuthData auth = data.getAuth(result.authToken());
        assertNotNull(auth);
        assertEquals("matt", auth.username());

    }

    @Test
    void registerNegativeAlreadyTaken() throws Exception {
        DataAccess data = new MemoryDataAccess();
        UserService service = new UserService(data);

        service.register(new RegisterRequest("matt", "pw", "matt@email.com"));

        assertThrows(AlreadyTakenException.class, ()->
            service.register(new RegisterRequest("matt", "pw", "matt@email.com")));
    }
}
