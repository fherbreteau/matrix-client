package io.github.fherbreteau.matrix.transport;

import java.util.Map;

/**
 * Abstraction over the HTTP layer used to talk to a homeserver.
 * The default implementation relies only on {@code java.net.http.HttpClient}.
 */
public interface HttpTransport {

    Response send(Request request);

    record Request(String method, String url, Map<String, String> headers, String body) {
    }

    record Response(int statusCode, String body) {
    }

    static HttpTransport create() {
        return new JdkHttpTransport();
    }
}
