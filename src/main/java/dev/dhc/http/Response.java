package dev.dhc.http;

import java.io.IOException;
import java.io.OutputStream;
import static java.nio.charset.StandardCharsets.US_ASCII;

public class Response {

    private final StatusCode status;
    private final Body body;

    Response(StatusCode status, Body body) {
        this.status = status;
        this.body = body;
    }

    Response(StatusCode status) {
        this(status, new Body.EmptyBody());
    }

    public StatusCode getStatus() {
        return status;
    }

    public Body getBody() {
        return body;
    }

    static Response notFound() {
        return new Response(StatusCode.NotFound);
    }

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

    void write(OutputStream w) throws IOException {
        sendStatus(w, status);
        appendHeader(w, "Content-Length", body.contentLength());
        if (body.getContentType().isPresent()) {
            appendHeader(w, "Content-Type", body.getContentType().get());
        }
        finishHeaders(w);
        if (body.contentLength() > 0) {
            body.write(w);
        }
        w.flush();
    }
}
