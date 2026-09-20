package io.github.fherbreteau.matrix.endpoint;

import java.util.Map;

import io.github.fherbreteau.matrix.error.MatrixServerException;
import io.github.fherbreteau.matrix.json.JsonObject;
import io.github.fherbreteau.matrix.json.JsonParser;
import io.github.fherbreteau.matrix.json.JsonValue;
import io.github.fherbreteau.matrix.model.MatrixVersions;
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
 *
 * <p>With homeserver discovery ({@code /.well-known/matrix/client}) and
 * capability validation at build time:
 * <pre>{@code
 * MatrixClient client = MatrixClient.builder("https://matrix.example.org")
 *         .discover()
 *         .build();
 * }</pre>
 */
public final class MatrixClient {

    private final HttpTransport transport;
    private final String homeserverUrl;
    private final MatrixVersions versions;
    private final DiscoveredHomeserver discovery;

    private MatrixClient(Builder builder) {
        this.transport = builder.transport;
        if (builder.discover) {
            this.discovery = HomeserverDiscovery.discover(builder.transport, builder.homeserverUrl);
            this.homeserverUrl = discovery.homeserverUrl();
        } else {
            this.discovery = null;
            this.homeserverUrl = builder.homeserverUrl;
        }
        this.versions = builder.validateVersions
                ? MatrixVersions.from(get(builder.transport, homeserverUrl, "_matrix/client/versions"))
                : null;
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
     * Returns the result of the discovery performed at build time, or
     * {@code null} when discovery was not requested.
     */
    public DiscoveredHomeserver getDiscovery() {
        return discovery;
    }

    /**
     * Returns the validated capabilities of the homeserver when version
     * validation was requested at build time, or {@code null} otherwise.
     */
    public MatrixVersions getCapabilities() {
        return versions;
    }

    /**
     * Retrieves the homeserver's supported Matrix spec versions.
     */
    public JsonValue getVersions() {
        return get("_matrix/client/versions");
    }

    /**
     * Retrieves and validates the homeserver's supported Matrix spec
     * versions. Unknown fields (such as {@code unstable_features}) are
     * preserved on the returned {@link MatrixVersions}.
     *
     * @throws io.github.fherbreteau.matrix.error.DiscoveryException if the response is malformed
     */
    public MatrixVersions getSupportedVersions() {
        return MatrixVersions.from(getVersions());
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
        Request request = new Request(method, homeserverUrl + "/" + path, Map.of(), body);
        HttpTransport.Response response = transport.send(request);
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw MatrixServerException.fromResponse(response.statusCode(),
                    parseOrNull(response.body()),
                    response.headers());
        }
        if (response.body() == null || response.body().isBlank()) {
            return new JsonObject();
        }
        return JsonParser.parse(response.body());
    }

    private static JsonValue get(HttpTransport transport, String base, String path) {
        Request request = new Request("GET", base + "/" + path, Map.of(), null);
        HttpTransport.Response response = transport.send(request);
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw MatrixServerException.fromResponse(response.statusCode(),
                    parseOrNull(response.body()),
                    response.headers());
        }
        if (response.body() == null || response.body().isBlank()) {
            return new JsonObject();
        }
        return JsonParser.parse(response.body());
    }

    private static JsonValue parseOrNull(String body) {
        if (body == null || body.isBlank()) {
            return null;
        }
        try {
            return JsonParser.parse(body);
        } catch (IllegalArgumentException _) {
            return null;
        }
    }

    public static final class Builder {

        private final String homeserverUrl;
        private HttpTransport transport = HttpTransport.create();
        private boolean discover;
        private boolean validateVersions;

        private Builder(String homeserverUrl) {
            if (homeserverUrl == null || homeserverUrl.isBlank()) {
                throw new IllegalArgumentException("homeserverUrl is required");
            }
            this.homeserverUrl = homeserverUrl.endsWith("/")
                    ? homeserverUrl.substring(0, homeserverUrl.length() - 1)
                    : homeserverUrl;
        }

        /**
         * Resolves the homeserver URL through {@code /.well-known/matrix/client}
         * at build time; on any discovery failure the explicit base URL is
         * used as fallback.
         */
        public Builder discover() {
            this.discover = true;
            return this;
        }

        /**
         * Fetches and validates {@code /_matrix/client/versions} at build
         * time so malformed capability responses fail fast.
         */
        public Builder validateVersions() {
            this.validateVersions = true;
            return this;
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
