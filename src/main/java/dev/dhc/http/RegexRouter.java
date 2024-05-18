package dev.dhc.http;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;
import static java.util.stream.Collectors.toMap;

public class RegexRouter implements Handler {

    private record Route(Set<Method> methods,
            Pattern pattern,
            Set<String> params,
            Handler handler) {

        private Optional<Response> handle(Request req) {
            if (!methods.contains(req.getMethod())) {
                return Optional.empty();
            }
            var m = pattern.matcher(req.getPath());
            if (!m.matches()) {
                return Optional.empty();
            }
            var boundParams = params.stream()
                    .collect(toMap(Function.identity(), m::group));
            return Optional.of(handler.handle(req.withParams(boundParams)));
        }

    }

    private final List<Route> routes = new ArrayList<>();

    public void addRoute(
            Set<Method> methods,
            Pattern pattern,
            Set<String> params,
            Handler handler
    ) {
        routes.add(new Route(methods, pattern, params, handler));
    }

    @Override
    public Response handle(Request req) {
        return routes.stream()
                .map(route -> route.handle(req))
                .flatMap(Optional::stream)
                .findFirst()
                .orElseGet(Response::notFound);
    }
}
