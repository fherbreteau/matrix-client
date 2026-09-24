package io.github.fherbreteau.matrix.transport;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * A binary request/response exchange for media transfers: uploads stream a byte source with an
 * explicit content type, downloads return the body as an {@link InputStream} so large media is
 * never fully buffered in memory. The default implementation relies only on {@code
 * java.net.http.HttpClient}.
 *
 * @see <a href="https://spec.matrix.org/latest/client-server-api/#content-repository">Matrix
 *     specification</a>
 */
public interface MediaTransport {

  /**
   * Sends a binary request with the given content type and returns the raw response.
   * Implementations must never log or expose secrets (access tokens, credentials).
   *
   * @param request the binary request to send
   * @return the binary response returned by the remote server
   * @see <a href="https://spec.matrix.org/latest/client-server-api/#content-repository">Matrix
   *     specification</a>
   */
  BinaryResponse send(BinaryRequest request);

  /**
   * A binary HTTP request: raw body bytes with an explicit content type. The {@code toString()}
   * representation never includes the body.
   *
   * @see <a href="https://spec.matrix.org/latest/client-server-api/#content-repository">Matrix
   *     specification</a>
   */
  record BinaryRequest(
      String method, String url, Map<String, String> headers, byte[] body, String contentType) {

    /**
     * Name of the HTTP header carrying the media content type.
     *
     * @see <a
     *     href="https://spec.matrix.org/latest/client-server-api/#post_matrixmediav3upload">Matrix
     *     specification</a>
     */
    public static final String CONTENT_TYPE_HEADER = "Content-Type";

    /**
     * Creates a binary media request. The body bytes are defensively copied.
     *
     * @param method the HTTP method
     * @param url the target URL
     * @param headers additional request headers
     * @param body raw media bytes
     * @param contentType the media MIME type
     * @see <a
     *     href="https://spec.matrix.org/latest/client-server-api/#post_matrixmediav3upload">Matrix
     *     specification</a>
     */
    public BinaryRequest {
      headers = headers == null ? Map.of() : Map.copyOf(headers);
      body = body == null ? new byte[0] : body.clone();
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if (!(obj instanceof BinaryRequest other)) {
        return false;
      }
      return method.equals(other.method)
          && url.equals(other.url)
          && headers.equals(other.headers)
          && Arrays.equals(body, other.body)
          && contentType.equals(other.contentType);
    }

    @Override
    public int hashCode() {
      return Objects.hash(method, url, headers, Arrays.hashCode(body), contentType);
    }

    @Override
    public String toString() {
      var sb = new StringBuilder(method).append(' ').append(url).append(" contentType=");
      sb.append(contentType);
      if (!headers.isEmpty()) {
        sb.append(" headers={");
        var first = true;
        for (Map.Entry<String, String> header : headers.entrySet()) {
          if (!first) {
            sb.append(", ");
          }
          first = false;
          sb.append(header.getKey()).append(':');
          if (HttpTransport.Request.AUTHORIZATION_HEADER.equalsIgnoreCase(header.getKey())) {
            sb.append("***");
          } else {
            sb.append('\'').append(header.getValue()).append('\'');
          }
        }
        sb.append('}');
      }
      return sb.append(" body=").append(body.length).append(" bytes").toString();
    }
  }

  /**
   * A binary HTTP response: the raw body as an {@link InputStream} so callers can stream large
   * media instead of buffering it in memory. Callers must close the stream.
   *
   * @see <a href="https://spec.matrix.org/latest/client-server-api/#content-repository">Matrix
   *     specification</a>
   */
  final class BinaryResponse {

    private final int statusCode;
    private final Map<String, String> headers;
    private final byte[] body;
    private final Long retryAfterMs;

    /**
     * Creates a binary response containing the media transfer result.
     *
     * @param statusCode the HTTP response status
     * @param headers the response headers
     * @param body the downloaded media bytes
     * @param retryAfterMs the optional parsed rate-limit delay, in milliseconds
     * @see <a href="https://spec.matrix.org/latest/client-server-api/#downloading-content">Matrix
     *     specification</a>
     */
    public BinaryResponse(
        int statusCode, Map<String, String> headers, byte[] body, Long retryAfterMs) {
      this.statusCode = statusCode;
      var normalized = new HashMap<String, String>();
      if (headers != null) {
        for (Map.Entry<String, String> header : headers.entrySet()) {
          normalized.put(header.getKey().toLowerCase(Locale.ROOT), header.getValue());
        }
      }
      this.headers = Map.copyOf(normalized);
      this.body = body == null ? new byte[0] : body;
      this.retryAfterMs = retryAfterMs;
    }

    public int statusCode() {
      return statusCode;
    }

    public Map<String, String> headers() {
      return headers;
    }

    public String header(String name) {
      return headers.get(name.toLowerCase(Locale.ROOT));
    }

    public Long retryAfterMs() {
      return retryAfterMs;
    }

    /**
     * Returns the body size in bytes.
     *
     * @return the body size in bytes
     * @see <a href="https://spec.matrix.org/latest/client-server-api/#downloading-content">Matrix
     *     specification</a>
     */
    public long contentLength() {
      return body.length;
    }

    /**
     * Opens the body as a stream; the underlying bytes are already buffered by the HTTP client, so
     * the stream never blocks on the network.
     *
     * @return the body stream; must be closed by the caller
     * @see <a href="https://spec.matrix.org/latest/client-server-api/#downloading-content">Matrix
     *     specification</a>
     */
    public InputStream bodyStream() {
      return new ByteArrayInputStream(body);
    }
  }

  /**
   * Returns a media transport backed by {@code java.net.http.HttpClient}.
   *
   * @return a media transport using the default HTTP client
   */
  static MediaTransport create() {
    return new JdkMediaTransport();
  }
}
