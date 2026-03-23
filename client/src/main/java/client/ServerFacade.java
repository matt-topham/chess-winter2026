package client;

import com.google.gson.Gson;

import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;

public class ServerFacade {

    private final String baseUrl;
    private final Gson gson = new Gson();

    public ServerFacade(String host, int port) {
        this.baseUrl = "http://" + host + ":" + port;
    }

    public void clear() throws ClientException {
        request("DELETE", "/db", null, null, Object.class);
    }


    private <T> T request(String method, String path, String authToken, Object body, Class<T> responseClass)
            throws ClientException {
        try {
            URI uri = URI.create(baseUrl + path);
            HttpURLConnection conn = (HttpURLConnection) uri.toURL().openConnection();
            conn.setRequestMethod(method);
            conn.setDoInput(true);

            conn.setRequestProperty("Accept", "application/json");

            if (authToken != null && !authToken.isBlank()) {
                conn.setRequestProperty("authorization", authToken);
            }

            if (body != null) {
                conn.setDoOutput(true);
                conn.setRequestProperty("Content-Type", "application/json");
                String json = gson.toJson(body);
                try (OutputStream os = conn.getOutputStream()) {
                    os.write(json.getBytes(StandardCharsets.UTF_8));
                }
            }

            int status = conn.getResponseCode();

            if (status == 200) {
                if (responseClass == Object.class) {
                    return null; // endpoints that return {}
                }
                try (var reader = new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8)) {
                    return gson.fromJson(reader, responseClass);
                }
            } else {
                String message = "Error: request failed";
                var errStream = conn.getErrorStream();
                if (errStream != null) {
                    try (var reader = new InputStreamReader(errStream, StandardCharsets.UTF_8)) {
                        ErrorResponse err = gson.fromJson(reader, ErrorResponse.class);
                        if (err != null && err.message != null && !err.message.isBlank()) {
                            message = err.message;
                        }
                    }
                }
                throw new ClientException(message, status);
            }

        } catch (ClientException e) {
            throw e;
        } catch (Exception e) {
            throw new ClientException("Error: " + e.getMessage(), 0);
        }
    }

    private static class ErrorResponse {
        String message;
    }

    public static class ClientException extends Exception {
        private final int status;

        public ClientException(String message, int status) {
            super(message);
            this.status = status;
        }

        public int status() {
            return status;
        }
    }
}