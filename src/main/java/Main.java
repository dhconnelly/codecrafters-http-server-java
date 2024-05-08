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

        Path checkPath(Path path) throws BadRequestException {
            path = path.normalize();
            if (root.relativize(path).startsWith("..")) {
                throw new BadRequestException("path not contained by root");
            }
            return path;
        }

        Response handleFileGet(Path root, Request req,
                Map<String, String> params) throws HttpException {
            Path path = checkPath(root.resolve(params.get("filename")));
            if (!Files.isReadable(path)) {
                throw new NotFoundException();
            }
            BufferedReader r;
            long size;
            try {
                r = new BufferedReader(
                        new InputStreamReader(Files.newInputStream(path)));
                size = Files.size(path);
            } catch (IOException e) {
                e.printStackTrace(System.err);
                throw new InternalServerErrorException(
                        "error while reading file");
            }
            return new Response(StatusCode.OK, Optional.of(
                    new Body.ReaderBody(r, "application/octet-stream", size)));
        }

        void copy(OutputStream w, InputStream r, int n)
                throws IOException, BadRequestException {
            System.out.printf("transferring %d bytes...\n", n);
            byte buf[] = new byte[4096];
            for (int k = 0; k < n;) {
                int chunk = Math.min(4096, n - k);
                System.out.printf("reading %d bytes...\n", chunk);
                int read = r.read(buf, 0, chunk);
                System.out.printf("read %d bytes\n", read);
                if (read <= 0) {
                    throw new BadRequestException("can't read enough bytes");
                }
                w.write(buf, 0, read);
                System.out.printf("sent %d bytes...\n", read);
                k += read;
                System.out.printf("%d bytes remaining\n", n - k);
            }
        }

        Response handleFilePost(Path root, Request req,
                Map<String, String> params) throws HttpException {
            Path path = checkPath(root.resolve(params.get("filename")));
            int size;
            try {
                size = Integer.parseInt(req.headers().get("Content-Length"));
            } catch (Exception e) {
                throw new BadRequestException(
                        "must specify integer Content-Length");
            }
            System.out.printf("writing content to disk at %s\n", path);
            try (var w = Files.newOutputStream(path)) {
                copy(w, req.body(), size);
            } catch (IOException e) {
                e.printStackTrace(System.err);
                throw new InternalServerErrorException(
                        "error while writing file");
            }
            return new Response(StatusCode.OK, Optional.empty());
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
