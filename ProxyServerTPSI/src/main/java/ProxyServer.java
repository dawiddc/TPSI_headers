import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Objects;

class ProxyServer {
    private static final int port = 8000;

    public static void main(String[] args) throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/", new RootHandler());
        System.out.println("Starting server on port:" + port);
        server.start();
    }

    private static class RootHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange httpExchange) {
            HttpURLConnection connection = null;
            if (validateAddressAgainstBlackList(httpExchange))
                try {
                    Logger.logValues(1, 0, 0);
                    connection = setupConnection(httpExchange);
                    byte[] requestBytes = readRequestBodyToByteArray(httpExchange.getRequestBody());
                    /* write request body if not GET */
                    if (!httpExchange.getRequestMethod().equals("GET")) {
                        connection.setDoOutput(true);
                        OutputStream os = connection.getOutputStream();
                        os.write(requestBytes);
                        Logger.logValues(0, requestBytes.length, 0);
                        os.close();
                    }
                    passServerResponse(httpExchange, connection);
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    Objects.requireNonNull(connection).disconnect();
                }
        }

        private byte[] readRequestBodyToByteArray(InputStream requestBody) throws Exception {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            int nRead;
            byte[] data = new byte[16384];
            while ((nRead = requestBody.read(data, 0, data.length)) != -1) {
                buffer.write(data, 0, nRead);
            }
            buffer.flush();
            return buffer.toByteArray();
        }

        private HttpURLConnection setupConnection(HttpExchange httpExchange) throws Exception {
            URL url = httpExchange.getRequestURI().toURL();
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setInstanceFollowRedirects(false);
            connection.setRequestMethod(httpExchange.getRequestMethod());
            connection.setRequestProperty("Via", httpExchange.getLocalAddress().toString());
            connection.setRequestProperty("X-Forwarded-For", httpExchange.getRemoteAddress().toString());
            Headers requestHeaders = httpExchange.getRequestHeaders();
            for (Map.Entry<String, List<String>> entry : requestHeaders.entrySet()) {
                String headerKey = entry.getKey();
                List<String> headerValues = entry.getValue();
                for (String value : headerValues) {
                    if (headerKey != null)
                        connection.setRequestProperty(headerKey, value);
                }
            }
            return connection;
        }

        private byte[] readAllBytes(InputStream is) {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();

            try {
                int nRead;
                byte[] data = new byte[16384];

                while ((nRead = is.read(data, 0, data.length)) != -1) {
                    buffer.write(data, 0, nRead);
                }

                buffer.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }

            return buffer.toByteArray();
        }

        private void passServerResponse(HttpExchange httpExchange, HttpURLConnection connection) {
            InputStream is;
            byte[] response = null;
            try {
                if (connection.getResponseCode() < 400) {
                    is = connection.getInputStream();
                } else {
                    is = connection.getErrorStream();
                }
                if (is.available() > 0)
                    response = readAllBytes(is);
                /* Pass headers */
                Map<String, List<String>> serverHeaders = connection.getHeaderFields();
                for (Map.Entry<String, List<String>> entry : serverHeaders.entrySet()) {
                    if (entry.getKey() != null && !entry.getKey().equalsIgnoreCase("Transfer-Encoding"))
                        httpExchange.getResponseHeaders().set(entry.getKey(), entry.getValue().get(0));
                }
                httpExchange.getResponseHeaders().set("Via", httpExchange.getLocalAddress().toString());
                long responseLength = (response != null) ? response.length : -1;
                httpExchange.sendResponseHeaders(connection.getResponseCode(), responseLength);
                if (responseLength != -1) {
                    /* write server response to client */
                    OutputStream clientOs = httpExchange.getResponseBody();
                    Logger.logValues(0, 0, response.length);
                    clientOs.write(response);
                    clientOs.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        private boolean validateAddressAgainstBlackList(HttpExchange httpExchange) {
            List<String> blackList = Blacklist.getBlacklist();
            for (String address : blackList) {
                if (httpExchange.getRequestURI().toString().startsWith(address)) {
                    try {
                        String blacklisted = "This site is blacklisted!";
                        httpExchange.sendResponseHeaders(403, blacklisted.length());
                        OutputStream clientOs = httpExchange.getResponseBody();
                        clientOs.write(blacklisted.getBytes());
                        clientOs.close();

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    return false;
                }
            }
            return true;
        }
    }
}
