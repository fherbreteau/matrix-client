package io.github.fherbreteau.matrix.endpoint;

import java.util.HashMap;
import java.util.Map;

import io.github.fherbreteau.matrix.error.MatrixServerException;
import io.github.fherbreteau.matrix.json.JsonParser;
import io.github.fherbreteau.matrix.json.JsonValue;
import io.github.fherbreteau.matrix.transport.HttpTransport;
import io.github.fherbreteau.matrix.transport.HttpTransport.Request;

/**
 * Entry point for talking to a Matrix homeserver.
 *
 * <p>Minimal example:
 * <pre>{@code
 * MatrixClient client = MatrixClient.builder("https://matrix.example.org").build();
 * JsonValue versions = client.getVersions();
 * }</pre>
 */
public final class MatrixClient {

    private final HttpTransport transport;
    private final String homeserverUrl;

    private MatrixClient(Builder builder) {
        this.transport = builder.transport;
        this.homeserverUrl = builder.homeserverUrl;
    }

    public static Builder builder(String homeserverUrl) {
        return new Builder(homeserverUrl);
    }

    public HttpTransport getTransport() {
        return transport;
    }

    public String getHomeserverUrl() {
        return homeserverUrl;
    }

    /**
     * Retrieves the homeserver's supported Matrix spec versions.
     */
    public JsonValue getVersions() {
        return get("_matrix/client/versions");
    }

    /**
     * Performs a GET request against the homeserver and returns the parsed JSON body.
     */
    public JsonValue get(String path) {
        return request("GET", path, null);
    }

    /**
     * Performs a POST request with a JSON body and returns the parsed JSON response.
     */
    public JsonValue post(String path, JsonValue body) {
        return request("POST", path, body == null ? null : body.toJson());
    }

    public JsonValue request(String method, String path, String body) {
        Map<String, String> headers = new HashMap<>();
        Request request = new Request(method, homeserverUrl + "/" + path, headers, body);
        HttpTransport.Response response = transport.send(request);
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            JsonValue parsed = null;
            try {
                parsed = JsonParser.parse(response.body());
            } catch (IllegalArgumentException ignored) {
                // non-JSON error body; errcode will fall back to M_UNRECOGNIZED
            }
            throw MatrixServerException.fromResponse(response.statusCode(), parsed);
        }
        if (response.body() == null || response.body().isBlank()) {
            return new io.github.fherbreteau.matrix.json.JsonObject();
        }
        return JsonParser.parse(response.body());
    }

    public static final class Builder {

        private final String homeserverUrl;
        private HttpTransport transport = HttpTransport.create();

        private Builder(String homeserverUrl) {
            if (homeserverUrl == null || homeserverUrl.isBlank()) {
                throw new IllegalArgumentException("homeserverUrl is required");
            }
            this.homeserverUrl = homeserverUrl.endsWith("/")
                    ? homeserverUrl.substring(0, homeserverUrl.length() - 1)
                    : homeserverUrl;
        }

        public Builder transport(HttpTransport transport) {
            this.transport = transport;
            return this;
        }

        public MatrixClient build() {
            return new MatrixClient(this);
        }
    }
}
