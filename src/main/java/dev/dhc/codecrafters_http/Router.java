package dev.dhc.codecrafters_http;

import java.nio.file.Path;
import java.util.Set;
import java.util.regex.Pattern;

import dev.dhc.http.FileSystemHandler;
import dev.dhc.http.Method;
import dev.dhc.http.RegexRouter;
import dev.dhc.http.SimpleHandler;

public class Router {
    public static RegexRouter make(Path root) {
        var router = new RegexRouter();
        router.addRoute(
                Set.of(Method.GET),
                Pattern.compile("\\/"),
                Set.of(),
                new SimpleHandler());
        router.addRoute(
                Set.of(Method.GET),
                Pattern.compile("\\/user-agent"),
                Set.of(),
                new SimpleHandler(req -> req.getHeader("User-Agent")));
        router.addRoute(
                Set.of(Method.GET),
                Pattern.compile("\\/echo\\/(?<text>[^\\/]+)"),
                Set.of("text"),
                new SimpleHandler(req -> req.getParam("text")));
        router.addRoute(
                Set.of(Method.GET, Method.POST),
                Pattern.compile("\\/files\\/(?<filename>[^\\/]+)"),
                Set.of("filename"),
                new FileSystemHandler(root));
        return router;
    }

}
