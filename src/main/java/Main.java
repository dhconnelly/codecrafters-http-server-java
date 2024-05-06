import static java.nio.charset.StandardCharsets.US_ASCII;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.regex.Pattern;

public class Main {
    private static final void die(Exception e) {
        e.printStackTrace(System.err);
        System.exit(1);
    }

    private static final Pattern REQUEST_LINE_PATTERN = Pattern.compile("GET (\\S+) HTTP\\/1.1");

    static void handle(Socket client) throws IOException {
        var lines = new BufferedReader(new InputStreamReader(client.getInputStream()));
        var requestLine = lines.readLine();
        System.out.printf("request: [%s]\n", requestLine);
        var matcher = REQUEST_LINE_PATTERN.matcher(requestLine);
        if (!matcher.matches()) {
            client.getOutputStream().write("HTTP/1.1 400 Bad Request\r\n\r\n".getBytes(US_ASCII));
            return;
        }
        var path = matcher.group(1);
        if (path.equals("/")) {
            client.getOutputStream().write("HTTP/1.1 200 OK\r\n\r\n".getBytes(US_ASCII));
            return;
        } else {
            client.getOutputStream().write("HTTP/1.1 404 Not Found\r\n\r\n".getBytes(US_ASCII));
            return;
        }
    }

    static void loop(ServerSocket server) {
        while (true) {
            try (var client = server.accept()) {
                handle(client);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(4221)) {
            serverSocket.setReuseAddress(true);
            loop(serverSocket);
        } catch (Exception e) {
            die(e);
        }
    }
}
