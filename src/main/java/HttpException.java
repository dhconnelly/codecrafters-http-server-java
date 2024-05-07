
public sealed abstract class HttpException extends Exception
        permits BadRequestException, NotFoundException {
    HttpException(String message) {
        super(message);
    }
}
