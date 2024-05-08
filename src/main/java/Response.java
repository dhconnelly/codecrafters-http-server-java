import static java.nio.charset.StandardCharsets.US_ASCII;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Optional;

record Response(StatusCode status, Optional<Body> body) {
    private void sendStatus(OutputStream w, StatusCode status)
            throws IOException {
        w.write("HTTP/1.1 %d %s\r\n".formatted(status.code(), status.message())
                .getBytes(US_ASCII));
    }

    private void appendHeader(OutputStream w, String key, Object value)
            throws IOException {
        w.write("%s: %s\r\n".formatted(key, value).getBytes(US_ASCII));
    }

    private void finishHeaders(OutputStream w) throws IOException {
        w.write("\r\n".getBytes(US_ASCII));
    }

    public void write(OutputStream w) throws IOException {
        sendStatus(w, status);
        if (body.isPresent()) {
            var b = body.get();
            appendHeader(w, "Content-Type", b.contentType());
            appendHeader(w, "Content-Length", b.contentLength());
            finishHeaders(w);
            b.write(w);
        } else {
            finishHeaders(w);
        }
        w.flush();
    }
}
