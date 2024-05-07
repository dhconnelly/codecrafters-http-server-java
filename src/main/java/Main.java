import static java.util.stream.Collectors.toMap;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.function.Function;
import java.util.regex.Pattern;

public class Main {
    static final void die(Exception e) {
        e.printStackTrace(System.err);
        System.exit(1);
    }

    sealed abstract static class HttpException extends Exception
            permits BadRequestException, NotFoundException {
        HttpException(String message) {
            super(message);
        }
    }

    final static class BadRequestException extends HttpException {
        BadRequestException(String message) {
            super(message);
        }
    }

    final static class NotFoundException extends HttpException {
        NotFoundException() {
            super("no route found for request");
        }
    }

    enum StatusCode {
        BadRequest, NotFound, OK;

        int code() {
            return switch (this) {
                case BadRequest -> 400;
                case NotFound -> 404;
                case OK -> 200;
            };
        }

        String message() {
            return switch (this) {
                case BadRequest -> "Bad Request";
                case NotFound -> "Not Found";
                case OK -> "OK";
            };
        }
    }

    static final Pattern REQUEST_LINE_PAT =
            Pattern.compile("GET (\\S+) HTTP\\/1.1");

    sealed interface Body permits ReaderBody, StringBody {
        String contentType();

        long contentLength();

        void write(BufferedWriter w) throws IOException;
    }

    static record ReaderBody(BufferedReader r, String contentType,
            long contentLength) implements Body {
        public void write(BufferedWriter w) throws IOException {
            r.transferTo(w);
        }
    }

    static record StringBody(String body, String contentType) implements Body {
        public long contentLength() {
            return body.length();
        }

        public void write(BufferedWriter w) throws IOException {
            w.write(body);
        }
    }

    static record Response(StatusCode status, Optional<Body> body) {
    }

    @FunctionalInterface
    interface HttpHandlerFunction {
        Response handle(Request req, Map<String, String> params)
                throws HttpException;
    }

    static class ResponseHandler {
        private final Pattern p;
        private final List<String> params;
        private final HttpHandlerFunction f;

        ResponseHandler(Pattern p, List<String> params, HttpHandlerFunction f) {
            this.p = p;
            this.params = params;
            this.f = f;
        }

        public Optional<Map<String, String>> match(String path) {
            var m = p.matcher(path);
            if (!m.matches()) {
                return Optional.empty();
            }
            return Optional.of(params.stream()
                    .collect(toMap(Function.identity(), m::group)));
        }

        public Response handle(Request req, Map<String, String> params)
                throws HttpException {
            return f.handle(req, params);
        }
    }

    static Map.Entry<String, String> parseHeader(String line)
            throws BadRequestException {
        try {
            var toks = line.split(": ");
            return Map.entry(toks[0], toks[1]);
        } catch (Exception e) {
            throw new BadRequestException("invalid header: " + line);
        }
    }

    static record Request(String path, Map<String, String> headers,
            BufferedReader body) {
        static Request parseFrom(BufferedReader r)
                throws IOException, BadRequestException {
            var requestLine = r.readLine();
            var matcher = REQUEST_LINE_PAT.matcher(requestLine);
            if (!matcher.matches()) {
                throw new BadRequestException("invalid request line");
            }
            var path = matcher.group(1);
            var headers = new HashMap<String, String>();
            for (String s; (s = r.readLine()) != null && !s.isEmpty();) {
                var kv = parseHeader(s);
                headers.put(kv.getKey(), kv.getValue());
            }
            return new Request(path, headers, r);
        }
    }

    static void sendStatus(BufferedWriter w, StatusCode status)
            throws IOException {
        System.out.printf("sending status %s\n", status);
        w.write("HTTP/1.1 %d %s\r\n".formatted(status.code(),
                status.message()));
        System.out.printf("done sending status\n");
    }

    static record HandlerMatch(ResponseHandler handler,
            Map<String, String> params) {
    }

    static HandlerMatch match(List<ResponseHandler> handlers, Request req)
            throws NotFoundException {
        for (var handler : handlers) {
            var params = handler.match(req.path);
            if (params.isPresent()) {
                return new HandlerMatch(handler, params.get());
            }
        }
        throw new NotFoundException();
    }

    static void appendHeader(BufferedWriter w, String key, Object value)
            throws IOException {
        w.write("%s: %s\r\n".formatted(key, value));
    }

    static void finishHeaders(BufferedWriter w) throws IOException {
        w.write("\r\n");
    }

    static void sendError(BufferedWriter w, HttpException he)
            throws IOException {
        switch (he) {
            case NotFoundException e -> sendStatus(w, StatusCode.NotFound);
            case BadRequestException e -> sendStatus(w, StatusCode.BadRequest);
        }
        finishHeaders(w);
        w.flush();
    }

    static void sendResponse(BufferedWriter w, Response resp)
            throws IOException {
        System.out.printf("handling done, sending response %s\n", resp);
        sendStatus(w, resp.status);
        if (resp.body.isPresent()) {
            Body body = resp.body.get();
            appendHeader(w, "Content-Type", body.contentType());
            appendHeader(w, "Content-Length", body.contentLength());
            finishHeaders(w);
            body.write(w);
        } else {
            finishHeaders(w);
        }
        w.flush();
    }

    static void handle(BufferedWriter w, BufferedReader r,
            List<ResponseHandler> handlers) throws IOException {
        try {
            System.out.println("parsing request");
            var req = Request.parseFrom(r);
            var handler = match(handlers, req);
            var resp = handler.handler.handle(req, handler.params);
            sendResponse(w, resp);
        } catch (HttpException he) {
            System.err.printf("error while handling! %s\n", he);
            sendError(w, he);
        }
    }

    static void serve(Socket client, List<ResponseHandler> handlers) {
        final var addr = client.getRemoteSocketAddress();
        System.out.printf("handling client %s\n", addr);
        try (client;
                var w = new BufferedWriter(
                        new OutputStreamWriter(client.getOutputStream()));
                var r = new BufferedReader(
                        new InputStreamReader(client.getInputStream()))) {
            System.out.printf("streams established for client %s\n", addr);
            handle(w, r, handlers);
            System.out.println("http handling done!");
        } catch (Exception e) {
            System.err.printf("error handling client %s:\n", addr);
            e.printStackTrace(System.err);
        } finally {
            System.err.printf("finished client %s\n", addr);
        }
    }

    static List<ResponseHandler> makeHandlers() {
        return List.of(
                new ResponseHandler(Pattern.compile("\\/"), List.of(),
                        (req, params) -> new Response(StatusCode.OK,
                                Optional.empty())),
                new ResponseHandler(
                        Pattern.compile("\\/echo\\/(?<text>[^\\/]+)"),
                        List.of("text"),
                        (req, params) -> new Response(StatusCode.OK,
                                Optional.of(new StringBody(params.get("text"),
                                        "text/plain")))));
    }

    public static void main(String[] args) {
        var handlers = makeHandlers();
        try (ServerSocket server = new ServerSocket(4221)) {
            server.setReuseAddress(true);
            var executor = Executors.newVirtualThreadPerTaskExecutor();
            while (true) {
                var client = server.accept();
                executor.submit(() -> serve(client, handlers));
            }
        } catch (Exception e) {
            die(e);
        }
    }
}
