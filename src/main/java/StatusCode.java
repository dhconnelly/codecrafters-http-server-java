
enum StatusCode {
    BadRequest, NotFound, OK;

    int code() {
        return switch (this) {
            case BadRequest -> 400;
            case NotFound -> 404;
            case OK -> 200;
        };
    }

    String message() {
        return switch (this) {
            case BadRequest -> "Bad Request";
            case NotFound -> "Not Found";
            case OK -> "OK";
        };
    }
}
