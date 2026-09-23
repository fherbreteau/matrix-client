package io.github.fherbreteau.matrix.transport;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Default {@link MediaTransport} built on the JDK's {@code java.net.http.HttpClient}. The response
 * body is fully read into memory but exposed as an {@link java.io.InputStream}, so callers stream
 * it without re-buffering; the maximum in-memory size is bounded by {@link HttpTransportConfig}.
 */
public final class JdkMediaTransport implements MediaTransport {

  private final HttpClient client;
  private final HttpTransportConfig config;

  /** Creates a media transport with a default {@code HttpClient} and configuration. */
  public JdkMediaTransport() {
    this(HttpClient.newHttpClient(), HttpTransportConfig.builder().build());
  }

  /**
   * Creates a media transport with a {@code HttpClient} built from the given configuration.
   *
   * @param config the transport configuration
   */
  public JdkMediaTransport(HttpTransportConfig config) {
    this(newClient(config), config);
  }

  /**
   * Creates a media transport over the given {@code HttpClient} with the given configuration.
   *
   * @param client the HTTP client used to transfer media
   * @param config the transport configuration
   */
  public JdkMediaTransport(HttpClient client, HttpTransportConfig config) {
    this.client = client;
    this.config = config;
  }

  private static HttpClient newClient(HttpTransportConfig config) {
    var builder = HttpClient.newBuilder();
    if (config.connectTimeout() != null) {
      builder.connectTimeout(config.connectTimeout());
    }
    if (config.followRedirects()) {
      builder.followRedirects(HttpClient.Redirect.NORMAL);
    }
    if (config.proxy() != null) {
      builder.proxy(config.proxy());
    }
    return builder.build();
  }

  @Override
  public BinaryResponse send(BinaryRequest request) {
    var builder = HttpRequest.newBuilder(URI.create(request.url()));
    if (config.requestTimeout() != null) {
      builder.timeout(config.requestTimeout());
    }
    builder.header("Content-Type", request.contentType());
    if (config.accessToken() != null) {
      builder.header(HttpTransport.Request.AUTHORIZATION_HEADER, "Bearer " + config.accessToken());
    }
    for (Map.Entry<String, String> header : request.headers().entrySet()) {
      builder.header(header.getKey(), header.getValue());
    }
    builder.method(
        request.method(),
        request.body().length == 0
            ? HttpRequest.BodyPublishers.noBody()
            : HttpRequest.BodyPublishers.ofByteArray(request.body()));
    try {
      Duration requestTimeout = config.requestTimeout();
      HttpResponse<byte[]> response =
          client.send(builder.build(), HttpResponse.BodyHandlers.ofByteArray());
      Long retryAfterMs =
          response
              .headers()
              .firstValue("Retry-After")
              .map(JdkMediaTransport::parseRetryAfter)
              .orElse(null);
      return new BinaryResponse(
          response.statusCode(), lowerCaseHeaders(response), response.body(), retryAfterMs);
    } catch (HttpTimeoutException e) {
      throw new TransportTimeoutException(
          "HTTP request timed out: " + request.method() + " " + request.url(), e);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new TransportInterruptedException("HTTP request interrupted", e);
    } catch (IOException e) {
      throw new UncheckedTransportException(
          "HTTP request failed: " + request.method() + " " + request.url(), e);
    }
  }

  private static Long parseRetryAfter(String value) {
    try {
      return Duration.ofSeconds(Long.parseLong(value.strip())).toMillis();
    } catch (NumberFormatException _) {
      return null;
    }
  }

  private static Map<String, String> lowerCaseHeaders(HttpResponse<byte[]> response) {
    var headers = new HashMap<String, String>();
    for (Map.Entry<String, java.util.List<String>> header : response.headers().map().entrySet()) {
      if (!header.getValue().isEmpty()) {
        headers.put(
            header.getKey().toLowerCase(java.util.Locale.ROOT), header.getValue().getFirst());
      }
    }
    return headers;
  }
}
