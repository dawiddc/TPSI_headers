import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;

class TPSIServer_B {

    private static final int port = 8000;
    private static String inputPath = null;
    private static HttpExchange globalExchange;

    public static void main(String[] args) throws Exception {
        if (args.length > 0) {
            inputPath = args[0];
            if (!inputPath.isEmpty()) {
                HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
                server.createContext("/", new RootHandler() {
                    @Override
                    public void handle(final HttpExchange exchange) {
                        globalExchange = exchange;
                        URI requestURI = exchange.getRequestURI();
                        manageUserRequest(inputPath, requestURI);
                    }
                });
                server.start();
                System.out.println("Starting server on port: " + port);
            }
        } else {
            System.out.println("Please provide file path");
        }
    }

    private static void manageUserRequest(String inputPath, URI requestUri) {
        try {
            String response = "";
            String stringUri = requestUri.toString();
            /* For PTA test purposes */
//            stringUri = "/../";

            File requestedDirectory = new File(inputPath + requestUri.getPath());
            requestedDirectory = requestedDirectory.getCanonicalFile();
            if (!requestedDirectory.toString().startsWith(inputPath)) {
                response = "Access denied";
                System.out.println("Prevented path traversal attack");
                globalExchange.sendResponseHeaders(403, response.getBytes().length);
            } else if (!requestedDirectory.exists()) {
                globalExchange.sendResponseHeaders(404, 0);
            } else {
                if (requestedDirectory.isFile()) {
                    byte[] byteResponse = prepareFile(requestedDirectory);
                    globalExchange.sendResponseHeaders(200, response.getBytes().length);
                    OutputStream os = globalExchange.getResponseBody();
                    os.write(byteResponse);
                    os.close();
                    return;
                } else {
                    response = buildFileList(requestedDirectory, stringUri);
                    globalExchange.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
                }
                globalExchange.sendResponseHeaders(200, response.getBytes().length);
            }
            OutputStream os = globalExchange.getResponseBody();
            os.write(response.getBytes());
            os.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static String buildFileList(File requestedDirectory, String stringUri) {
        StringBuilder responseStringBuilder = new StringBuilder();
        responseStringBuilder.append("<head></head><body><ul>");
        File[] files = requestedDirectory.listFiles();
        if (stringUri.endsWith("/"))
            stringUri = stringUri.substring(0, stringUri.length() - 1);
        if (files == null || files.length == 0) {
            return "<head></head><body><p>This directory is empty!</p><h><a href=\"http://localhost:8000" + stringUri.concat("/../") + "\">Go back!</a></h></body>";
        }
        for (File file : files) {
            if (file.isFile()) {
                responseStringBuilder.append("<li>File: <a href=\"http://localhost:8000").append(stringUri).append("/").append(file.getName()).append("\">").append(file.getName()).append("</a></li>\n");
            } else if (file.isDirectory()) {
                responseStringBuilder.append("<li>Directory: <a href=\"http://localhost:8000").append(stringUri).append("/").append(file.getName()).append("\">").append(file.getName()).append("</a></li>\n");
            }
        }
        if (requestedDirectory.getParent().startsWith(inputPath))
            responseStringBuilder.append("<li><a href=\"http://localhost:8000").append(stringUri.concat("/../")).append("\">Go up!</a></li>");
        responseStringBuilder.append("</ul></body>");
        return responseStringBuilder.toString();
    }

    private static byte[] prepareFile(File requestedDirectory) {
        File requestedFile = new File(requestedDirectory.toString());
        byte[] requestedFileBytes = new byte[(int) requestedFile.length()];
        try {
            FileInputStream fis = new FileInputStream(requestedFile);
            BufferedInputStream bis = new BufferedInputStream(fis);
            bis.read(requestedFileBytes, 0, requestedFileBytes.length);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return requestedFileBytes;
    }

    static class RootHandler implements HttpHandler {
        public void handle(HttpExchange exchange) {
        }
    }
}
