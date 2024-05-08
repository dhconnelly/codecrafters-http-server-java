import java.io.IOException;
import java.util.List;
import java.util.Map;

public class Router {
    private final List<Handler> handlers;

    public Router(List<Handler> handlers) {
        this.handlers = handlers;
    }

    private static final record HandlerMatch(Handler handler,
            Map<String, String> params) {
    }

    private HandlerMatch match(Request req) throws NotFoundException {
        for (var handler : handlers) {
            var params = handler.match(req.method(), req.path());
            if (params.isPresent()) {
                return new HandlerMatch(handler, params.get());
            }
        }
        throw new NotFoundException();
    }

    public Response handle(Request req) throws HttpException, IOException {
        var handler = match(req);
        return handler.handler.handle(req, handler.params);
    }

}
