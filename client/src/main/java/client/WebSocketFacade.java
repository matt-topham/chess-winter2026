package client;

import com.google.gson.Gson;
import websocket.commands.ConnectCommand;
import websocket.messages.ServerMessage;
import websocket.messages.LoadGameMessage;
import websocket.messages.NotificationMessage;
import websocket.messages.ErrorMessage;

import jakarta.websocket.*;
import java.net.URI;
import java.util.function.Consumer;

public class WebSocketFacade {

    private final Gson gson = new Gson();
    private Session session;

    private final Consumer<LoadGameMessage> onLoadGame;
    private final Consumer<String> onNotification;
    private final Consumer<String> onError;

    public WebSocketFacade(String host, int port,
                           Consumer<LoadGameMessage> onLoadGame,
                           Consumer<String> onNotification,
                           Consumer<String> onError) {
        this.onLoadGame = onLoadGame;
        this.onNotification = onNotification;
        this.onError = onError;

        try {
            URI uri = new URI("ws://" + host + ":" + port + "/ws");
            WebSocketContainer container = ContainerProvider.getWebSocketContainer();
            container.connectToServer(new Endpoint() {
                @Override
                public void onOpen(Session session, EndpointConfig config) {
                    WebSocketFacade.this.session = session;
                    session.addMessageHandler(String.class, WebSocketFacade.this::handleMessage);
                }

                @Override
                public void onError(Session session, Throwable thr) {
                    onError.accept("WebSocket error: " + thr.getMessage());
                }

                @Override
                public void onClose(Session session, CloseReason closeReason) {
                    onNotification.accept("Disconnected: " + closeReason.getReasonPhrase());
                }
            }, uri);

        } catch (Exception e) {
            throw new RuntimeException("Failed to connect websocket: " + e.getMessage(), e);
        }
    }

    public void connectGame(String authToken, int gameId, String playerColor) {
        send(new ConnectCommand(authToken, gameId, playerColor)); // WHITE/BLACK/OBSERVER
    }

    public void close() {
        try {
            if (session != null) session.close();
        } catch (Exception ignored) {}
    }

    private void send(Object command) {
        try {
            if (session == null) {
                onError.accept("Error: websocket not connected");
                return;
            }
            session.getBasicRemote().sendText(gson.toJson(command));
        } catch (Exception e) {
            onError.accept("Error: failed to send message");
        }
    }

    private void handleMessage(String json) {
        try {
            ServerMessage base = gson.fromJson(json, ServerMessage.class);
            if (base == null || base.getServerMessageType() == null) {
                onError.accept("Error: invalid server message");
                return;
            }

            switch (base.getServerMessageType()) {
                case LOAD_GAME -> onLoadGame.accept(gson.fromJson(json, LoadGameMessage.class));
                case NOTIFICATION -> {
                    NotificationMessage m = gson.fromJson(json, NotificationMessage.class);
                    onNotification.accept(m.getMessage());
                }
                case ERROR -> {
                    ErrorMessage m = gson.fromJson(json, ErrorMessage.class);
                    onError.accept(m.getMessage());
                }
            }

        } catch (Exception e) {
            onError.accept("Error: failed to parse server message");
        }
    }

    public void leaveGame(String authToken, int gameId) {
        send(new websocket.commands.LeaveCommand(authToken, gameId));
    }
}