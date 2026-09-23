package io.github.fherbreteau.matrix.transport;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * A binary request/response exchange for media transfers: uploads stream a byte source with an
 * explicit content type, downloads return the body as an {@link InputStream} so large media is
 * never fully buffered in memory. The default implementation relies only on {@code
 * java.net.http.HttpClient}.
 */
public interface MediaTransport {

  /**
   * Sends a binary request with the given content type and returns the raw response.
   * Implementations must never log or expose secrets (access tokens, credentials).
   *
   * @param request the binary request to send
   * @return the binary response returned by the remote server
   */
  BinaryResponse send(BinaryRequest request);

  /**
   * A binary HTTP request: raw body bytes with an explicit content type. The {@code toString()}
   * representation never includes the body.
   */
  final class BinaryRequest {

    private final String method;
    private final String url;
    private final Map<String, String> headers;
    private final byte[] body;
    private final String contentType;

    /**
     * Creates a binary request.
     *
     * @param method the HTTP method
     * @param url the target URL
     * @param headers the extra headers
     * @param body the raw body bytes
     * @param contentType the MIME type of the body
     */
    public BinaryRequest(
        String method, String url, Map<String, String> headers, byte[] body, String contentType) {
      this.method = method;
      this.url = url;
      this.headers = headers == null ? Map.of() : Map.copyOf(headers);
      this.body = body == null ? new byte[0] : body.clone();
      this.contentType = contentType;
    }

    public String method() {
      return method;
    }

    public String url() {
      return url;
    }

    public Map<String, String> headers() {
      return headers;
    }

    public byte[] body() {
      return body.clone();
    }

    public String contentType() {
      return contentType;
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
          && java.util.Arrays.equals(body, other.body)
          && contentType.equals(other.contentType);
    }

    @Override
    public int hashCode() {
      return java.util.Objects.hash(
          method, url, headers, java.util.Arrays.hashCode(body), contentType);
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
   */
  final class BinaryResponse {

    private final int statusCode;
    private final Map<String, String> headers;
    private final byte[] body;
    private final Long retryAfterMs;

    public BinaryResponse(
        int statusCode, Map<String, String> headers, byte[] body, Long retryAfterMs) {
      this.statusCode = statusCode;
      var normalized = new HashMap<String, String>();
      if (headers != null) {
        for (Map.Entry<String, String> header : headers.entrySet()) {
          normalized.put(header.getKey().toLowerCase(java.util.Locale.ROOT), header.getValue());
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
     */
    public long contentLength() {
      return body.length;
    }

    /**
     * Opens the body as a stream; the underlying bytes are already buffered by the HTTP client, so
     * the stream never blocks on the network.
     *
     * @return the body stream; must be closed by the caller
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
