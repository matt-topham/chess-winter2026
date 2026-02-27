package service;

import dataaccess.DataAccess;
import dataaccess.MemoryDataAccess;
import dataaccess.UnauthorizedException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class GameServiceTest {
    private static String registerAndLogin(UserService userService) throws Exception {
        userService.register(new RegisterRequest("matt", "pw", "matt@email.com"));
        return userService.login(new LoginRequest("matt", "pw")).authToken();
    }

    @Test
    void listGamesPositive() throws Exception {
        DataAccess data = new MemoryDataAccess();
        UserService userService = new UserService(data);
        GameService gameService = new GameService(data);

        String token = registerAndLogin(userService);

        var res = gameService.listGames(token);
        assertNotNull(res.games());
        assertTrue(res.games().isEmpty());
    }

    @Test
    void listGamesNegative() {
        DataAccess data = new MemoryDataAccess();
        GameService gameService = new GameService(data);

        assertThrows(UnauthorizedException.class, () ->
                gameService.listGames("nope"));
    }
}
