package io.github.fherbreteau.matrix.transport;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/** A streaming binary HTTP response. Callers must close the response body. */
public class BinaryResponse implements AutoCloseable {

  private final int statusCode;
  private final Map<String, String> headers;
  private final InputStream body;
  private final long contentLength;
  private final Long retryAfterMs;

  /**
   * Creates a response from a byte array.
   *
   * @param statusCode the HTTP status
   * @param headers response headers
   * @param body response bytes
   * @param retryAfterMs optional rate-limit delay
   */
  public BinaryResponse(
      int statusCode, Map<String, String> headers, byte[] body, Long retryAfterMs) {
    this(
        statusCode,
        headers,
        new ByteArrayInputStream(body == null ? new byte[0] : body),
        body == null ? 0 : body.length,
        retryAfterMs);
  }

  /**
   * Creates a response over a streaming body.
   *
   * @param statusCode the HTTP status
   * @param headers response headers
   * @param body streaming response bytes
   * @param contentLength known length, or a negative value when unknown
   * @param retryAfterMs optional rate-limit delay
   */
  public BinaryResponse(
      int statusCode,
      Map<String, String> headers,
      InputStream body,
      long contentLength,
      Long retryAfterMs) {
    this.statusCode = statusCode;
    var normalized = new HashMap<String, String>();
    if (headers != null) {
      for (Map.Entry<String, String> header : headers.entrySet()) {
        normalized.put(header.getKey().toLowerCase(Locale.ROOT), header.getValue());
      }
    }
    this.headers = Map.copyOf(normalized);
    this.body = Objects.requireNonNull(body, "body");
    this.contentLength = contentLength;
    this.retryAfterMs = retryAfterMs;
  }

  /**
   * Returns the HTTP status.
   *
   * @return the HTTP status
   */
  public int statusCode() {
    return statusCode;
  }

  /**
   * Returns response headers.
   *
   * @return response headers
   */
  public Map<String, String> headers() {
    return headers;
  }

  /**
   * Returns a case-insensitive response header.
   *
   * @param name the header name
   * @return the header value or {@code null}
   */
  public String header(String name) {
    return headers.get(name.toLowerCase(Locale.ROOT));
  }

  /**
   * Returns the optional rate-limit delay.
   *
   * @return the delay in milliseconds
   */
  public Long retryAfterMs() {
    return retryAfterMs;
  }

  /**
   * Returns the known response size.
   *
   * @return the size in bytes, or a negative value when unknown
   */
  public long contentLength() {
    return contentLength;
  }

  /** Closes the streaming response body. */
  @Override
  public void close() throws IOException {
    body.close();
  }

  /**
   * Returns the response body stream; callers must close it.
   *
   * @return the response body stream
   */
  public InputStream bodyStream() {
    return body;
  }
}
