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
 * {@code /.well-known/matrix/client} and exposes the protocol outcome so callers can distinguish
 * ignored discovery from failures requiring user input or termination.
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
    HttpTransport.Response response;
    try {
      response = fetchWellKnown(transport, normalizedBase);
    } catch (TransportException | MatrixException exception) {
      return new DiscoveredHomeserver(
          normalizedBase,
          null,
          null,
          false,
          DiscoveryOutcome.FAIL_PROMPT,
          "Well-known request failed");
    }
    if (response.statusCode() == 404) {
      return new DiscoveredHomeserver(
          normalizedBase,
          null,
          null,
          true,
          DiscoveryOutcome.IGNORE,
          "Well-known endpoint not found");
    }
    if (response.statusCode() != 200) {
      return new DiscoveredHomeserver(
          normalizedBase,
          null,
          null,
          false,
          DiscoveryOutcome.FAIL_PROMPT,
          "Well-known endpoint returned HTTP " + response.statusCode());
    }
    if (response.body() == null || response.body().isBlank()) {
      return new DiscoveredHomeserver(
          normalizedBase,
          null,
          null,
          false,
          DiscoveryOutcome.FAIL_PROMPT,
          "Well-known body is empty");
    }
    JsonValue wellKnown;
    try {
      wellKnown = JsonParser.parse(response.body());
    } catch (IllegalArgumentException exception) {
      return new DiscoveredHomeserver(
          normalizedBase,
          null,
          null,
          false,
          DiscoveryOutcome.FAIL_PROMPT,
          "Well-known body is not valid JSON");
    }
    if (!wellKnown.isObject()) {
      return new DiscoveredHomeserver(
          normalizedBase,
          null,
          wellKnown,
          false,
          DiscoveryOutcome.FAIL_PROMPT,
          "Well-known body is not a JSON object");
    }
    JsonValue homeserverSection = wellKnown.asObject().get("m.homeserver");
    if (homeserverSection == null || !homeserverSection.isObject()) {
      return new DiscoveredHomeserver(
          normalizedBase,
          null,
          wellKnown,
          false,
          DiscoveryOutcome.FAIL_PROMPT,
          "m.homeserver is missing");
    }
    JsonValue baseUrlValue = homeserverSection.asObject().get("base_url");
    if (baseUrlValue == null || !baseUrlValue.isString() || baseUrlValue.asString().isBlank()) {
      return new DiscoveredHomeserver(
          normalizedBase,
          null,
          wellKnown,
          false,
          DiscoveryOutcome.FAIL_PROMPT,
          "m.homeserver.base_url is missing or invalid");
    }
    String homeserverUrl;
    try {
      homeserverUrl = normalize(baseUrlValue.asString().strip());
    } catch (IllegalArgumentException exception) {
      return new DiscoveredHomeserver(
          normalizedBase,
          null,
          wellKnown,
          false,
          DiscoveryOutcome.FAIL_ERROR,
          "m.homeserver.base_url is not a valid URL");
    }
    JsonValue identityServer = wellKnown.asObject().get("m.identity_server");
    String identityServerUrl = null;
    if (identityServer != null) {
      if (!identityServer.isObject()) {
        return new DiscoveredHomeserver(
            homeserverUrl,
            null,
            wellKnown,
            false,
            DiscoveryOutcome.FAIL_PROMPT,
            "m.identity_server is invalid");
      }
      JsonValue identityUrlValue = identityServer.asObject().get("base_url");
      if (identityUrlValue == null
          || !identityUrlValue.isString()
          || identityUrlValue.asString().isBlank()) {
        return new DiscoveredHomeserver(
            homeserverUrl,
            null,
            wellKnown,
            false,
            DiscoveryOutcome.FAIL_PROMPT,
            "m.identity_server.base_url is missing");
      }
      try {
        identityServerUrl = normalize(identityUrlValue.asString().strip());
      } catch (IllegalArgumentException exception) {
        return new DiscoveredHomeserver(
            homeserverUrl,
            null,
            wellKnown,
            false,
            DiscoveryOutcome.FAIL_ERROR,
            "m.identity_server.base_url is not a valid URL");
      }
    }
    return new DiscoveredHomeserver(
        homeserverUrl, identityServerUrl, wellKnown, false, DiscoveryOutcome.DISCOVERED, null);
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
      if (uri.getRawQuery() != null || uri.getRawFragment() != null) {
        throw new IllegalArgumentException("baseUrl must not contain a query or fragment");
      }
      return url;
    } catch (URISyntaxException e) {
      throw new IllegalArgumentException("baseUrl is not a valid URI: " + baseUrl, e);
    }
  }

  private static HttpTransport.Response fetchWellKnown(
      HttpTransport transport, String normalizedBase) {
    Request request = new Request("GET", normalizedBase + WELL_KNOWN_PATH, Map.of(), null);
    return transport.send(request);
  }
}
