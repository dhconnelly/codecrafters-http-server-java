import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;

sealed interface Body permits Body.ReaderBody, Body.StringBody {
    String contentType();

    long contentLength();

    void write(BufferedWriter w) throws IOException;

    final record ReaderBody(BufferedReader r, String contentType,
            long contentLength) implements Body {
        public void write(BufferedWriter w) throws IOException {
            r.transferTo(w);
        }
    }

    final record StringBody(String body, String contentType) implements Body {
        public long contentLength() {
            return body.length();
        }

        public void write(BufferedWriter w) throws IOException {
            w.write(body);
        }
    }
}
