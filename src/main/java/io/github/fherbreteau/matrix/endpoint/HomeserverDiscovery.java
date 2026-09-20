package io.github.fherbreteau.matrix.endpoint;

import io.github.fherbreteau.matrix.error.MatrixException;
import io.github.fherbreteau.matrix.json.JsonParser;
import io.github.fherbreteau.matrix.json.JsonValue;
import io.github.fherbreteau.matrix.transport.HttpTransport;
import io.github.fherbreteau.matrix.transport.HttpTransport.Request;
import io.github.fherbreteau.matrix.transport.TransportException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

/**
 * Homeserver discovery per the Matrix specification: resolves the authoritative homeserver URL from
 * {@code /.well-known/matrix/client}, with clear fallback rules — any discovery failure (transport
 * error, non-2xx response, malformed body, missing or invalid {@code base_url}) falls back to the
 * explicitly provided base URL.
 */
@SuppressWarnings("java:S1075")
public final class HomeserverDiscovery {

  private static final String WELL_KNOWN_PATH = "/.well-known/matrix/client";

  private HomeserverDiscovery() {}

  /**
   * Discovers the homeserver behind {@code baseUrl}. The returned record carries the validated
   * homeserver URL, an optional identity server URL and the raw well-known payload; {@link
   * DiscoveredHomeserver#usedFallback()} tells whether the explicit URL was used instead of the
   * discovery result.
   *
   * @param transport the transport used to reach the homeserver
   * @param baseUrl the explicitly provided homeserver URL
   * @return the discovered homeserver, or the fallback result on failure
   */
  public static DiscoveredHomeserver discover(HttpTransport transport, String baseUrl) {
    String normalizedBase = normalize(baseUrl);
    JsonValue wellKnown = fetchWellKnown(transport, normalizedBase);
    if (wellKnown == null) {
      return new DiscoveredHomeserver(normalizedBase, null, null, true);
    }
    String homeserverUrl = extractUrl(wellKnown, "m.homeserver");
    if (homeserverUrl == null) {
      return new DiscoveredHomeserver(normalizedBase, null, wellKnown, true);
    }
    return new DiscoveredHomeserver(
        homeserverUrl, extractUrl(wellKnown, "m.identity_server"), wellKnown, false);
  }

  /**
   * Normalizes a homeserver base URL: removes trailing slashes and validates the scheme (http or
   * https).
   *
   * @param baseUrl the URL to normalize
   * @return the normalized URL without trailing slashes
   */
  public static String normalize(String baseUrl) {
    if (baseUrl == null || baseUrl.isBlank()) {
      throw new IllegalArgumentException("baseUrl is required");
    }
    String url = baseUrl.strip();
    while (url.endsWith("/")) {
      url = url.substring(0, url.length() - 1);
    }
    try {
      URI uri = new URI(url);
      String scheme = uri.getScheme();
      if (scheme == null
          || !(scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https"))) {
        throw new IllegalArgumentException("baseUrl must use the http or https scheme: " + baseUrl);
      }
      return url;
    } catch (URISyntaxException e) {
      throw new IllegalArgumentException("baseUrl is not a valid URI: " + baseUrl, e);
    }
  }

  private static JsonValue fetchWellKnown(HttpTransport transport, String normalizedBase) {
    Request request = new Request("GET", normalizedBase + WELL_KNOWN_PATH, Map.of(), null);
    HttpTransport.Response response;
    try {
      response = transport.send(request);
    } catch (TransportException | MatrixException _) {
      return null;
    }
    if (response.statusCode() < 200 || response.statusCode() >= 300) {
      return null;
    }
    if (response.body() == null || response.body().isBlank()) {
      return null;
    }
    try {
      JsonValue body = JsonParser.parse(response.body());
      return body.isObject() ? body : null;
    } catch (IllegalArgumentException _) {
      return null;
    }
  }

  private static String extractUrl(JsonValue wellKnown, String key) {
    JsonValue section = wellKnown.asObject().get(key);
    if (section == null || !section.isObject()) {
      return null;
    }
    JsonValue baseUrlValue = section.asObject().get("base_url");
    if (baseUrlValue == null || !baseUrlValue.isString()) {
      return null;
    }
    String url = baseUrlValue.asString().strip();
    if (url.isEmpty()) {
      return null;
    }
    try {
      return normalize(url);
    } catch (IllegalArgumentException _) {
      return null;
    }
  }
}
