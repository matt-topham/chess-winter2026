package websocket.commands;

public class ResignCommand extends UserGameCommand {
    public ResignCommand(String authToken, int gameID) {
        super(CommandType.RESIGN, authToken, gameID);
    }
}
