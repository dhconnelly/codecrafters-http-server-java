import static java.nio.charset.StandardCharsets.US_ASCII;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
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

    private void sendStatus(OutputStream w, StatusCode status)
            throws IOException {
        System.out.printf("sending status %s\n", status);
        w.write("HTTP/1.1 %d %s\r\n".formatted(status.code(), status.message())
                .getBytes(US_ASCII));
        System.out.printf("done sending status\n");
    }

    private void appendHeader(OutputStream w, String key, Object value)
            throws IOException {
        w.write("%s: %s\r\n".formatted(key, value).getBytes(US_ASCII));
    }

    private void finishHeaders(OutputStream w) throws IOException {
        w.write("\r\n".getBytes(US_ASCII));
    }

    private void sendError(OutputStream w, HttpException he)
            throws IOException {
        switch (he) {
            case NotFoundException e -> sendStatus(w, StatusCode.NotFound);
            case BadRequestException e -> sendStatus(w, StatusCode.BadRequest);
            case InternalServerErrorException e -> sendStatus(w,
                    StatusCode.InternalServerError);
        }
        finishHeaders(w);
        w.flush();
    }

    private void sendResponse(OutputStream w, Response resp)
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

    private void fulfill(OutputStream out, InputStream in) throws IOException {
        try {
            var req = Request.parseFrom(in);
            var resp = router.handle(req);
            sendResponse(out, resp);
        } catch (HttpException he) {
            System.err.printf("error while handling! %s\n", he);
            sendError(out, he);
        }
    }

    private void serve(Socket client) {
        final var addr = client.getRemoteSocketAddress();
        System.out.printf("handling client %s\n", addr);
        try (client;
                var w = new BufferedOutputStream(client.getOutputStream());
                var r = new BufferedInputStream(client.getInputStream())) {
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
