package com.dawiddc.main;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.URL;

public class ProxyServer {
    private static final int port = 8000;
    private static HttpServer server;

    public static void main(String[] args) throws Exception {
        server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/", new RootHandler());
    }


    private static class RootHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange httpExchange) {
            HttpURLConnection connection = null;
            try {
                connection = setupConnection(httpExchange);
                byte[] requestBytes = readRequestBodyToByteArray(httpExchange.getRequestBody());
                OutputStream os = connection.getOutputStream();
                os.write(requestBytes);
                os.close();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                connection.disconnect();
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
            HttpURLConnection connection = null;
            URL url = httpExchange.getRequestURI().toURL();
            connection = (HttpURLConnection) url.openConnection();
            connection.setInstanceFollowRedirects(false);
            connection.setRequestMethod(httpExchange.getRequestMethod());
            connection.setRequestProperty("Via", server.getAddress().getHostString());
            Headers requestHeaders = httpExchange.getRequestHeaders();
            for (String key : requestHeaders.keySet()) {
                connection.setRequestProperty(key, requestHeaders.get(key).get(0));
            }
            return connection;
        }
    }
}
