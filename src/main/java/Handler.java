import static java.util.stream.Collectors.toMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;

public class Handler {
    @FunctionalInterface
    interface HttpHandlerFunction {
        Response handle(Request req, Map<String, String> params);
    }

    private final Pattern p;
    private final List<String> params;
    private final Set<Method> methods;
    private final HttpHandlerFunction f;

    Handler(Pattern p, List<Method> methods, List<String> params,
            HttpHandlerFunction f) {
        this.p = p;
        this.methods = Set.copyOf(methods);
        this.params = params;
        this.f = f;
    }

    public Optional<Response> handle(Request req) {
        if (!methods.contains(req.method())) {
            return Optional.empty();
        }
        var m = p.matcher(req.path());
        if (!m.matches()) {
            return Optional.empty();
        }
        var boundParams =
                params.stream().collect(toMap(Function.identity(), m::group));
        return Optional.of(f.handle(req, boundParams));
    }

}
