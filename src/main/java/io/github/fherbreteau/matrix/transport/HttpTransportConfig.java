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

  private HttpTransportConfig(Builder builder) {
    this.connectTimeout = builder.connectTimeout;
    this.requestTimeout = builder.requestTimeout;
    this.followRedirects = builder.followRedirects;
    this.proxy = builder.proxy;
    this.accessToken = builder.accessToken;
  }

  /** Returns a new {@link Builder}. */
  public static Builder builder() {
    return new Builder();
  }

  /** Returns the configured connect timeout, or {@code null} when unset. */
  public Duration connectTimeout() {
    return connectTimeout;
  }

  /** Returns the configured request timeout, or {@code null} when unset. */
  public Duration requestTimeout() {
    return requestTimeout;
  }

  /** Returns whether HTTP redirects are followed automatically. */
  public boolean followRedirects() {
    return followRedirects;
  }

  /** Returns the configured proxy selector, or {@code null} when unset. */
  public ProxySelector proxy() {
    return proxy;
  }

  /** Returns the configured access token, or {@code null} when unset. */
  public String accessToken() {
    return accessToken;
  }

  /** Builder for {@link HttpTransportConfig}. */
  public static final class Builder {

    private Duration connectTimeout;
    private Duration requestTimeout;
    private boolean followRedirects = true;
    private ProxySelector proxy;
    private String accessToken;

    private Builder() {}

    /** Maximum time to establish the TCP connection. */
    public Builder connectTimeout(Duration connectTimeout) {
      this.connectTimeout = connectTimeout;
      return this;
    }

    /** Maximum time for a whole request/response exchange. */
    public Builder requestTimeout(Duration requestTimeout) {
      this.requestTimeout = requestTimeout;
      return this;
    }

    /** Whether HTTP redirects are followed automatically (default: true). */
    public Builder followRedirects(boolean followRedirects) {
      this.followRedirects = followRedirects;
      return this;
    }

    /** Proxy selector used to reach the homeserver. */
    public Builder proxy(ProxySelector proxy) {
      this.proxy = proxy;
      return this;
    }

    /**
     * Access token sent as a {@code Bearer} {@code Authorization} header on authenticated requests.
     * The token is never logged.
     */
    public Builder accessToken(String accessToken) {
      this.accessToken = accessToken;
      return this;
    }

    /** Builds the configuration. */
    public HttpTransportConfig build() {
      return new HttpTransportConfig(this);
    }
  }
}
