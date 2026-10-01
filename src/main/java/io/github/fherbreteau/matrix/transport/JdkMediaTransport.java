package io.github.fherbreteau.matrix.transport;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.OptionalLong;

/**
 * Default {@link MediaTransport} built on the JDK's {@code java.net.http.HttpClient}. Request and
 * response bodies stream without being fully buffered. Configured limits are enforced while bytes
 * are transferred.
 *
 * @see <a href="https://spec.matrix.org/latest/client-server-api/#content-repository">Matrix
 *     specification</a>
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

  /**
   * Returns the configured media upload size limit.
   *
   * @return the maximum upload size, or zero if unlimited
   */
  public long maxUploadBytes() {
    return config.maxMediaUploadBytes();
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
    return send(
        new StreamingBinaryRequest(
            request.method(),
            request.url(),
            request.headers(),
            new java.io.ByteArrayInputStream(request.body()),
            OptionalLong.of(request.body().length),
            request.contentType()),
        0,
        0);
  }

  @Override
  public BinaryResponse send(BinaryRequest request, long maxResponseBytes) {
    return send(
        new StreamingBinaryRequest(
            request.method(),
            request.url(),
            request.headers(),
            new java.io.ByteArrayInputStream(request.body()),
            OptionalLong.of(request.body().length),
            request.contentType()),
        0,
        maxResponseBytes);
  }

  @Override
  public BinaryResponse send(
      StreamingBinaryRequest request, long maxUploadBytes, long maxResponseBytes) {
    OptionalLong contentLength = request.contentLength();
    long uploadLimit = maxUploadBytes > 0 ? maxUploadBytes : config.maxMediaUploadBytes();
    if (contentLength.isPresent() && uploadLimit > 0 && contentLength.getAsLong() > uploadLimit) {
      closeQuietly(request.body());
      throw new MediaSizeLimitException(uploadLimit);
    }
    HttpRequest.BodyPublisher publisher =
        HttpRequest.BodyPublishers.ofInputStream(
            () -> limitedUploadStream(request.body(), uploadLimit));
    if (contentLength.isPresent() && contentLength.getAsLong() > 0) {
      publisher = HttpRequest.BodyPublishers.fromPublisher(publisher, contentLength.getAsLong());
    }
    return send(
        request.method(),
        request.url(),
        request.headers(),
        request.contentType(),
        publisher,
        maxResponseBytes);
  }

  private BinaryResponse send(
      String method,
      String url,
      Map<String, String> headers,
      String contentType,
      HttpRequest.BodyPublisher publisher,
      long maxResponseBytes) {
    var builder = HttpRequest.newBuilder(URI.create(url));
    if (config.requestTimeout() != null) {
      builder.timeout(config.requestTimeout());
    }
    builder.header("Content-Type", contentType);
    if (config.accessToken() != null) {
      builder.header(HttpTransport.Request.AUTHORIZATION_HEADER, "Bearer " + config.accessToken());
    }
    headers.forEach(builder::header);
    builder.method(method, publisher);
    try {
      HttpResponse<?> response =
          client.send(builder.build(), HttpResponse.BodyHandlers.ofInputStream());
      InputStream body = (InputStream) response.body();
      long contentLength =
          parseContentLength(response.headers().firstValue("Content-Length").orElse(null));
      if (maxResponseBytes > 0 && contentLength > maxResponseBytes) {
        closeQuietly(body);
        throw new MediaSizeLimitException(maxResponseBytes);
      }
      if (maxResponseBytes > 0) {
        body = limitedResponseStream(body, maxResponseBytes);
      }
      Long retryAfterMs =
          response.headers().firstValue("Retry-After").map(RetryAfterParser::parse).orElse(null);
      return new BinaryResponse(
          response.statusCode(), lowerCaseHeaders(response), body, contentLength, retryAfterMs);
    } catch (HttpTimeoutException e) {
      throw new TransportTimeoutException(
          "HTTP request timed out: " + method + " " + UrlRedaction.redactQueryAndFragment(url), e);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new TransportInterruptedException("HTTP request interrupted", e);
    } catch (IOException e) {
      throw new UncheckedTransportException(
          "HTTP request failed: " + method + " " + UrlRedaction.redactQueryAndFragment(url), e);
    }
  }

  static InputStream limitedUploadStream(InputStream input, long maximumBytes) {
    return limitedStream(input, maximumBytes);
  }

  private static InputStream limitedResponseStream(InputStream input, long maximumBytes) {
    return limitedStream(input, maximumBytes);
  }

  private static InputStream limitedStream(InputStream input, long maximumBytes) {
    return new FilterInputStream(input) {
      private long transferred;

      @Override
      public int read() throws IOException {
        if (atLimit()) {
          verifyEndOfStream();
        }
        int value = super.read();
        if (value != -1) {
          transferred++;
        }
        return value;
      }

      @Override
      public int read(byte[] bytes, int offset, int length) throws IOException {
        if (length == 0) {
          return 0;
        }
        if (atLimit()) {
          verifyEndOfStream();
        }
        int permittedLength =
            maximumBytes > 0 ? (int) Math.min(length, maximumBytes - transferred) : length;
        int count = super.read(bytes, offset, permittedLength);
        if (count > 0) {
          transferred += count;
        }
        return count;
      }

      private boolean atLimit() {
        return maximumBytes > 0 && transferred >= maximumBytes;
      }

      private void verifyEndOfStream() throws IOException {
        int extra = super.read();
        if (extra != -1) {
          closeQuietly(in);
          throw new MediaSizeLimitException(maximumBytes);
        }
      }
    };
  }

  private static long parseContentLength(String value) {
    if (value == null) {
      return -1;
    }
    try {
      return Long.parseLong(value);
    } catch (NumberFormatException _) {
      return -1;
    }
  }

  private static void closeQuietly(InputStream input) {
    try {
      input.close();
    } catch (IOException _) {
      // ignored exception
    }
  }

  private static Map<String, String> lowerCaseHeaders(HttpResponse<?> response) {
    var headers = new HashMap<String, String>();
    for (Map.Entry<String, List<String>> header : response.headers().map().entrySet()) {
      if (!header.getValue().isEmpty()) {
        headers.put(header.getKey().toLowerCase(Locale.ROOT), header.getValue().getFirst());
      }
    }
    return headers;
  }
}
