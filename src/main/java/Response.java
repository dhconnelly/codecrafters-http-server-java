import java.util.Optional;

public record Response(StatusCode status, Optional<Body> body) {

}
