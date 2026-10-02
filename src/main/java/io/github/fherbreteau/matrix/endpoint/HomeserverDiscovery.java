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
    } catch (TransportException | MatrixException _) {
      return new DiscoveredHomeserver(
          normalizedBase,
          null,
          null,
          false,
          DiscoveryOutcome.FAIL_PROMPT,
          "Well-known request failed");
    }
    DiscoveredHomeserver responseOutcome = responseOutcome(normalizedBase, response);
    if (responseOutcome != null) {
      return responseOutcome;
    }
    JsonValue wellKnown = parseWellKnown(normalizedBase, response.body());
    if (wellKnown == null || !wellKnown.isObject()) {
      return failed(
          normalizedBase,
          wellKnown,
          DiscoveryOutcome.FAIL_PROMPT,
          wellKnown == null
              ? "Well-known body is not valid JSON"
              : "Well-known body is not a JSON object");
    }
    UrlValue homeserverValue = extractUrl(wellKnown, "m.homeserver", normalizedBase);
    if (homeserverValue.failure() != null) {
      return homeserverValue.failure();
    }
    UrlValue identityValue = extractUrl(wellKnown, "m.identity_server", homeserverValue.url());
    if (identityValue.failure() != null) {
      return identityValue.failure();
    }
    return new DiscoveredHomeserver(
        homeserverValue.url(),
        identityValue.url(),
        wellKnown,
        false,
        DiscoveryOutcome.DISCOVERED,
        null);
  }

  private static DiscoveredHomeserver responseOutcome(
      String fallbackUrl, HttpTransport.Response response) {
    if (response.statusCode() == 404) {
      return new DiscoveredHomeserver(
          fallbackUrl, null, null, true, DiscoveryOutcome.IGNORE, "Well-known endpoint not found");
    }
    if (response.statusCode() != 200) {
      return failed(
          fallbackUrl,
          null,
          DiscoveryOutcome.FAIL_PROMPT,
          "Well-known endpoint returned HTTP " + response.statusCode());
    }
    if (response.body() == null || response.body().isBlank()) {
      return failed(fallbackUrl, null, DiscoveryOutcome.FAIL_PROMPT, "Well-known body is empty");
    }
    return null;
  }

  private static JsonValue parseWellKnown(String fallbackUrl, String body) {
    try {
      return JsonParser.parse(body);
    } catch (IllegalArgumentException _) {
      return null;
    }
  }

  private static UrlValue extractUrl(JsonValue wellKnown, String key, String fallbackUrl) {
    JsonValue section = wellKnown.asObject().get(key);
    if (section == null && "m.identity_server".equals(key)) {
      return new UrlValue(null, null);
    }
    if (section == null || !section.isObject()) {
      return new UrlValue(
          null,
          failed(
              fallbackUrl,
              wellKnown,
              DiscoveryOutcome.FAIL_PROMPT,
              key + " is missing or invalid"));
    }
    JsonValue baseUrlValue = section.asObject().get("base_url");
    if (baseUrlValue == null || !baseUrlValue.isString() || baseUrlValue.asString().isBlank()) {
      return new UrlValue(
          null,
          failed(
              fallbackUrl,
              wellKnown,
              DiscoveryOutcome.FAIL_PROMPT,
              key + ".base_url is missing or invalid"));
    }
    try {
      return new UrlValue(normalize(baseUrlValue.asString().strip()), null);
    } catch (IllegalArgumentException _) {
      return new UrlValue(
          null,
          failed(
              fallbackUrl,
              wellKnown,
              DiscoveryOutcome.FAIL_ERROR,
              key + ".base_url is not a valid URL"));
    }
  }

  private static DiscoveredHomeserver failed(
      String fallbackUrl, JsonValue wellKnown, DiscoveryOutcome outcome, String reason) {
    return new DiscoveredHomeserver(fallbackUrl, null, wellKnown, false, outcome, reason);
  }

  private record UrlValue(String url, DiscoveredHomeserver failure) {}

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
