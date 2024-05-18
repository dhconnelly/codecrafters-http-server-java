package dev.dhc.http;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

public class Request {

    private final Map<String, String> headers;
    private final Map<String, String> params;
    private final String path;
    private final Method method;

    private InputStream body;

    private Request(Method method, String path, Map<String, String> headers, Map<String, String> params, InputStream body) {
        this.headers = headers;
        this.path = path;
        this.method = method;
        this.body = body;
        this.params = params;
    }

    private Request(Method method, String path, Map<String, String> headers, InputStream body) {
        this(method, path, headers, Map.of(), body);
    }

    private static final Pattern REQUEST_LINE_PAT
            = Pattern.compile("(GET|POST) (\\S+) HTTP\\/1.1");

    private static Map.Entry<String, String> parseHeader(String line)
            throws BadRequestException {
        try {
            var toks = line.split(": ");
            return Map.entry(toks[0], toks[1]);
        } catch (Exception e) {
            throw new BadRequestException("invalid header: " + line);
        }
    }

    // custom readline to avoid using InputStreamReader, which reads ahead
    // in the underlying stream -- whereas we need to keep all of that unread
    // so we can pass it off to the client.
    private static String readLine(InputStream r) throws IOException {
        var b = new StringBuilder();
        for (int c; (c = r.read()) >= 0;) {
            b.append((char) c);
            int n = b.length();
            if (n >= 2 && b.charAt(n - 2) == '\r' && b.charAt(n - 1) == '\n') {
                var line = b.substring(0, n - 2);
                b.setLength(0);
                return line;
            }
        }
        return b.toString();
    }

    public static Request parseFrom(InputStream in)
            throws IOException, BadRequestException {
        var requestLine = readLine(in);
        var matcher = REQUEST_LINE_PAT.matcher(requestLine);
        if (!matcher.matches()) {
            throw new BadRequestException("invalid request line");
        }
        Method method;
        try {
            method = Method.valueOf(matcher.group(1));
        } catch (Exception e) {
            throw new BadRequestException("unsupported http method");
        }
        var path = matcher.group(2);
        var headers = new HashMap<String, String>();
        for (String s; (s = readLine(in)) != null && !s.isEmpty();) {
            var kv = parseHeader(s);
            headers.put(kv.getKey(), kv.getValue());
        }
        return new Request(method, path, headers, in);
    }

    public Request withParams(Map<String, String> params) {
        return new Request(method, path, headers, params, body);
    }

    public Method getMethod() {
        return method;
    }

    public String getPath() {
        return path;
    }

    public InputStream getBody() {
        return body;
    }

    public String getHeader(String header) {
        return headers.get(header);
    }

    public String getParam(String param) {
        return params.get(param);
    }
}
