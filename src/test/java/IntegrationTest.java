
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.dhc.codecrafters_http.Router;
import dev.dhc.http.BadRequestException;
import dev.dhc.http.Handler;
import dev.dhc.http.Request;
import dev.dhc.http.StatusCode;

public class IntegrationTest {

    private Path fsRoot;
    private Handler handler;

    @BeforeEach
    public void setUp() throws IOException {
        fsRoot = Files.createTempDirectory(null);
        handler = Router.make(fsRoot);
    }

    private static final String USER_AGENT = "codecrafters-it";

    private Request get(String path) throws IOException, BadRequestException {
        var out = new ByteArrayOutputStream();
        var w = new OutputStreamWriter(out, StandardCharsets.US_ASCII);
        w.write("GET %s HTTP/1.1\r\n".formatted(path));
        w.write("User-Agent: %s\r\n".formatted(USER_AGENT));
        w.write("\r\n");
        w.flush();
        return Request.parseFrom(new ByteArrayInputStream(out.toByteArray()));
    }

    private Request post(String path, InputStream in, long contentLength) throws IOException, BadRequestException {
        var out = new ByteArrayOutputStream();
        var w = new OutputStreamWriter(out, StandardCharsets.US_ASCII);
        w.write("POST %s HTTP/1.1\r\n".formatted(path));
        w.write("User-Agent: %s\r\n".formatted(USER_AGENT));
        w.write("Content-Length: %d\r\n".formatted(contentLength));
        w.write("\r\n");
        w.flush();
        in.transferTo(out);
        out.flush();
        return Request.parseFrom(new ByteArrayInputStream(out.toByteArray()));
    }

    @Test
    public void test404() throws IOException, BadRequestException {
        var req = get("/foo");
        var resp = handler.handle(req);
        assertEquals(StatusCode.NotFound, resp.getStatus());
        assertEquals(0, resp.getBody().contentLength());
    }

    @Test
    public void testRoot() throws IOException, BadRequestException {
        var req = get("/");
        var resp = handler.handle(req);
        assertEquals(StatusCode.OK, resp.getStatus());
        assertEquals(0, resp.getBody().contentLength());
    }

    @Test
    public void testUserAgent() throws IOException, BadRequestException {
        var req = get("/user-agent");
        var resp = handler.handle(req);
        assertEquals(StatusCode.OK, resp.getStatus());
        assertEquals(USER_AGENT.length(), resp.getBody().contentLength());
        assertEquals(USER_AGENT, resp.getBody().takeString());
    }

    @Test
    public void testEcho() throws IOException, BadRequestException {
        var req = get("/echo/foo");
        var resp = handler.handle(req);
        assertEquals(StatusCode.OK, resp.getStatus());
        assertEquals("foo".length(), resp.getBody().contentLength());
        assertEquals("foo", resp.getBody().takeString());
    }

    @Test
    public void testGetFile() throws BadRequestException, IOException {
        Files.write(
                fsRoot.resolve("hello.txt"),
                "hello".getBytes(StandardCharsets.US_ASCII));

        var req = get("/files/hello.txt");
        var resp = handler.handle(req);
        assertEquals(StatusCode.OK, resp.getStatus());
        assertEquals("application/octet-stream", resp.getBody().getContentType().get());
        assertEquals("hello".length(), resp.getBody().contentLength());
        assertEquals("hello", resp.getBody().takeString());
    }

    @Test
    public void testPostFile() throws IOException, BadRequestException {
        var buf = "goodbye".getBytes(StandardCharsets.US_ASCII);
        var req = post(
                "/files/goodbye.txt",
                new ByteArrayInputStream(buf),
                buf.length);
        var resp = handler.handle(req);
        assertEquals(StatusCode.Created, resp.getStatus());
        assertEquals(0, resp.getBody().contentLength());
        assertEquals(
                "goodbye",
                Files.readString(
                        fsRoot.resolve("goodbye.txt"),
                        StandardCharsets.US_ASCII));
    }
}
