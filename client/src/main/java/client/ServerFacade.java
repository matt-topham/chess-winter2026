package client;

import com.google.gson.Gson;

public class ServerFacade {

    private final String baseUrl;
    private final Gson gson = new Gson();

    public ServerFacade(String host, int port) {
        this.baseUrl = "http//:" + host + ":" + port;
    }

}
