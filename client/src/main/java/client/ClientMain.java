package client;

import chess.*;

import java.util.Scanner;

public class ClientMain {

    private final ServerFacade facade;
    private final Scanner in = new Scanner(System.in);

    private enum State {PRELOGIN, POSTLOGIN}

    private State state = State.PRELOGIN;

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

    }

    private void doLogin(String[] parts) throws Exception {

    }

    private void handlePostLogin(String line) throws Exception {

    }

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
