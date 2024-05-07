import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

public record Request(String path, Map<String, String> headers,
        BufferedReader body) {

    private static final Pattern REQUEST_LINE_PAT =
            Pattern.compile("GET (\\S+) HTTP\\/1.1");

    private static Map.Entry<String, String> parseHeader(String line)
            throws BadRequestException {
        try {
            var toks = line.split(": ");
            return Map.entry(toks[0], toks[1]);
        } catch (Exception e) {
            throw new BadRequestException("invalid header: " + line);
        }
    }

    static Request parseFrom(BufferedReader r)
            throws IOException, BadRequestException {
        var requestLine = r.readLine();
        var matcher = REQUEST_LINE_PAT.matcher(requestLine);
        if (!matcher.matches()) {
            throw new BadRequestException("invalid request line");
        }
        var path = matcher.group(1);
        var headers = new HashMap<String, String>();
        for (String s; (s = r.readLine()) != null && !s.isEmpty();) {
            var kv = parseHeader(s);
            headers.put(kv.getKey(), kv.getValue());
        }
        return new Request(path, headers, r);
    }
}
