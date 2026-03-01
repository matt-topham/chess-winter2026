package service;

import dataaccess.*;
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

    @Test
    void createGamePositive() throws Exception {
        DataAccess data = new MemoryDataAccess();
        UserService userService = new UserService(data);
        GameService gameService = new GameService(data);

        String token = registerAndLogin(userService);

        var res = gameService.createGame(new CreateGameRequest(token, "Game"));
        assertTrue(res.gameID() > 0);
        assertNotNull(data.getGame(res.gameID()));
        assertEquals("Game", data.getGame(res.gameID()).gameName());
    }

    @Test
    void createGameNegative() throws Exception {
        DataAccess data = new MemoryDataAccess();
        UserService userService = new UserService(data);
        GameService gameService = new GameService(data);

        String token = registerAndLogin(userService);

        assertThrows(BadRequestException.class, () ->
                gameService.createGame(new CreateGameRequest(token, "    ")));
    }

    @Test
    void joinGamePositive () throws Exception {
        DataAccess data = new MemoryDataAccess();
        UserService userService = new UserService(data);
        GameService gameService = new GameService(data);

        String token = registerAndLogin(userService);

        int gameID = gameService.createGame(new CreateGameRequest(token, "Game1")).gameID();
        gameService.joinGame(new JoinGameRequest(token, gameID, "WHITE"));

        assertEquals("matt", data.getGame(gameID).whiteUsername());
    }

    @Test
    void joinGameNegative() throws Exception {
        DataAccess data = new MemoryDataAccess();
        UserService userService = new UserService(data);
        GameService gameService = new GameService(data);

        String token = registerAndLogin(userService);

        int gameID = gameService.createGame(new CreateGameRequest(token, "Game1")).gameID();
        gameService.joinGame(new JoinGameRequest(token, gameID, "WHITE"));

        assertThrows(AlreadyTakenException.class, () ->
                gameService.joinGame(new JoinGameRequest(token, gameID, "WHITE")));
    }
}
