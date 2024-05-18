package dev.dhc.http;


import java.util.Optional;
import java.util.function.Function;

public class SimpleHandler implements Handler {

    private final Function<Request, Optional<String>> f;

    public SimpleHandler(Function<Request, String> f) {
        this.f = req -> Optional.of(f.apply(req));
    }

    public SimpleHandler() {
        this.f = req -> Optional.empty();
    }

    @Override
    public Response handle(Request req) {
        Body body = f.apply(req)
            .map(s -> (Body) new Body.StringBody(s))
            .orElse((Body) new Body.EmptyBody());
        return new Response(StatusCode.OK, body);
    }
}
