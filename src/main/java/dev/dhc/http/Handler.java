package dev.dhc.http;

public interface Handler {

    default Response handle(Request req) {
        return switch(req.getMethod()) {
            case GET -> get(req);
            case POST -> post(req);
        };
    }

    default Response get(Request req) {
        return Response.notFound();
    }

    default Response post(Request req) {
        return Response.notFound();
    }
}
