package dev.dhc.http;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.Executors;

public class Server {

    private final int port;
    private final Handler handler;

    public Server(int port, Handler handler) {
        this.port = port;
        this.handler = handler;
    }

    private void fulfill(OutputStream out, InputStream in) throws IOException {
        Response resp;
        try {
            resp = handler.handle(Request.parseFrom(in));
        } catch (BadRequestException e) {
            resp = new Response(StatusCode.BadRequest);
        }
        resp.write(out);
    }

    private void serve(Socket client) {
        final var addr = client.getRemoteSocketAddress();
        try (client;
            var r = new BufferedInputStream(client.getInputStream());
            var w = new BufferedOutputStream(client.getOutputStream())
         ) {
            fulfill(w, r);
        } catch (IOException e) {
            System.err.printf("error handling client %s:\n", addr);
            e.printStackTrace(System.err);
        }
    }

    public void run() throws IOException {
        try (ServerSocket server = new ServerSocket(port)) {
            server.setReuseAddress(true);
            var executor = Executors.newVirtualThreadPerTaskExecutor();
            while (true) {
                var client = server.accept();
                executor.submit(() -> serve(client));
            }
        }
    }

}
