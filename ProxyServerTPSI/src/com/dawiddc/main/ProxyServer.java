package com.dciok.main;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.URL;

public class ProxyServer {
    private static final int port = 8000;
    private final HttpServer server;
    private ProxyServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress(port), 0);
    }

    private HttpServer getProxyServer(){ return this.server; }


    public static void main(String[] args) throws Exception {
        ProxyServer proxyServer = new ProxyServer();
        proxyServer.getProxyServer().createContext("/", new RootHandler());
    }

    private static class RootHandler implements HttpHandler {

        @Override
        public void handle(HttpExchange httpExchange) {
            HttpURLConnection connection = null;
            try {
//            httpExchange.getRequestBody();
                Headers requestHeaders = httpExchange.getRequestHeaders();
                URL url = httpExchange.getRequestURI().toURL();
                connection = (HttpURLConnection) url.openConnection();
                connection.setInstanceFollowRedirects(false);
                connection.setRequestMethod(httpExchange.getRequestMethod());
                connection.setRequestProperty("Via", server.getAddress());
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                connection.disconnect();
            }

        }
    }
}
