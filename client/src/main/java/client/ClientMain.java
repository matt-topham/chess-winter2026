package client;

import chess.*;
import model.GameData;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class ClientMain {

    private final ServerFacade facade;
    private final Scanner in = new Scanner(System.in);

    private enum State {PRELOGIN, POSTLOGIN}

    private State state = State.PRELOGIN;

    private String authToken = null;

    private List<GameData> lastListedGames = new ArrayList<>();

    public ClientMain(String host, int port) {
        this.facade =  new ServerFacade(host, port);
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
                }
                else {
                    handlePostLogin(line);
                }
            }
            catch (ServerFacade.ClientException e) {
                System.out.println(cleanMessage(e.getMessage()));
            }
            catch (Exception e) {
                System.out.println("An error occurred. Please try again.");
            }
        }
    }

    private boolean handlePreLogin(String line) throws Exception {
        String[] parts = splitCommand(line);
        String command = parts[0].toLowerCase();

        switch (command) {
            case "help" -> preLoginHelp();
            case "quit", "exit" -> {return true;}
            case "register" -> doRegister(parts);
            case "login" -> doLogin(parts);
            default -> System.out.println("Unknown command. Type 'help.");
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

        switch(command) {
            case "help" -> postLoginHelp();
            case "logout" -> doLogout();
            case "create" -> doCreate(line);
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
        int gameId = facade.createGame(authToken, name);
        System.out.println("Created game: " + name);
    }

    private void doList() throws Exception {}

    private void doPlay(String[] parts) throws Exception {}

    private void doObserve(String[] parts) throws Exception {}

    private String prompt() {
        return (state == State.PRELOGIN) ? "[prelogin] >>> " : "[postlogin] >>> ";
    }

    private static String cleanMessage(String msg) {
        // come back and finish this
        return "Error: " + msg;
    }

    private static String[] splitCommand(String line) {
        return line.trim().split("\\s+");
    }
}
