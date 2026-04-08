package client;

import chess.ChessGame;
import model.GameData;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class ClientMain {

    private final String host;
    private final int port;

    private final ServerFacade facade;
    private WebSocketFacade ws = null;

    private final Scanner in = new Scanner(System.in);

    private enum State { PRELOGIN, POSTLOGIN }
    private State state = State.PRELOGIN;

    private String authToken = null;
    private List<GameData> lastListedGames = new ArrayList<>();

    public ClientMain(String host, int port) {
        this.host = host;
        this.port = port;
        this.facade = new ServerFacade(host, port);
    }

    public static void main(String[] args) {
        String host = "localhost";
        int port = 8080;

        if (args.length >= 1) host = args[0];
        if (args.length >= 2) port = Integer.parseInt(args[1]);

        new ClientMain(host, port).run();
    }

    private void run() {
        System.out.println("♕ 240 Chess Client");
        preLoginHelp();

        while (true) {
            System.out.print(prompt());
            String line = in.nextLine().trim();
            if (line.isEmpty()) continue;

            try {
                if (state == State.PRELOGIN) {
                    if (handlePreLogin(line)) return;
                } else {
                    handlePostLogin(line);
                }
            } catch (ServerFacade.ClientException e) {
                System.out.println(cleanMessage(e.getMessage()));
            } catch (Exception e) {
                System.out.println("An error occurred. Please try again.");
            }
        }
    }

    private boolean handlePreLogin(String line) throws Exception {
        String[] parts = splitCommand(line);
        String command = parts[0].toLowerCase();

        switch (command) {
            case "help" -> preLoginHelp();
            case "quit", "exit" -> { return true; }
            case "register" -> doRegister(parts);
            case "login" -> doLogin(parts);
            default -> System.out.println("Unknown command. Type 'help'.");
        }
        return false;
    }

    private void preLoginHelp() {
        System.out.println("""
                Prelogin commands:
                   help
                   register <username> <password> <email>
                   login <username> <password>
                   quit
                """);
    }

    private void doRegister(String[] parts) throws Exception {
        if (parts.length != 4) {
            System.out.println("Usage: register <username> <password> <email>");
            return;
        }
        var auth = facade.register(parts[1], parts[2], parts[3]);
        this.authToken = auth.authToken();
        this.state = State.POSTLOGIN;
        System.out.println("Registered and logged in as " + auth.username() + ".");
        postLoginHelp();
    }

    private void doLogin(String[] parts) throws Exception {
        if (parts.length != 3) {
            System.out.println("Usage: login <username> <password>");
            return;
        }
        var auth = facade.login(parts[1], parts[2]);
        this.authToken = auth.authToken();
        this.state = State.POSTLOGIN;
        System.out.println("Logged in as " + auth.username() + ".");
        postLoginHelp();
    }

    private void handlePostLogin(String line) throws Exception {
        String[] parts = splitCommand(line);
        String command = parts[0].toLowerCase();

        switch (command) {
            case "help" -> postLoginHelp();
            case "logout" -> doLogout();
            case "create" -> doCreate(line); // allows spaces in name
            case "list" -> doList();
            case "play" -> doPlay(parts);
            case "observe" -> doObserve(parts);
            default -> System.out.println("Unknown command. Type 'help'.");
        }
    }

    private void postLoginHelp() {
        System.out.println("""
                Postlogin commands:
                    help
                    list
                    create <game name>
                    play <game #> <white|black>
                    observe <game #>
                    logout
                """);
    }

    private void doLogout() throws Exception {
        if (ws != null) {
            ws.close();
            ws = null;
        }

        facade.logout(authToken);
        authToken = null;
        lastListedGames = new ArrayList<>();
        state = State.PRELOGIN;

        System.out.println("Logged out.");
        preLoginHelp();
    }

    private void doCreate(String fullLine) throws Exception {
        String name = fullLine.substring("create".length()).trim();
        if (name.isEmpty()) {
            System.out.println("Usage: create <game name>");
            return;
        }
        facade.createGame(authToken, name);
        System.out.println("Created game: " + name);
    }

    private void doList() throws Exception {
        GameData[] games = facade.listGames(authToken);
        lastListedGames = new ArrayList<>(List.of(games));

        if (lastListedGames.isEmpty()) {
            System.out.println("No games found.");
            return;
        }

        System.out.println("Games:");
        for (int i = 0; i < lastListedGames.size(); i++) {
            GameData game = lastListedGames.get(i);
            String white = (game.whiteUsername() == null) ? "-" : game.whiteUsername();
            String black = (game.blackUsername() == null) ? "-" : game.blackUsername();
            System.out.printf(" %d) %s  [white: %s, black: %s]%n", i + 1, game.gameName(), white, black);
        }
    }

    private void doPlay(String[] parts) throws Exception {
        if (parts.length != 3) {
            System.out.println("Usage: play <game #> <white|black>");
            return;
        }

        Integer index = parseGameNumber(parts[1]);
        if (index == null) return;

        String colorInput = parts[2].toLowerCase();
        String color;
        ChessGame.TeamColor perspective;

        if (colorInput.equals("white")) {
            color = "WHITE";
            perspective = ChessGame.TeamColor.WHITE;
        } else if (colorInput.equals("black")) {
            color = "BLACK";
            perspective = ChessGame.TeamColor.BLACK;
        } else {
            System.out.println("Color must be 'white' or 'black'.");
            return;
        }

        GameData game = lastListedGames.get(index);

        facade.joinGame(authToken, game.gameID(), color);

        if (ws != null) {
            ws.close();
            ws = null;
        }

        ws = new WebSocketFacade(
                host, port,
                load -> {
                    System.out.print(ui.EscapeSequences.ERASE_SCREEN);
                    BoardPrinter.printBoard(load.getGame().getBoard(), perspective);
                },
                System.out::println,
                System.out::println
        );

        ws.connectGame(authToken, game.gameID(), color);
    }

    private void doObserve(String[] parts) throws Exception {
        if (parts.length != 2) {
            System.out.println("Usage: observe <game #>");
            return;
        }

        Integer index = parseGameNumber(parts[1]);
        if (index == null) return;

        GameData game = lastListedGames.get(index);

        if (ws != null) {
            ws.close();
            ws = null;
        }

        ws = new WebSocketFacade(
                host, port,
                load -> {
                    System.out.print(ui.EscapeSequences.ERASE_SCREEN);
                    BoardPrinter.printBoard(load.getGame().getBoard(), ChessGame.TeamColor.WHITE);
                },
                System.out::println,
                System.out::println
        );

        ws.connectGame(authToken, game.gameID(), "OBSERVER");
    }

    private Integer parseGameNumber(String s) {
        if (lastListedGames.isEmpty()) {
            System.out.println("No games listed yet. Run 'list' first.");
            return null;
        }
        try {
            int n = Integer.parseInt(s);
            if (n < 1 || n > lastListedGames.size()) {
                System.out.println("Game # must be between 1 and " + lastListedGames.size() + ".");
                return null;
            }
            return n - 1;
        } catch (NumberFormatException e) {
            System.out.println("Game # must be a number.");
            return null;
        }
    }

    private String prompt() {
        return (state == State.PRELOGIN) ? "[prelogin] >>> " : "[postlogin] >>> ";
    }

    private static String cleanMessage(String msg) {
        if (msg == null || msg.isBlank()) {
            return "An error occurred.";
        }
        if (msg.toLowerCase().contains("error")) {
            return msg;
        }
        return "Error: " + msg;
    }

    private static String[] splitCommand(String line) {
        return line.trim().split("\\s+");
    }
}