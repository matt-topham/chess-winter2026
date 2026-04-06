package websocket.commands;

public class ConnectCommand extends UserGameCommand {

    private final String playerColor;

    public ConnectCommand(String authToken, int gameID, String playerColor) {
        super(CommandType.CONNECT, authToken, gameID);
        this.playerColor = playerColor;
    }

    public String getPlayerColor() {
        return playerColor;
    }
}
