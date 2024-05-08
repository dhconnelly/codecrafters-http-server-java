import java.io.IOException;
import java.util.List;
import java.util.Optional;

public class Router {
    private final List<Handler> handlers;

    public Router(List<Handler> handlers) {
        this.handlers = handlers;
    }

    public Response handle(Request req) throws IOException {
        for (var handler : handlers) {
            var outcome = handler.handle(req);
            if (outcome.isPresent()) {
                return outcome.get();
            }
        }
        return new Response(StatusCode.NotFound, Optional.empty());
    }
}
