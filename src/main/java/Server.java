import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.Executors;

public class Server {
    private final int port;
    private final Router router;

    public Server(int port, List<Handler> handlers) {
        this.port = port;
        this.router = new Router(handlers);
    }

    private void sendStatus(BufferedWriter w, StatusCode status)
            throws IOException {
        System.out.printf("sending status %s\n", status);
        w.write("HTTP/1.1 %d %s\r\n".formatted(status.code(),
                status.message()));
        System.out.printf("done sending status\n");
    }

    private void appendHeader(BufferedWriter w, String key, Object value)
            throws IOException {
        w.write("%s: %s\r\n".formatted(key, value));
    }

    private void finishHeaders(BufferedWriter w) throws IOException {
        w.write("\r\n");
    }

    private void sendError(BufferedWriter w, HttpException he)
            throws IOException {
        switch (he) {
            case NotFoundException e -> sendStatus(w, StatusCode.NotFound);
            case BadRequestException e -> sendStatus(w, StatusCode.BadRequest);
        }
        finishHeaders(w);
        w.flush();
    }

    private void sendResponse(BufferedWriter w, Response resp)
            throws IOException {
        System.out.printf("handling done, sending response %s\n", resp);
        sendStatus(w, resp.status());
        if (resp.body().isPresent()) {
            Body body = resp.body().get();
            appendHeader(w, "Content-Type", body.contentType());
            appendHeader(w, "Content-Length", body.contentLength());
            finishHeaders(w);
            body.write(w);
        } else {
            finishHeaders(w);
        }
        w.flush();
    }

    private void fulfill(BufferedWriter w, BufferedReader r)
            throws IOException {
        try {
            var req = Request.parseFrom(r);
            var resp = router.handle(req);
            sendResponse(w, resp);
        } catch (HttpException he) {
            System.err.printf("error while handling! %s\n", he);
            sendError(w, he);
        }
    }

    private void serve(Socket client) {
        final var addr = client.getRemoteSocketAddress();
        System.out.printf("handling client %s\n", addr);
        try (client;
                var w = new BufferedWriter(
                        new OutputStreamWriter(client.getOutputStream()));
                var r = new BufferedReader(
                        new InputStreamReader(client.getInputStream()))) {
            System.out.printf("streams established for client %s\n", addr);
            fulfill(w, r);
            System.out.println("http handling done!");
        } catch (IOException e) {
            System.err.printf("error handling client %s:\n", addr);
            e.printStackTrace(System.err);
        } finally {
            System.err.printf("finished client %s\n", addr);
        }
    }

    void run() throws IOException {
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
