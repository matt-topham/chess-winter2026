package client;

import chess.*;

public class ClientMain {

    private final ServerFacade facade;

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
    }
}
