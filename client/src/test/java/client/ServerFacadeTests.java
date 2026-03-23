package client;

import model.AuthData;
import model.GameData;
import org.junit.jupiter.api.*;
import server.Server;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ServerFacadeTests {

    private static Server server;
    private static ServerFacade facade;

    @BeforeAll
    public static void init() {
        server = new Server();
        int port = server.run(0);
        System.out.println("Started test HTTP server on " + port);
        facade = new ServerFacade("localhost", port);
    }

    @AfterAll
    static void stopServer() {
        server.stop();
    }

    @BeforeEach
    void clearDb() throws Exception {
        facade.clear();
    }

    @Test
    void clearPositive() throws Exception {
        facade.clear();
        assertTrue(true);
    }

    @Test
    void registerPositive() throws Exception {
        AuthData auth = facade.register("u1", "pw", "u1@mail.com");
        assertNotNull(auth);
        assertEquals("u1", auth.username());
        assertNotNull(auth.authToken());
        assertTrue(auth.authToken().length() > 10);
    }

    @Test
    void registerNegative() throws Exception {
        facade.register("u1", "pw", "u1@mail.com");
        assertThrows(ServerFacade.ClientException.class, () ->
                facade.register("u1", "pw2", "u2@mail.com"));
    }

    @Test
    void loginPositive() throws Exception {
        facade.register("u1", "pw", "u1@mail.com");
        AuthData auth = facade.login("u1", "pw");
        assertNotNull(auth);
        assertEquals("u1", auth.username());
        assertTrue(auth.authToken().length() > 10);
    }

    @Test
    void loginNegative() throws Exception {
        facade.register("u1", "pw", "u1@mail.com");
        assertThrows(ServerFacade.ClientException.class, () ->
                facade.login("u1", "wrong"));
    }

    @Test
    void logoutPositive() throws Exception {
        AuthData auth = facade.register("u1", "pw", "u1@mail.com");
        facade.logout(auth.authToken());
        assertTrue(true);
    }

    @Test
    void logoutNegative() {
        assertThrows(ServerFacade.ClientException.class, () ->
                facade.logout("not-a-real-token"));
    }

    @Test
    void createGamePositive() throws Exception {
        AuthData auth = facade.register("u1", "pw", "u1@mail.com");
        int gameId = facade.createGame(auth.authToken(), "My Game");
        assertTrue(gameId > 0);
    }

    @Test
    void createGameNegative() {
        assertThrows(ServerFacade.ClientException.class, () ->
                facade.createGame("bad-token", "My Game"));
    }

    @Test
    void listGamesPositive() throws Exception {
        AuthData auth = facade.register("u1", "pw", "u1@mail.com");
        facade.createGame(auth.authToken(), "A");
        facade.createGame(auth.authToken(), "B");

        GameData[] games = facade.listGames(auth.authToken());
        assertNotNull(games);
        assertEquals(2, games.length);
    }

    @Test
    void listGamesNegative() {
        assertThrows(ServerFacade.ClientException.class, () ->
                facade.listGames("bad-token"));
    }
}