import static java.nio.charset.StandardCharsets.US_ASCII;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Main {
    private static final void die(Exception e) {
        e.printStackTrace(System.err);
        System.exit(1);
    }

    private static final Pattern REQUEST_LINE_PATTERN = Pattern.compile("GET (\\S+) HTTP\\/1.1");
    private static final Pattern ECHO_PATTERN = Pattern.compile("\\/echo\\/([^\\/]+)");

    static void handle(Socket client) throws IOException {
        var lines = new BufferedReader(new InputStreamReader(client.getInputStream()));
        var requestLine = lines.readLine();
        System.out.printf("request: [%s]\n", requestLine);

        // TODO: parsing
        var headers = new HashMap<String, String>();
        for (String line; (line = lines.readLine()) != null && !line.isEmpty();) {
            var toks = line.split(": ");
            headers.put(toks[0], toks[1]);
        }

        // TODO: routing
        var matcher = REQUEST_LINE_PATTERN.matcher(requestLine);
        if (!matcher.matches()) {
            client.getOutputStream().write("HTTP/1.1 400 Bad Request\r\n\r\n".getBytes(US_ASCII));
            return;
        }
        var path = matcher.group(1);
        Matcher m;
        if (path.equals("/")) {
            client.getOutputStream().write("HTTP/1.1 200 OK\r\n\r\n".getBytes(US_ASCII));
        } else if (path.equals("/user-agent")) {
            String string = headers.get("User-Agent");
            client.getOutputStream()
                    .write("HTTP/1.1 200 OK\r\nContent-Type: text/plain\r\nContent-Length: %d\r\n\r\n%s"
                            .formatted(string.length(), string)
                            .getBytes(US_ASCII));
        } else if ((m = ECHO_PATTERN.matcher(path)).matches()) {
            String string = m.group(1);
            client.getOutputStream()
                    .write("HTTP/1.1 200 OK\r\nContent-Type: text/plain\r\nContent-Length: %d\r\n\r\n%s"
                            .formatted(string.length(), string)
                            .getBytes(US_ASCII));
        } else {
            client.getOutputStream().write("HTTP/1.1 404 Not Found\r\n\r\n".getBytes(US_ASCII));
        }
    }

    static Runnable handler(Socket client) {
        // TODO: error handling
        return () -> {
            try (client) {
                handle(client);
            } catch (Exception e) {
                e.printStackTrace();
            }
        };
    }

    static void loop(ServerSocket server) throws IOException {
        var executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        System.out.println("initialized fixed thread pool executor");
        while (true) {
            var client = server.accept();
            System.out.printf("accepted client: %s\n", client.getRemoteSocketAddress());
            System.out.println("sending to queue");
            executor.submit(handler(client));
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
