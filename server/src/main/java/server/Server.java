package server;

import com.google.gson.Gson;
import dataaccess.*;
import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.websocket.WsContext;
import io.javalin.websocket.WsMessageContext;
import model.AuthData;
import model.GameData;
import service.*;
import chess.ChessGame;
import chess.InvalidMoveException;
import websocket.commands.*;
import websocket.messages.*;

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

        initDatabase();

        DataAccess data = new MySqlDataAccess();
        ClearService clearService = new ClearService(data);
        UserService userService = new UserService(data);
        GameService gameService = new GameService(data);

        registerWebSocket(data);
        registerHttp(clearService, userService, gameService);
        registerExceptionHandlers();
    }

    private void initDatabase() {
        try {
            DatabaseManager.initialize();
        } catch (DataAccessException e) {
            throw new RuntimeException(e);
        }
    }

    private void registerWebSocket(DataAccess data) {
        javalin.ws("/ws", ws -> {
            ws.onMessage(ctx -> onWsMessage(ctx, data));
            ws.onClose(this::onWsClose);
        });
    }

    private void registerHttp(ClearService clearService, UserService userService, GameService gameService) {
        javalin.delete("/db", ctx -> {
            clearService.clear();
            okEmpty(ctx);
        });

        javalin.post("/user", ctx -> {
            RegisterRequest request = gson.fromJson(ctx.body(), RegisterRequest.class);
            RegisterResult result = userService.register(request);
            okJson(ctx, result);
        });

        javalin.post("/session", ctx -> {
            LoginRequest request = gson.fromJson(ctx.body(), LoginRequest.class);
            LoginResult result = userService.login(request);
            okJson(ctx, result);
        });

        javalin.delete("/session", ctx -> {
            String token = ctx.header("authorization");
            userService.logout(new LogoutRequest(token));
            okEmpty(ctx);
        });

        javalin.get("/game", ctx -> {
            String token = ctx.header("authorization");
            ListGameResult result = gameService.listGames(token);
            okJson(ctx, result);
        });

        javalin.post("/game", ctx -> {
            String token = ctx.header("authorization");
            CreateGameRequest body = gson.fromJson(ctx.body(), CreateGameRequest.class);
            CreateGameResult result = gameService.createGame(new CreateGameRequest(token, body.gameName()));
            okJson(ctx, result);
        });

        javalin.put("/game", ctx -> {
            String token = ctx.header("authorization");
            JoinGameRequest body = gson.fromJson(ctx.body(), JoinGameRequest.class);
            gameService.joinGame(new JoinGameRequest(token, body.gameID(), body.playerColor()));
            okEmpty(ctx);
        });
    }

    private void registerExceptionHandlers() {
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


    private void onWsMessage(WsMessageContext ctx, DataAccess data) {
        String json = ctx.message();
        try {
            UserGameCommand base = gson.fromJson(json, UserGameCommand.class);
            if (base == null || base.getCommandType() == null) {
                sendError(ctx, "Error: invalid command");
                return;
            }

            switch (base.getCommandType()) {
                case CONNECT -> handleConnect(ctx, json, data);
                case MAKE_MOVE -> handleMakeMove(ctx, json, data);
                case LEAVE -> handleLeave(ctx, json, data);
                case RESIGN -> handleResign(ctx, json, data);
                default -> sendError(ctx, "Error: unsupported command");
            }
        } catch (Exception e) {
            sendError(ctx, "Error: " + safeMsg(e));
        }
    }

    private void onWsClose(WsContext ctx) {
        ConnInfo info = connInfoBySession.remove(ctx);
        if (info == null) {
            sessionsByGame.values().forEach(set -> set.remove(ctx));
            return;
        }

        Set<WsContext> set = sessionsByGame.get(info.gameId());
        if (set != null) {
            set.remove(ctx);
            if (set.isEmpty()) {
                sessionsByGame.remove(info.gameId());
            }
        }
    }

    private void handleConnect(WsContext ctx, String json, DataAccess data) throws DataAccessException {
        ConnectCommand cmd = gson.fromJson(json, ConnectCommand.class);
        String token = (cmd == null) ? null : cmd.getAuthToken();
        if (isBlank(token)) {
            sendError(ctx, "Error: unauthorized");
            return;
        }

        Integer gameIdObj = (cmd == null) ? null : cmd.getGameID();
        if (gameIdObj == null) {
            sendError(ctx, "Error: bad request");
            return;
        }
        int gameId = gameIdObj;

        AuthData auth = data.getAuth(token);
        String username = (auth == null) ? null : auth.username();
        if (isBlank(username)) {
            sendError(ctx, "Error: unauthorized");
            return;
        }

        GameData game = data.getGame(gameId);
        if (game == null || game.game() == null) {
            sendError(ctx, "Error: bad request");
            return;
        }

        String role = normalizeRole(cmd.getPlayerColor(), username, game);
        if (role == null) {
            sendError(ctx, "Error: bad request");
            return;
        }

        sessionsByGame.computeIfAbsent(gameId, k -> ConcurrentHashMap.newKeySet()).add(ctx);
        connInfoBySession.put(ctx, new ConnInfo(username, gameId, role));

        ctx.send(gson.toJson(new LoadGameMessage(game.game())));

        broadcastExcept(gameId, ctx, new NotificationMessage(username + " joined as " + role));
    }

    private void handleLeave(WsContext ctx, String json, DataAccess data) throws DataAccessException {
        ConnInfo info = connInfoBySession.get(ctx);
        if (info == null) {
            return;
        }

        LeaveCommand cmd = gson.fromJson(json, LeaveCommand.class);
        String token = (cmd == null) ? null : cmd.getAuthToken();
        if (isBlank(token) || !authMatchesUser(data, token, info.username())) {
            sendError(ctx, "Error: unauthorized");
            return;
        }

        removeSessionFromGame(ctx, info.gameId());

        connInfoBySession.remove(ctx);

        if (isPlayerRole(info.role())) {
            clearPlayerSpotIfOwned(data, info.gameId(), info.username(), info.role());
        }

        broadcastExcept(info.gameId(), ctx, new NotificationMessage(info.username() + " left the game"));
    }

    private void handleResign(WsContext ctx, String json, DataAccess data) throws DataAccessException {
        ConnInfo info = connInfoBySession.get(ctx);
        if (info == null) {
            sendError(ctx, "Error: unauthorized");
            return;
        }
        if (!isPlayerRole(info.role())) {
            sendError(ctx, "Error: unauthorized");
            return;
        }

        ResignCommand cmd = gson.fromJson(json, ResignCommand.class);
        String token = (cmd == null) ? null : cmd.getAuthToken();
        if (isBlank(token) || !authMatchesUser(data, token, info.username())) {
            sendError(ctx, "Error: unauthorized");
            return;
        }

        GameData gameData = data.getGame(info.gameId());
        if (gameData == null || gameData.game() == null) {
            sendError(ctx, "Error: bad request");
            return;
        }

        ChessGame game = gameData.game();
        if (game.isGameOver()) {
            sendError(ctx, "Error: game is over");
            return;
        }

        ChessGame.TeamColor resigningTeam = "WHITE".equals(info.role())
                ? ChessGame.TeamColor.WHITE
                : ChessGame.TeamColor.BLACK;
        ChessGame.TeamColor winner = (resigningTeam == ChessGame.TeamColor.WHITE)
                ? ChessGame.TeamColor.BLACK
                : ChessGame.TeamColor.WHITE;

        game.setGameOver(true);
        game.setWinner(winner);

        data.updateGame(new GameData(
                gameData.gameID(),
                gameData.whiteUsername(),
                gameData.blackUsername(),
                gameData.gameName(),
                game
        ));

        broadcast(info.gameId(), new NotificationMessage(info.username() + " resigned. " + winner + " wins."));
    }

    private void handleMakeMove(WsContext ctx, String json, DataAccess data) throws DataAccessException {
        ConnInfo info = connInfoBySession.get(ctx);
        if (info == null) {
            sendError(ctx, "Error: unauthorized");
            return;
        }
        if (!isPlayerRole(info.role())) {
            sendError(ctx, "Error: unauthorized");
            return;
        }

        MakeMoveCommand cmd = gson.fromJson(json, MakeMoveCommand.class);
        String token = (cmd == null) ? null : cmd.getAuthToken();
        if (isBlank(token) || !authMatchesUser(data, token, info.username())) {
            sendError(ctx, "Error: unauthorized");
            return;
        }
        if (cmd.getMove() == null) {
            sendError(ctx, "Error: bad request");
            return;
        }

        GameData gameData = data.getGame(info.gameId());
        if (gameData == null || gameData.game() == null) {
            sendError(ctx, "Error: bad request");
            return;
        }

        ChessGame game = gameData.game();
        if (game.isGameOver()) {
            sendError(ctx, "Error: game is over");
            return;
        }

        ChessGame.TeamColor mover = "WHITE".equals(info.role()) ? ChessGame.TeamColor.WHITE : ChessGame.TeamColor.BLACK;
        if (game.getTeamTurn() != mover) {
            sendError(ctx, "Error: not your turn");
            return;
        }

        try {
            game.makeMove(cmd.getMove());
        } catch (InvalidMoveException e) {
            sendError(ctx, "Error: invalid move");
            return;
        }

        ChessGame.TeamColor opponent = (mover == ChessGame.TeamColor.WHITE)
                ? ChessGame.TeamColor.BLACK
                : ChessGame.TeamColor.WHITE;

        boolean checkmate = game.isInCheckmate(opponent);
        boolean stalemate = !checkmate && game.isInStalemate(opponent);
        boolean check = !checkmate && !stalemate && game.isInCheck(opponent);

        if (checkmate) {
            game.setGameOver(true);
            game.setWinner(mover);
        } else if (stalemate) {
            game.setGameOver(true);
            game.setWinner(null);
        }

        data.updateGame(new GameData(
                gameData.gameID(),
                gameData.whiteUsername(),
                gameData.blackUsername(),
                gameData.gameName(),
                game
        ));

        broadcast(info.gameId(), new LoadGameMessage(game));

        String moveText = cmd.getMove().getStartPosition() + " to " + cmd.getMove().getEndPosition();
        broadcastExcept(info.gameId(), ctx, new NotificationMessage(info.username() + " moved " + moveText));

        if (checkmate) {
            broadcast(info.gameId(), new NotificationMessage(opponent + " is in checkmate"));
        } else if (stalemate) {
            broadcast(info.gameId(), new NotificationMessage("Stalemate"));
        } else if (check) {
            broadcast(info.gameId(), new NotificationMessage(opponent + " is in check"));
        }
    }

    private String normalizeRole(String requestedRole, String username, GameData game) {
        if (requestedRole == null || requestedRole.isBlank()) {
            if (username.equals(game.whiteUsername())) return "WHITE";
            if (username.equals(game.blackUsername())) return "BLACK";
            return "OBSERVER";
        }

        String role = requestedRole.trim().toUpperCase();
        if (role.equals("WHITE") || role.equals("BLACK") || role.equals("OBSERVER")) {
            return role;
        }
        return null;
    }

    private boolean authMatchesUser(DataAccess data, String authToken, String username) throws DataAccessException {
        AuthData auth = data.getAuth(authToken);
        return auth != null && username != null && username.equals(auth.username());
    }

    private void removeSessionFromGame(WsContext ctx, int gameId) {
        Set<WsContext> set = sessionsByGame.get(gameId);
        if (set == null) return;

        set.remove(ctx);
        if (set.isEmpty()) {
            sessionsByGame.remove(gameId);
        }
    }

    private boolean isPlayerRole(String role) {
        return "WHITE".equals(role) || "BLACK".equals(role);
    }

    private void clearPlayerSpotIfOwned(DataAccess data, int gameId, String username, String role) throws DataAccessException {
        GameData game = data.getGame(gameId);
        if (game == null) return;

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

        if (!changed) return;

        data.updateGame(new GameData(
                game.gameID(),
                white,
                black,
                game.gameName(),
                game.game()
        ));
    }

    private void sendError(WsContext ctx, String message) {
        ctx.send(gson.toJson(new ErrorMessage(message)));
    }

    private void broadcastExcept(int gameId, WsContext exceptCtx, Object messageObj) {
        String msg = gson.toJson(messageObj);
        String exceptId = exceptCtx.sessionId();
        for (WsContext s : sessionsByGame.getOrDefault(gameId, Set.of())) {
            if (!s.sessionId().equals(exceptId)) {
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

    private void okJson(Context ctx, Object obj) {
        ctx.status(200);
        ctx.contentType("application/json");
        ctx.result(gson.toJson(obj));
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

    private static String safeMsg(Exception e) {
        return (e.getMessage() == null || e.getMessage().isBlank()) ? "unknown error" : e.getMessage();
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }


    public int run(int desiredPort) {
        javalin.start(desiredPort);
        return javalin.port();
    }

    public void stop() {
        javalin.stop();
    }
}