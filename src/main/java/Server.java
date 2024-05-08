import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executors;

public class Server {
    private final int port;
    private final Router router;

    public Server(int port, List<Handler> handlers) {
        this.port = port;
        this.router = new Router(handlers);
    }

    private void fulfill(OutputStream out, InputStream in) throws IOException {
        Response resp;
        try {
            var req = Request.parseFrom(in);
            resp = router.handle(req);
        } catch (BadRequestException e) {
            resp = new Response(StatusCode.BadRequest, Optional.empty());
        }
        resp.write(out);
    }

    private void serve(Socket client) {
        final var addr = client.getRemoteSocketAddress();
        try (client;
                var w = new BufferedOutputStream(client.getOutputStream());
                var r = new BufferedInputStream(client.getInputStream())) {
            fulfill(w, r);
        } catch (IOException e) {
            System.err.printf("error handling client %s:\n", addr);
            e.printStackTrace(System.err);
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
