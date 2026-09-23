package io.github.fherbreteau.matrix.transport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class JdkMediaTransportTest {

  @Test
  void sendsRawBytesAndParsesTheResponse() throws Exception {
    HttpClient client =
        new HttpClient() {
          @Override
          public java.util.Optional<java.net.CookieHandler> cookieHandler() {
            throw new UnsupportedOperationException();
          }

          @Override
          public java.util.Optional<Duration> connectTimeout() {
            throw new UnsupportedOperationException();
          }

          @Override
          public Redirect followRedirects() {
            throw new UnsupportedOperationException();
          }

          @Override
          public java.util.Optional<java.net.ProxySelector> proxy() {
            throw new UnsupportedOperationException();
          }

          @Override
          public javax.net.ssl.SSLContext sslContext() {
            throw new UnsupportedOperationException();
          }

          @Override
          public javax.net.ssl.SSLParameters sslParameters() {
            throw new UnsupportedOperationException();
          }

          @Override
          public java.util.Optional<java.net.Authenticator> authenticator() {
            throw new UnsupportedOperationException();
          }

          @Override
          public HttpClient.Version version() {
            throw new UnsupportedOperationException();
          }

          @Override
          public java.util.Optional<java.util.concurrent.Executor> executor() {
            throw new UnsupportedOperationException();
          }

          @Override
          public <T> HttpResponse<T> send(
              HttpRequest request, HttpResponse.BodyHandler<T> responseBodyHandler) {
            assertThat(request.headers().firstValue("Content-Type")).contains("image/png");
            assertThat(request.headers().firstValue("Authorization")).contains("Bearer tok");
            @SuppressWarnings("unchecked")
            HttpResponse<T> response = (HttpResponse<T>) new StubResponse(new byte[] {1, 2, 3});
            return response;
          }

          @Override
          public <T> java.util.concurrent.CompletableFuture<HttpResponse<T>> sendAsync(
              HttpRequest request, HttpResponse.BodyHandler<T> responseBodyHandler) {
            throw new UnsupportedOperationException();
          }

          @Override
          public <T> java.util.concurrent.CompletableFuture<HttpResponse<T>> sendAsync(
              HttpRequest request,
              HttpResponse.BodyHandler<T> responseBodyHandler,
              HttpResponse.PushPromiseHandler<T> pushPromiseHandler) {
            throw new UnsupportedOperationException();
          }
        };
    var config =
        HttpTransportConfig.builder()
            .accessToken("tok")
            .requestTimeout(Duration.ofSeconds(5))
            .build();
    var transport = new JdkMediaTransport(client, config);
    var request =
        new MediaTransport.BinaryRequest(
            "POST", "https://m/_upload", Map.of(), new byte[] {9, 9}, "image/png");
    var response = transport.send(request);
    assertThat(response.statusCode()).isEqualTo(200);
    assertThat(response.header("content-type")).isEqualTo("image/png");
    assertThat(response.contentLength()).isEqualTo(3);
    assertThat(response.bodyStream().readAllBytes()).isEqualTo(new byte[] {1, 2, 3});
  }

  @Test
  void parsesRetryAfterHeader() throws Exception {
    HttpClient client =
        new JdkMediaTransportTestClient() {
          @Override
          public <T> HttpResponse<T> send(
              HttpRequest request, HttpResponse.BodyHandler<T> responseBodyHandler) {
            @SuppressWarnings("unchecked")
            HttpResponse<T> response = (HttpResponse<T>) new StubRetryResponse();
            return response;
          }
        }.asClient();
    var transport = new JdkMediaTransport(client, HttpTransportConfig.builder().build());
    var response =
        transport.send(
            new MediaTransport.BinaryRequest("GET", "https://m/x", Map.of(), null, "a/b"));
    assertThat(response.retryAfterMs()).isEqualTo(7000L);
  }

  private abstract static class JdkMediaTransportTestClient extends HttpClient {
    @Override
    public java.util.Optional<java.net.CookieHandler> cookieHandler() {
      throw new UnsupportedOperationException();
    }

    @Override
    public java.util.Optional<Duration> connectTimeout() {
      throw new UnsupportedOperationException();
    }

    @Override
    public Redirect followRedirects() {
      throw new UnsupportedOperationException();
    }

    @Override
    public java.util.Optional<java.net.ProxySelector> proxy() {
      throw new UnsupportedOperationException();
    }

    @Override
    public javax.net.ssl.SSLContext sslContext() {
      throw new UnsupportedOperationException();
    }

    @Override
    public javax.net.ssl.SSLParameters sslParameters() {
      throw new UnsupportedOperationException();
    }

    @Override
    public java.util.Optional<java.net.Authenticator> authenticator() {
      throw new UnsupportedOperationException();
    }

    @Override
    public HttpClient.Version version() {
      throw new UnsupportedOperationException();
    }

    @Override
    public java.util.Optional<java.util.concurrent.Executor> executor() {
      throw new UnsupportedOperationException();
    }

    @Override
    public <T> java.util.concurrent.CompletableFuture<HttpResponse<T>> sendAsync(
        HttpRequest request, HttpResponse.BodyHandler<T> responseBodyHandler) {
      throw new UnsupportedOperationException();
    }

    @Override
    public <T> java.util.concurrent.CompletableFuture<HttpResponse<T>> sendAsync(
        HttpRequest request,
        HttpResponse.BodyHandler<T> responseBodyHandler,
        HttpResponse.PushPromiseHandler<T> pushPromiseHandler) {
      throw new UnsupportedOperationException();
    }

    java.net.http.HttpClient asClient() {
      return this;
    }
  }

  private static final class StubRetryResponse implements HttpResponse<byte[]> {
    @Override
    public int statusCode() {
      return 200;
    }

    @Override
    public HttpRequest request() {
      throw new UnsupportedOperationException();
    }

    @Override
    public Optional<HttpResponse<byte[]>> previousResponse() {
      throw new UnsupportedOperationException();
    }

    @Override
    public java.net.http.HttpHeaders headers() {
      return java.net.http.HttpHeaders.of(
          Map.of("Retry-After", java.util.List.of("7")), (name, value) -> true);
    }

    @Override
    public byte[] body() {
      return new byte[0];
    }

    @Override
    public URI uri() {
      throw new UnsupportedOperationException();
    }

    @Override
    public HttpClient.Version version() {
      throw new UnsupportedOperationException();
    }

    @Override
    public java.util.Optional<javax.net.ssl.SSLSession> sslSession() {
      throw new UnsupportedOperationException();
    }
  }

  @Test
  void rawBodyNeverAppearsInToString() {
    var request =
        new MediaTransport.BinaryRequest(
            "POST", "https://m/x", Map.of(), new byte[100], "image/png");
    assertThat(request.toString()).contains("body=100 bytes").doesNotContain("\u0000");
  }

  @Test
  void createReturnsADefaultTransport() {
    assertThat(MediaTransport.create()).isInstanceOf(JdkMediaTransport.class);
  }

  @Test
  void configConstructorBuildsAWorkingTransport() throws Exception {
    var transport = new JdkMediaTransport(HttpTransportConfig.builder().build());
    assertThatThrownBy(
            () ->
                transport.send(
                    new MediaTransport.BinaryRequest(
                        "GET", "http://localhost:1/x", Map.of(), null, "a/b")))
        .isInstanceOf(UncheckedTransportException.class);
  }

  @Test
  void headersAreExposedCaseInsensitively() {
    var response =
        new MediaTransport.BinaryResponse(
            200, Map.of("Content-Type", "image/png"), new byte[0], null);
    assertThat(response.headers()).containsEntry("content-type", "image/png");
    assertThat(response.header("CONTENT-TYPE")).isEqualTo("image/png");
  }

  private static final class StubResponse implements HttpResponse<byte[]> {
    private final byte[] body;

    private StubResponse(byte[] body) {
      this.body = body;
    }

    @Override
    public int statusCode() {
      return 200;
    }

    @Override
    public HttpRequest request() {
      throw new UnsupportedOperationException();
    }

    @Override
    public Optional<HttpResponse<byte[]>> previousResponse() {
      throw new UnsupportedOperationException();
    }

    @Override
    public java.net.http.HttpHeaders headers() {
      return java.net.http.HttpHeaders.of(
          Map.of("Content-Type", java.util.List.of("image/png")), (name, value) -> true);
    }

    @Override
    public byte[] body() {
      return body;
    }

    @Override
    public URI uri() {
      throw new UnsupportedOperationException();
    }

    @Override
    public HttpClient.Version version() {
      throw new UnsupportedOperationException();
    }

    @Override
    public java.util.Optional<javax.net.ssl.SSLSession> sslSession() {
      throw new UnsupportedOperationException();
    }
  }
}
