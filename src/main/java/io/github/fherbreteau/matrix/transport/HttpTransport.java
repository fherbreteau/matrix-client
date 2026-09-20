package io.github.fherbreteau.matrix.transport;

import java.util.Locale;
import java.util.Map;

/**
 * Abstraction over the HTTP layer used to talk to a homeserver. The default implementation relies
 * only on {@code java.net.http.HttpClient}.
 */
public interface HttpTransport {

  /**
   * Sends the request and returns the response. Implementations must never log or expose secrets
   * (access tokens, credentials).
   *
   * @param request the request to send
   * @return the response returned by the remote server
   */
  Response send(Request request);

  /**
   * An HTTP request. The {@code toString()} representation redacts the {@code Authorization} header
   * so access tokens never leak into logs.
   */
  record Request(String method, String url, Map<String, String> headers, String body) {

    /** Name of the HTTP header carrying access-token authentication. */
    public static final String AUTHORIZATION_HEADER = "Authorization";

    public Request {
      headers = headers == null ? Map.of() : Map.copyOf(headers);
    }

    @Override
    public String toString() {
      var sb = new StringBuilder(method).append(' ').append(url).append(" headers=");
      if (headers.isEmpty()) {
        sb.append("{}");
      } else {
        sb.append('{');
        var first = true;
        for (Map.Entry<String, String> header : headers.entrySet()) {
          if (!first) {
            sb.append(", ");
          }
          first = false;
          sb.append(header.getKey()).append(':');
          if (AUTHORIZATION_HEADER.equalsIgnoreCase(header.getKey())) {
            sb.append("***");
          } else {
            sb.append('\'').append(header.getValue()).append('\'');
          }
        }
        sb.append('}');
      }
      if (body != null) {
        sb.append(" body=").append(body.length()).append(" bytes");
      }
      return sb.toString();
    }
  }

  /**
   * An HTTP response with status code, response headers and decoded body. The {@code retryAfterMs}
   * field carries a parsed {@code Retry-After} header (in milliseconds) when present, for
   * rate-limit handling.
   */
  record Response(int statusCode, Map<String, String> headers, String body, Long retryAfterMs) {

    public Response(int statusCode, String body) {
      this(statusCode, Map.of(), body, null);
    }

    public Response {
      headers = headers == null ? Map.of() : Map.copyOf(headers);
    }

    public String header(String name) {
      return headers.get(name.toLowerCase(Locale.ROOT));
    }

    @Override
    public String toString() {
      return "Response[statusCode="
          + statusCode
          + (retryAfterMs != null ? ", retryAfterMs=" + retryAfterMs : "")
          + ", body="
          + (body == null ? 0 : body.length())
          + " bytes]";
    }
  }

  /**
   * Returns the default transport backed by {@code java.net.http.HttpClient}.
   *
   * @return the default transport backed by {@code java.net.http.HttpClient}
   */
  static HttpTransport create() {
    return new JdkHttpTransport();
  }
}
