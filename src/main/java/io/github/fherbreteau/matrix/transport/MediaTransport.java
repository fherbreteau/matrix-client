package io.github.fherbreteau.matrix.transport;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.OptionalLong;

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
   * Sends a binary request and enforces the response size limit.
   *
   * @param request the binary request
   * @param maxResponseBytes maximum permitted response size, or zero for no limit
   * @return the binary response
   * @throws MediaSizeLimitException if the response exceeds the configured limit
   */
  default BinaryResponse send(BinaryRequest request, long maxResponseBytes) {
    BinaryResponse response = send(request);
    if (maxResponseBytes <= 0) {
      return response;
    }
    if (response.contentLength() > maxResponseBytes) {
      try {
        response.close();
      } catch (IOException e) {
        throw new UncheckedIOException(e);
      }
      throw new MediaSizeLimitException(maxResponseBytes);
    }
    return new BinaryResponse(
        response.statusCode(),
        response.headers(),
        limited(response.bodyStream(), maxResponseBytes),
        response.contentLength(),
        response.retryAfterMs());
  }

  /**
   * Sends a binary request with bounded upload and response sizes.
   *
   * @param request the binary request
   * @param maxUploadBytes maximum permitted upload size, or zero for no limit
   * @param maxResponseBytes maximum permitted response size, or zero for no limit
   * @return the binary response
   * @throws MediaSizeLimitException if either transfer exceeds its configured limit
   */
  default BinaryResponse send(
      StreamingBinaryRequest request, long maxUploadBytes, long maxResponseBytes) {
    try {
      byte[] content = readLimited(request.body(), maxUploadBytes);
      if (request.contentLength().isPresent()
          && request.contentLength().getAsLong() != content.length) {
        throw new IOException("Media source length did not match its declared content length");
      }
      return send(
          new BinaryRequest(
              request.method(), request.url(), request.headers(), content, request.contentType()),
          maxResponseBytes);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private static InputStream limited(InputStream input, long maximumBytes) {
    return new InputStream() {
      private long transferred;

      @Override
      public int read() throws IOException {
        if (transferred >= maximumBytes) {
          return verifyEnd(input);
        }
        int value = input.read();
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
        if (transferred >= maximumBytes) {
          return verifyEnd(input);
        }
        int count = input.read(bytes, offset, (int) Math.min(length, maximumBytes - transferred));
        if (count > 0) {
          transferred += count;
        }
        return count;
      }

      @Override
      public void close() throws IOException {
        input.close();
      }

      private int verifyEnd(InputStream stream) throws IOException {
        int extra = stream.read();
        if (extra != -1) {
          stream.close();
          throw new MediaSizeLimitException(maximumBytes);
        }
        return -1;
      }
    };
  }

  private static byte[] readLimited(InputStream input, long maximumBytes) throws IOException {
    try (input;
        var output = new ByteArrayOutputStream()) {
      byte[] buffer = new byte[8192];
      long total = 0;
      int count;
      while ((count = input.read(buffer)) != -1) {
        total += count;
        if (maximumBytes > 0 && total > maximumBytes) {
          throw new MediaSizeLimitException(maximumBytes);
        }
        output.write(buffer, 0, count);
      }
      return output.toByteArray();
    }
  }

  /**
   * A streaming binary HTTP request.
   *
   * @param method the HTTP method
   * @param url the target URL
   * @param headers additional request headers
   * @param body media source
   * @param contentLength known length, or empty for unknown length
   * @param contentType media MIME type
   */
  record StreamingBinaryRequest(
      String method,
      String url,
      Map<String, String> headers,
      InputStream body,
      OptionalLong contentLength,
      String contentType) {

    public StreamingBinaryRequest {
      headers = headers == null ? Map.of() : Map.copyOf(headers);
      Objects.requireNonNull(body, "body");
      contentLength = contentLength == null ? OptionalLong.empty() : contentLength;
      Objects.requireNonNull(contentType, "contentType");
    }
  }

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
      var sb =
          new StringBuilder(method)
              .append(' ')
              .append(UrlRedaction.redactQueryAndFragment(url))
              .append(" contentType=");
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
   * Returns the default media transport backed by the JDK HTTP client.
   *
   * @return the default media transport
   * @see <a href="https://spec.matrix.org/latest/client-server-api/#content-repository">Matrix
   *     specification</a>
   */
  static MediaTransport create() {
    return new JdkMediaTransport();
  }
}
