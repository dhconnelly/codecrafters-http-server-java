import static java.util.stream.Collectors.toMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Pattern;

public class Handler {
    @FunctionalInterface
    interface HttpHandlerFunction {
        Response handle(Request req, Map<String, String> params)
                throws HttpException;
    }

    private final Pattern p;
    private final List<String> params;
    private final HttpHandlerFunction f;

    Handler(Pattern p, List<String> params, Handler.HttpHandlerFunction f) {
        this.p = p;
        this.params = params;
        this.f = f;
    }

    public Optional<Map<String, String>> match(String path) {
        var m = p.matcher(path);
        if (!m.matches()) {
            return Optional.empty();
        }
        return Optional.of(
                params.stream().collect(toMap(Function.identity(), m::group)));
    }

    public Response handle(Request req, Map<String, String> params)
            throws HttpException {
        return f.handle(req, params);
    }

}
