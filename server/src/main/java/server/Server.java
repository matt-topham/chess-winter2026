package server;

import dataaccess.*;
import io.javalin.Javalin;
import io.javalin.http.Context;
import com.google.gson.Gson;
import io.javalin.websocket.WsContext;
import model.AuthData;
import model.GameData;
import service.*;
import websocket.commands.UserGameCommand;
import websocket.commands.ConnectCommand;
import websocket.messages.LoadGameMessage;
import websocket.messages.ErrorMessage;
import websocket.messages.NotificationMessage;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class Server {

    private final Javalin javalin;
    private final Gson gson = new Gson();
    private final ConcurrentHashMap<Integer, Set<WsContext>> sessionsByGame = new ConcurrentHashMap<>();

    private record ConnInfo(String username, int gameId, String role) {}
    private final Map<WsContext, ConnInfo> connInfoBySession = new ConcurrentHashMap<>();

    public Server() {
        javalin = Javalin.create(config -> config.staticFiles.add("web"));

        try {
            DatabaseManager.initialize();
        } catch (DataAccessException e) {
            throw new RuntimeException(e);
        }

        DataAccess data = new MySqlDataAccess();
        ClearService clearService = new ClearService(data);
        UserService userService = new UserService(data);
        GameService gameService = new GameService(data);

        // Websocket
        javalin.ws("/ws", ws -> {

            ws.onMessage(ctx -> {
                String json = ctx.message();

                try {
                    UserGameCommand base = gson.fromJson(json, UserGameCommand.class);

                    if (base == null || base.getCommandType() == null) {
                        ctx.send(gson.toJson(new ErrorMessage("Error: invalid command")));
                        return;
                    }

                    switch (base.getCommandType()) {
                        case CONNECT -> handleConnect(ctx, json, data);
                        case LEAVE -> handleLeave(ctx, data);
                        default -> ctx.send(gson.toJson(new ErrorMessage("Error: unsupported command")));
                    }

                } catch (Exception e) {
                    ctx.send(gson.toJson(new ErrorMessage("Error: " + safeMsg(e))));
                }
            });

            ws.onClose(ctx -> {
                connInfoBySession.remove(ctx);
                sessionsByGame.values().forEach(set -> set.remove(ctx));
            });
        });

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

    private void handleConnect(WsContext ctx, String json, DataAccess data) throws DataAccessException {

        ConnectCommand cmd = gson.fromJson(json, ConnectCommand.class);

        if (cmd == null || cmd.getAuthToken() == null || cmd.getAuthToken().isBlank()) {
            ctx.send(gson.toJson(new ErrorMessage("Error: unauthorized")));
            return;
        }

        int gameId = cmd.getGameID();
        String role = cmd.getPlayerColor();
        if (role == null || role.isBlank()) {
            ctx.send(gson.toJson(new ErrorMessage("Error: bad request")));
            return;
        }

        AuthData auth = data.getAuth(cmd.getAuthToken());
        if (auth == null || auth.username() == null || auth.username().isBlank()) {
            ctx.send(gson.toJson(new ErrorMessage("Error: unauthorized")));
            return;
        }
        String username = auth.username();

        GameData game = data.getGame(gameId);
        if (game == null || game.game() == null) {
            ctx.send(gson.toJson(new ErrorMessage("Error: bad request")));
            return;
        }

        sessionsByGame.computeIfAbsent(gameId, k -> ConcurrentHashMap.newKeySet()).add(ctx);
        connInfoBySession.put(ctx, new ConnInfo(username, gameId, role));

        ctx.send(gson.toJson(new LoadGameMessage(game.game())));

        broadcastExcept(gameId, ctx, new NotificationMessage(username + " joined as " + role));
    }

    private void broadcastExcept(int gameId, WsContext except, Object messageObj) {
        String msg = gson.toJson(messageObj);
        for (WsContext s : sessionsByGame.getOrDefault(gameId, Set.of())) {
            if (s != except) {
                s.send(msg);
            }
        }
    }

    private void broadcast(int gameId, Object messageObj) {
        String msg = gson.toJson(messageObj);
        for (WsContext s : sessionsByGame.getOrDefault(gameId, Set.of())) {
            s.send(msg);
        }
    }

    private void handleLeave(WsContext ctx, DataAccess data) throws DataAccessException {

        ConnInfo info = connInfoBySession.remove(ctx);
        if (info == null) {
            return;
        }

        int gameId = info.gameId();
        String username = info.username();
        String role = info.role();

        Set<WsContext> set = sessionsByGame.get(gameId);
        if (set != null) {
            set.remove(ctx);
            if (set.isEmpty()) {
                sessionsByGame.remove(gameId);
            }
        }

        if ("WHITE".equals(role) || "BLACK".equals(role)) {
            GameData game = data.getGame(gameId);
            if (game != null) {
                String white = game.whiteUsername();
                String black = game.blackUsername();

                boolean changed = false;
                if ("WHITE".equals(role) && username.equals(white)) {
                    white = null;
                    changed = true;
                }
                if ("BLACK".equals(role) && username.equals(black)) {
                    black = null;
                    changed = true;
                }

                if (changed) {
                    GameData updated = new GameData(
                            game.gameID(),
                            white,
                            black,
                            game.gameName(),
                            game.game()
                    );
                    data.updateGame(updated);
                }
            }
        }

        broadcast(gameId, new NotificationMessage(username + " left the game"));
    }
}
