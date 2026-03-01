package server;

import dataaccess.*;
import io.javalin.Javalin;
import io.javalin.http.Context;
import com.google.gson.Gson;
import service.*;

import java.util.Map;

import javax.xml.crypto.Data;

public class Server {

    private final Javalin javalin;
    private final Gson gson = new Gson();

    public Server() {
        javalin = Javalin.create(config -> config.staticFiles.add("web"));

        DataAccess data = new MemoryDataAccess();
        ClearService clearService = new ClearService(data);
        UserService userService = new UserService(data);
        GameService gameService = new GameService(data);
        // Clear
        javalin.delete("/db", ctx -> {
            clearService.clear();
            okEmpty(ctx);
        });
        // Register
        javalin.post("/user", ctx -> {
            RegisterRequest request = gson.fromJson(ctx.body(), RegisterRequest.class);
            RegisterResult result = userService.register(request);
            okJson(ctx, result);
        });
        // Login
        javalin.post("/session", ctx -> {
            LoginRequest request = gson.fromJson(ctx.body(), LoginRequest.class);
            LoginResult result = userService.login(request);
            okJson(ctx, result);
        });
        // Logout
        javalin.delete("/session", ctx -> {
            String token = ctx.header("authorization");
            userService.logout(new LogoutRequest(token));
            okEmpty(ctx);
        });
        // List games
        javalin.get("/game", ctx -> {
            String token = ctx.header("authorization");
            ListGameResult result = gameService.listGames(token);
            okJson(ctx, result);
        });
        // Create game
        javalin.post("/game", ctx -> {
            String token = ctx.header("authorization");
            CreateGameRequest body = gson.fromJson(ctx.body(), CreateGameRequest.class);

            CreateGameResult result = gameService.createGame(new CreateGameRequest(token, body.gameName()));
            okJson(ctx, result);
        });
        // Join game
        javalin.put("/game", ctx -> {
            String token = ctx.header("authorization");
            JoinGameRequest body = gson.fromJson(ctx.body(), JoinGameRequest.class);

            gameService.joinGame(new JoinGameRequest(token, body.gameID(), body.playerColor()));
            okEmpty(ctx);
        });

        javalin.exception(BadRequestException.class,
                (e, ctx) -> err(ctx, 400, "Error: Bad request"));
        javalin.exception(UnauthorizedException.class,
                (e, ctx) -> err(ctx, 401, "Error: Unauthorized"));
        javalin.exception(AlreadyTakenException.class,
                (e, ctx) -> err(ctx, 403, "Error: Already taken"));
        javalin.exception(DataAccessException.class,
                (e, ctx) -> err(ctx, 500, "Error: " + safeMsg(e)));
        javalin.exception(Exception.class,
                (e, ctx) -> err(ctx, 500, "Error: " + safeMsg(e)));
    }

    private void okJson(Context ctx, Object obj) {
        ctx.status(200);
        ctx.contentType("application/json");
        ctx.result(gson.toJson(obj));
    }

    private static String safeMsg(Exception e) {
        return(e.getMessage() == null || e.getMessage().isBlank() ? "unknown error" : e.getMessage());
    }

    private void err(Context ctx, int status, String message) {
        ctx.status(status);
        ctx.contentType("application/json");
        ctx.result(gson.toJson(Map.of("message", message)));
    }

    private static void okEmpty(Context ctx) {
        ctx.status(200);
        ctx.contentType("application/json");
        ctx.result("{}");
    }

    public int run(int desiredPort) {
        javalin.start(desiredPort);
        return javalin.port();
    }

    public void stop() {
        javalin.stop();
    }
}
