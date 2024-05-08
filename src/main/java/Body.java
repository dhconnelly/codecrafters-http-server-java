import java.io.BufferedReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;

sealed interface Body permits Body.ReaderBody, Body.StringBody {
    String contentType();

    long contentLength();

    void write(OutputStream w) throws IOException;

    final record ReaderBody(BufferedReader r, String contentType,
            long contentLength) implements Body {
        public void write(OutputStream out) throws IOException {
            try (r) {
                var w = new OutputStreamWriter(out);
                r.transferTo(w);
                w.flush();
            }
        }
    }

    final record StringBody(String body, String contentType) implements Body {
        public long contentLength() {
            return body.length();
        }

        public void write(OutputStream w) throws IOException {
            w.write(body.getBytes(StandardCharsets.US_ASCII));
        }
    }
}
