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

class TPSIServer {
    public static void main(String[] args) throws Exception {
        int port = 8000;
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/", new RootHandler());
        server.createContext("/echo/", new EchoHandler());
        server.createContext("/redirect/", new RedirectHandler());
        server.createContext("/cookies/", new CookiesHandler());
        server.createContext("/auth/", new AuthHandler());
        HttpContext authContext = server.createContext("/auth2/", new Auth2Handler());
        authContext.setAuthenticator(new BasicAuthenticator("get") {
            @Override
            public boolean checkCredentials(String username, String password) {
                return username.equals("dawid") && password.equals("password");
            }
        });
        System.out.println("Starting server on port: " + port);
        server.start();
    }

    static class RootHandler implements HttpHandler {
        public void handle(HttpExchange exchange) throws IOException {
            standardResponse(exchange);
        }

        private static void standardResponse(HttpExchange exchange) throws IOException {
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
            json = JsonWriter.formatJson(json);

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
            System.out.println("Redirecting...");
            exchange.sendResponseHeaders(301, 0);
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
            String hardcodedUser = "dawid", hardcodedPass = "password";

            List<String> authHeader;
            try {
            if (exchange.getRequestHeaders().containsKey("Authorization")) {
                authHeader = exchange.getRequestHeaders().get("Authorization");
                byte[] decodedCredentials = Base64.getDecoder().decode(authHeader.get(0).split(" ")[1].getBytes());
                String[] stringCredentials = new String(decodedCredentials).split(":");
                String requestUser = stringCredentials[0];
                String requestPass = stringCredentials[1];

                if (requestUser.equals(hardcodedUser) && requestPass.equals(hardcodedPass)) {
                    RootHandler.standardResponse(exchange);
                } else {
                    sendUnauthorizedResponse(exchange);
                }
            } else {
                sendUnauthorizedResponse(exchange);
            }
        } catch(Exception e) {
                sendUnauthorizedResponse(exchange);
        }}

        private void sendUnauthorizedResponse(HttpExchange exchange) throws IOException {
            exchange.getResponseHeaders().set("WWW-Authenticate", "Basic");
            String notAuthorized = "You are not authorized to access this site.";
            exchange.sendResponseHeaders(401, notAuthorized.getBytes().length);
            OutputStream os = exchange.getResponseBody();
            os.write(notAuthorized.getBytes());
            os.close();
        }
    }

    static class Auth2Handler implements HttpHandler {
        public void handle(HttpExchange exchange) throws IOException {
            standardResponse(exchange);
        }

        private static void standardResponse(HttpExchange exchange) throws IOException {
            RootHandler.standardResponse(exchange);
        }
    }

}