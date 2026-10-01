package io.github.fherbreteau.matrix.transport;

import java.net.ProxySelector;
import java.time.Duration;

/**
 * Configuration for {@link JdkHttpTransport}: base transport behavior such as timeouts, redirect
 * handling, proxy selection and access-token authentication. Use the {@link Builder} via {@link
 * JdkHttpTransport#config()}.
 */
public final class HttpTransportConfig {

  private final Duration connectTimeout;
  private final Duration requestTimeout;
  private final boolean followRedirects;
  private final ProxySelector proxy;
  private final String accessToken;
  private final long maxMediaUploadBytes;

  private HttpTransportConfig(Builder builder) {
    this.connectTimeout = builder.connectTimeout;
    this.requestTimeout = builder.requestTimeout;
    this.followRedirects = builder.followRedirects;
    this.proxy = builder.proxy;
    this.accessToken = builder.accessToken;
    this.maxMediaUploadBytes = builder.maxMediaUploadBytes;
  }

  /**
   * Returns a new {@link Builder}.
   *
   * @return a new builder
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Returns the configured connect timeout, or {@code null} when unset.
   *
   * @return the configured connect timeout, or {@code null} when unset
   */
  public Duration connectTimeout() {
    return connectTimeout;
  }

  /**
   * Returns the configured request timeout, or {@code null} when unset.
   *
   * @return the configured request timeout, or {@code null} when unset
   */
  public Duration requestTimeout() {
    return requestTimeout;
  }

  /**
   * Returns whether HTTP redirects are followed automatically.
   *
   * @return whether HTTP redirects are followed automatically
   */
  public boolean followRedirects() {
    return followRedirects;
  }

  /**
   * Returns the configured proxy selector, or {@code null} when unset.
   *
   * @return the configured proxy selector, or {@code null} when unset
   */
  public ProxySelector proxy() {
    return proxy;
  }

  /**
   * Returns the configured access token, or {@code null} when unset.
   *
   * @return the configured access token, or {@code null} when unset
   */
  public String accessToken() {
    return accessToken;
  }

  /**
   * Returns the maximum media upload size, or zero when no client-side limit is configured.
   *
   * @return the maximum media upload size in bytes, or zero for no limit
   */
  public long maxMediaUploadBytes() {
    return maxMediaUploadBytes;
  }

  /** Builder for {@link HttpTransportConfig}. */
  public static final class Builder {

    private Duration connectTimeout;
    private Duration requestTimeout;
    private boolean followRedirects = true;
    private ProxySelector proxy;
    private String accessToken;
    private long maxMediaUploadBytes;

    private Builder() {}

    /**
     * Maximum time to establish the TCP connection.
     *
     * @param connectTimeout maximum time to establish the TCP connection
     * @return this builder for chaining
     */
    public Builder connectTimeout(Duration connectTimeout) {
      this.connectTimeout = connectTimeout;
      return this;
    }

    /**
     * Maximum time for a whole request/response exchange.
     *
     * @param requestTimeout maximum time for a whole request/response exchange
     * @return this builder for chaining
     */
    public Builder requestTimeout(Duration requestTimeout) {
      this.requestTimeout = requestTimeout;
      return this;
    }

    /**
     * Whether HTTP redirects are followed automatically (default: true).
     *
     * @param followRedirects whether redirects are followed automatically
     * @return this builder for chaining
     */
    public Builder followRedirects(boolean followRedirects) {
      this.followRedirects = followRedirects;
      return this;
    }

    /**
     * Proxy selector used to reach the homeserver.
     *
     * @param proxy the proxy selector used to reach the homeserver
     * @return this builder for chaining
     */
    public Builder proxy(ProxySelector proxy) {
      this.proxy = proxy;
      return this;
    }

    /**
     * Access token sent as a {@code Bearer} {@code Authorization} header on authenticated requests.
     * The token is never logged.
     *
     * @param accessToken the access token sent as a Bearer authorization header
     * @return this builder for chaining
     */
    public Builder accessToken(String accessToken) {
      this.accessToken = accessToken;
      return this;
    }

    /**
     * Sets the maximum number of bytes accepted for media uploads; zero disables the limit.
     *
     * @param maxMediaUploadBytes the maximum media upload size in bytes, or zero for no limit
     * @return this builder for chaining
     */
    public Builder maxMediaUploadBytes(long maxMediaUploadBytes) {
      if (maxMediaUploadBytes < 0) {
        throw new IllegalArgumentException("maxMediaUploadBytes must not be negative");
      }
      this.maxMediaUploadBytes = maxMediaUploadBytes;
      return this;
    }

    /**
     * Builds the configuration.
     *
     * @return the built configuration
     */
    public HttpTransportConfig build() {
      return new HttpTransportConfig(this);
    }
  }
}
