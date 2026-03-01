package server;

import dataaccess.DataAccess;
import dataaccess.DataAccessException;
import dataaccess.MemoryDataAccess;
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
            ctx.status(200).json(result);
        });
        // Login
        javalin.post("/session", ctx -> {
            LoginRequest request = gson.fromJson(ctx.body(), LoginRequest.class);
            LoginResult result = userService.login(request);
            ctx.status(200).json(result);
        });

    }

    private static void okEmpty(Context ctx) {
        ctx.status(200).json(Map.of());
    }

    public int run(int desiredPort) {
        javalin.start(desiredPort);
        return javalin.port();
    }

    public void stop() {
        javalin.stop();
    }
}
