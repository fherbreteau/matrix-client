package io.github.fherbreteau.matrix.transport;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;

/**
 * Default {@link HttpTransport} built on the JDK's {@code java.net.http.HttpClient}.
 */
public final class JdkHttpTransport implements HttpTransport {

    private final HttpClient client;

    public JdkHttpTransport() {
        this(HttpClient.newHttpClient());
    }

    public JdkHttpTransport(HttpClient client) {
        this.client = client;
    }

    @Override
    public Response send(Request request) {
        var builder = HttpRequest.newBuilder(URI.create(request.url()));
        builder.header("Accept", "application/json");
        if (request.body() != null) {
            builder.header("Content-Type", "application/json");
            builder.method(request.method(),
                    HttpRequest.BodyPublishers.ofString(request.body()));
        } else {
            builder.method(request.method(), HttpRequest.BodyPublishers.noBody());
        }
        for (Map.Entry<String, String> header : request.headers().entrySet()) {
            builder.header(header.getKey(), header.getValue());
        }
        try {
            HttpResponse<String> response =
                    client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            return new Response(response.statusCode(), response.body());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("HTTP request interrupted", e);
        } catch (java.io.IOException e) {
            throw new UncheckedTransportException("HTTP request failed", e);
        }
    }
}
