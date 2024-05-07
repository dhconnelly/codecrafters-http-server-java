
public sealed abstract class HttpException extends Exception permits
        BadRequestException, NotFoundException, InternalServerErrorException {
    HttpException(String message) {
        super(message);
    }
}
