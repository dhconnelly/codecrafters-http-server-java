package dev.dhc.http;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.Scanner;

public sealed interface Body permits Body.StreamBody, Body.StringBody, Body.EmptyBody {

    public Optional<String> getContentType();

    public long contentLength();

    public void write(OutputStream w) throws IOException;

    public String takeString() throws IOException;

    public final record EmptyBody() implements Body {

        @Override
        public Optional<String> getContentType() {
            return Optional.empty();
        }

        @Override
        public long contentLength() {
            return 0;
        }

        @Override
        public void write(OutputStream w) throws IOException {
        }

        @Override
        public String takeString() throws IOException {
            return "";
        }
    }

    public final record StreamBody(InputStream r, String contentType,
            long contentLength) implements Body {

        @Override
        public void write(OutputStream out) throws IOException {
            try (r) {
                long written = r.transferTo(out);
                if (written != contentLength) {
                    throw new AssertionError(
                            "bad content length: expected %d, got %d".formatted(
                                    contentLength, written));
                }
            }
        }

        @Override
        public String takeString() throws IOException {
            // https://stackoverflow.com/a/35446009
            var s = new Scanner(r).useDelimiter("\\A");
            return s.next();
        }

        @Override
        public Optional<String> getContentType() {
            return Optional.of(contentType);
        }
    }

    public final record StringBody(String body, String contentType) implements Body {

        @Override
        public long contentLength() {
            return body.length();
        }

        @Override
        public Optional<String> getContentType() {
            return Optional.of(contentType);
        }

        @Override
        public void write(OutputStream w) throws IOException {
            w.write(body.getBytes(StandardCharsets.US_ASCII));
        }

        @Override
        public String takeString() {
            return body;
        }
    }
}
