package dev.dhc.http;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

public class FileSystemHandler implements Handler {

    private final Path root;

    public FileSystemHandler(Path root) {
        this.root = root;
    }

    private Optional<Path> getValidPath(String filename) {
        Path path = root.resolve(filename).normalize();
        return root.relativize(path).startsWith("..") ? Optional.empty()
                : Optional.of(path);
    }

    private void copy(OutputStream w, InputStream r, int n)
            throws IOException, BadRequestException {
        byte buf[] = new byte[4096];
        for (int k = 0; k < n;) {
            int chunk = Math.min(4096, n - k);
            int read = r.read(buf, 0, chunk);
            if (read <= 0) {
                throw new BadRequestException("can't read enough bytes");
            }
            w.write(buf, 0, read);
            k += read;
        }
    }

    @Override
    public Response get(Request req) {
        Optional<Path> validPath = getValidPath(req.getParam("filename"));
        if (!validPath.isPresent()) {
            return new Response(StatusCode.NotFound);
        }
        Path path = validPath.get();
        if (!Files.isReadable(path)) {
            return new Response(StatusCode.NotFound);
        }
        InputStream r;
        long size;
        try {
            r = Files.newInputStream(path);
            size = Files.size(path);
        } catch (IOException e) {
            return new Response(StatusCode.InternalServerError);
        }
        return new Response(
            StatusCode.OK,
            new Body.StreamBody(r, "application/octet-stream", size));
    }

    @Override
    public Response post(Request req) {
        Optional<Path> validPath = getValidPath(req.getParam("filename"));
        if (!validPath.isPresent()) {
            return new Response(StatusCode.NotFound);
        }
        Path path = validPath.get();
        int size;
        try {
            size = Integer.parseInt(req.getHeader("Content-Length"));
        } catch (NumberFormatException e) {
            return new Response(StatusCode.BadRequest);
        }
        try (var w = Files.newOutputStream(path)) {
            copy(w, req.getBody(), size);
            if (Files.size(path) != size) {
                return new Response(
                    StatusCode.BadRequest,
                    new Body.StringBody("bad content length", "text/plain"));
            }
        } catch (IOException e) {
            return new Response(StatusCode.InternalServerError);
        } catch (BadRequestException e) {
            return new Response(StatusCode.BadRequest);
        }
        return new Response(StatusCode.Created);
    }
}
