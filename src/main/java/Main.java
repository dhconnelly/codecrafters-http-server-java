import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
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

    static Response fileHandler(Path root, Request req,
            Map<String, String> params) throws HttpException {
        Path path = root.resolve(params.get("filename"));
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
            throw new InternalServerErrorException("error while reading file");
        }
        return new Response(StatusCode.OK, Optional
                .of(new Body.ReaderBody(r, "application/octet-stream", size)));
    }

    static List<Handler> makeHandlers(String[] args) {
        Path root = args.length == 0 ? Path.of(".") : Path.of(args[1]);
        return List.of(
                new Handler(Pattern.compile("\\/"), List.of(),
                        (req, params) -> new Response(StatusCode.OK,
                                Optional.empty())),
                new Handler(Pattern.compile("\\/echo\\/(?<text>[^\\/]+)"),
                        List.of("text"),
                        (req, params) -> new Response(StatusCode.OK,
                                Optional.of(new Body.StringBody(
                                        params.get("text"), "text/plain")))),
                new Handler(Pattern.compile("\\/files\\/(?<filename>[^\\/]+)"),
                        List.of("filename"),
                        (req, params) -> fileHandler(root, req, params)),
                new Handler(Pattern.compile("\\/user-agent"), List.of(),
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
