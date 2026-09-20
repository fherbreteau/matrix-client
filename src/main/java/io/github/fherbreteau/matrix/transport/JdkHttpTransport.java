package io.github.fherbreteau.matrix.transport;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Default {@link HttpTransport} built on the JDK's {@code java.net.http.HttpClient}. Supports GET,
 * POST, PUT, DELETE and authenticated requests, configurable timeouts, redirects and proxy
 * behavior, and never logs access tokens.
 */
public final class JdkHttpTransport implements HttpTransport {

  private final HttpClient client;
  private final HttpTransportConfig config;

  /** Creates a transport with a default {@code HttpClient} and configuration. */
  public JdkHttpTransport() {
    this(HttpClient.newHttpClient(), HttpTransportConfig.builder().build());
  }

  /** Creates a transport over the given {@code HttpClient} with default configuration. */
  public JdkHttpTransport(HttpClient client) {
    this(client, HttpTransportConfig.builder().build());
  }

  /** Creates a transport with a {@code HttpClient} built from the given configuration. */
  public JdkHttpTransport(HttpTransportConfig config) {
    this(newClient(config), config);
  }

  private JdkHttpTransport(HttpClient client, HttpTransportConfig config) {
    this.client = client;
    this.config = config;
  }

  /**
   * Returns a builder producing a {@link HttpTransportConfig}; use it to create a transport with
   * {@code new JdkHttpTransport(config.build())}.
   */
  public static HttpTransportConfig.Builder config() {
    return HttpTransportConfig.builder();
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
  public Response send(Request request) {
    var builder = HttpRequest.newBuilder(URI.create(request.url()));
    if (config.requestTimeout() != null) {
      builder.timeout(config.requestTimeout());
    }
    builder.header("Accept", "application/json");
    if (config.accessToken() != null) {
      builder.header(Request.AUTHORIZATION_HEADER, "Bearer " + config.accessToken());
    }
    if (request.body() != null) {
      builder.header("Content-Type", "application/json");
      builder.method(
          request.method(),
          HttpRequest.BodyPublishers.ofString(request.body(), StandardCharsets.UTF_8));
    } else {
      builder.method(request.method(), HttpRequest.BodyPublishers.noBody());
    }
    for (Map.Entry<String, String> header : request.headers().entrySet()) {
      builder.header(header.getKey(), header.getValue());
    }
    try {
      HttpResponse<String> response =
          client.send(builder.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
      return new Response(
          response.statusCode(),
          lowerCaseHeaders(response),
          response.body(),
          parseRetryAfter(response.headers().firstValue("Retry-After").orElse(null)));
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

  private static Map<String, String> lowerCaseHeaders(HttpResponse<String> response) {
    var headers = new HashMap<String, String>();
    for (Map.Entry<String, List<String>> header : response.headers().map().entrySet()) {
      if (!header.getValue().isEmpty()) {
        headers.put(header.getKey().toLowerCase(Locale.ROOT), header.getValue().getFirst());
      }
    }
    return headers;
  }

  private static Long parseRetryAfter(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    try {
      return Duration.ofSeconds(Long.parseLong(value.strip())).toMillis();
    } catch (NumberFormatException _) {
      return null;
    }
  }
}
