import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;


public class Main {
    static final void die(Exception e) {
        e.printStackTrace(System.err);
        System.exit(1);
    }

    static List<Handler> makeHandlers() {
        return List.of(
                new Handler(Pattern.compile("\\/"), List.of(),
                        (req, params) -> new Response(StatusCode.OK,
                                Optional.empty())),
                new Handler(Pattern.compile("\\/echo\\/(?<text>[^\\/]+)"),
                        List.of("text"),
                        (req, params) -> new Response(StatusCode.OK,
                                Optional.of(new Body.StringBody(
                                        params.get("text"), "text/plain")))));
    }

    public static void main(String[] args) {
        var handlers = makeHandlers();
        var server = new Server(4221, handlers);
        try {
            server.run();
        } catch (Exception e) {
            die(e);
        }
    }
}
