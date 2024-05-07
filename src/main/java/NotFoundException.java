
public final class NotFoundException extends HttpException {
    NotFoundException() {
        super("no route found for request");
    }
}
