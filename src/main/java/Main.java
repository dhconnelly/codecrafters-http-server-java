import static java.nio.charset.StandardCharsets.US_ASCII;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class Main {
    private static final void die(Exception e) {
        e.printStackTrace(System.err);
        System.exit(1);
    }

    static void handle(Socket client) throws IOException {
        try (client) {
            client.getOutputStream().write("HTTP/1.1 200 OK\r\n\r\n".getBytes(US_ASCII));
        }
    }

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(4221)) {
            serverSocket.setReuseAddress(true);
            handle(serverSocket.accept());
        } catch (Exception e) {
            die(e);
        }
    }
}
