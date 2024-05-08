import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;


public class Main {
    static final void die(Exception e) {
        e.printStackTrace(System.err);
        System.exit(1);
    }

    static final void die(String message) {
        System.err.println(message);
        System.exit(1);
    }

    static class FileSystemHandler {
        private final Path root;

        public FileSystemHandler(Path root) {
            this.root = root;
        }

        Optional<Path> getValidPath(String filename) {
            Path path = root.resolve(filename).normalize();
            return root.relativize(path).startsWith("..") ? Optional.empty()
                    : Optional.of(path);
        }

        Response handleFileGet(Path root, Request req,
                Map<String, String> params) {
            Optional<Path> validPath = getValidPath(params.get("filename"));
            if (!validPath.isPresent()) {
                return new Response(StatusCode.NotFound, Optional.empty());
            }
            Path path = validPath.get();
            if (!Files.isReadable(path)) {
                return new Response(StatusCode.NotFound, Optional.empty());
            }
            BufferedReader r;
            long size;
            try {
                r = new BufferedReader(
                        new InputStreamReader(Files.newInputStream(path)));
                size = Files.size(path);
            } catch (IOException e) {
                return new Response(StatusCode.InternalServerError,
                        Optional.empty());
            }
            return new Response(StatusCode.OK, Optional.of(
                    new Body.ReaderBody(r, "application/octet-stream", size)));
        }

        void copy(OutputStream w, InputStream r, int n)
                throws IOException, BadRequestException {
            byte buf[] = new byte[4096];
            for (int k = 0; k < n;) {
                int chunk = Math.min(4096, n - k);
                int read = r.read(buf, 0, chunk);
                if (read <= 0) {
                    throw new BadRequestException("can't read enough bytes");
                }
                w.write(buf, 0, read);
                k += read;
            }
        }

        Response handleFilePost(Path root, Request req,
                Map<String, String> params) {
            Optional<Path> validPath = getValidPath(params.get("filename"));
            if (!validPath.isPresent()) {
                return new Response(StatusCode.NotFound, Optional.empty());
            }
            Path path = validPath.get();
            int size;
            try {
                size = Integer.parseInt(req.headers().get("Content-Length"));
            } catch (Exception e) {
                return new Response(StatusCode.BadRequest, Optional.empty());
            }
            try (var w = Files.newOutputStream(path)) {
                copy(w, req.body(), size);
            } catch (IOException e) {
                return new Response(StatusCode.InternalServerError,
                        Optional.empty());
            } catch (BadRequestException e) {
                return new Response(StatusCode.BadRequest, Optional.empty());
            }
            return new Response(StatusCode.Created, Optional.empty());
        }
    }

    static List<Handler> makeHandlers(String[] args) {
        Path root = args.length == 0 ? Path.of(".") : Path.of(args[1]);
        var fs = new FileSystemHandler(root);
        return List.of(
                new Handler(Pattern.compile("\\/"), List.of(Method.GET),
                        List.of(),
                        (req, params) -> new Response(StatusCode.OK,
                                Optional.empty())),
                new Handler(Pattern.compile("\\/echo\\/(?<text>[^\\/]+)"),
                        List.of(Method.GET), List.of("text"),
                        (req, params) -> new Response(StatusCode.OK,
                                Optional.of(new Body.StringBody(
                                        params.get("text"), "text/plain")))),
                new Handler(Pattern.compile("\\/files\\/(?<filename>[^\\/]+)"),
                        List.of(Method.GET), List.of("filename"),
                        (req, params) -> fs.handleFileGet(root, req, params)),
                new Handler(Pattern.compile("\\/files\\/(?<filename>[^\\/]+)"),
                        List.of(Method.POST), List.of("filename"),
                        (req, params) -> fs.handleFilePost(root, req, params)),
                new Handler(Pattern.compile("\\/user-agent"),
                        List.of(Method.GET), List.of(),
                        (req, params) -> new Response(StatusCode.OK,
                                Optional.of(new Body.StringBody(
                                        req.headers().get("User-Agent"),
                                        "text/plain")))));
    }

    public static void main(String[] args) {
        var handlers = makeHandlers(args);
        var server = new Server(4221, handlers);
        try {
            server.run();
        } catch (Exception e) {
            die(e);
        }
    }
}
