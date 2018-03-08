import com.cedarsoftware.util.io.JsonWriter;
import com.sun.net.httpserver.*;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

public class TPSIServer {
    public static void main(String[] args) throws Exception {
        int port = 8000;
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/", new RootHandler());
        server.createContext("/echo/", new EchoHandler());
        server.createContext("/redirect/", new RedirectHandler());
        server.createContext("/cookies/", new CookiesHandler());
        server.createContext("/auth/", new AuthHandler());
        server.createContext("/auth2/", new Auth2Handler());
        System.out.println("Starting server on port: " + port);
        server.start();
    }

    static class RootHandler implements HttpHandler {
        public void handle(HttpExchange exchange) throws IOException {
            byte[] response = Files.readAllBytes(Paths.get("index.html"));
            exchange.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
            exchange.sendResponseHeaders(200, response.length);
            OutputStream os = exchange.getResponseBody();
            os.write(response);
            os.close();
        }
    }

    static class EchoHandler implements HttpHandler {
        public void handle(HttpExchange exchange) throws IOException {
            Headers headers = exchange.getRequestHeaders();
            String json = JsonWriter.objectToJson(headers);
            String.format(json = JsonWriter.formatJson(json));

            exchange.getResponseHeaders().set("Content-Type", "application/json;");
            exchange.sendResponseHeaders(200, json.length());
            OutputStream os = exchange.getResponseBody();
            os.write(json.getBytes());
            os.close();
        }
    }

    static class RedirectHandler implements HttpHandler {
        public void handle(HttpExchange exchange) throws IOException {
            exchange.getResponseHeaders().set("Location", "http://www.google.com");
            exchange.sendResponseHeaders(301, 0);
            OutputStream os = exchange.getResponseBody();
            os.write(null);
            os.close();
        }
    }

    static class CookiesHandler implements HttpHandler {
        public void handle(HttpExchange exchange) throws IOException {
            /* Generate random cookie */
            UUID idOne = UUID.randomUUID();
            String id = String.valueOf(idOne);

            byte[] response = Files.readAllBytes(Paths.get("index.html"));
            exchange.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
            exchange.getResponseHeaders().set("Set-Cookie", "ID=" + id + "; Path=/; Domain=localhost");
            exchange.sendResponseHeaders(200, response.length);
            OutputStream os = exchange.getResponseBody();
            os.write(response);
            os.close();
        }
    }

    static class AuthHandler implements HttpHandler {
        public void handle(HttpExchange exchange) throws IOException {
            String harcodedUser = "dawid";
            String hardcodedPass = "password";

            byte[] response = Files.readAllBytes(Paths.get("index.html"));
            List<String> authHeader = null;
            if (exchange.getRequestHeaders().containsKey("Authorization")) {

                authHeader = exchange.getRequestHeaders().get("Authorization");
                byte[] decodedCredentials = Base64.getDecoder().decode(authHeader.get(1));
                String[] stringCredentials = decodedCredentials.toString().split("\\:");
                String requestUser = stringCredentials[0];
                String requestPass = stringCredentials[1];

                if (requestUser == harcodedUser && requestPass == hardcodedPass) {
                    exchange.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
                    exchange.sendResponseHeaders(200, response.length);
                    OutputStream os = exchange.getResponseBody();
                    os.write(response);
                    os.close();
                    return;
                } else {
                    exchange.sendResponseHeaders(401, -1);
                }
            } else {
                exchange.sendResponseHeaders(401, -1);
            }
            OutputStream os = exchange.getResponseBody();
            os.write(null);
            os.close();
        }
    }

    static class Auth2Handler implements HttpHandler {
        public void handle(HttpExchange exchange) throws IOException {
            byte[] response = Files.readAllBytes(Paths.get("index.html"));
            exchange.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
            exchange.sendResponseHeaders(200, response.length);
            OutputStream os = exchange.getResponseBody();
            os.write(response);
        }
    }

}