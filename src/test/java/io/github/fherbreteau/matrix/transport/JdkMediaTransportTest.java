package io.github.fherbreteau.matrix.transport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.net.Authenticator;
import java.net.CookieHandler;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSession;
import org.junit.jupiter.api.Test;

class JdkMediaTransportTest {

  @Test
  void sendsRawBytesAndParsesTheResponse() {
    HttpClient client =
        new HttpClient() {
          @Override
          public Optional<CookieHandler> cookieHandler() {
            throw new UnsupportedOperationException();
          }

          @Override
          public Optional<Duration> connectTimeout() {
            throw new UnsupportedOperationException();
          }

          @Override
          public Redirect followRedirects() {
            throw new UnsupportedOperationException();
          }

          @Override
          public Optional<ProxySelector> proxy() {
            throw new UnsupportedOperationException();
          }

          @Override
          public SSLContext sslContext() {
            throw new UnsupportedOperationException();
          }

          @Override
          public SSLParameters sslParameters() {
            throw new UnsupportedOperationException();
          }

          @Override
          public Optional<Authenticator> authenticator() {
            throw new UnsupportedOperationException();
          }

          @Override
          public HttpClient.Version version() {
            throw new UnsupportedOperationException();
          }

          @Override
          public Optional<Executor> executor() {
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
          public <T> CompletableFuture<HttpResponse<T>> sendAsync(
              HttpRequest request, HttpResponse.BodyHandler<T> responseBodyHandler) {
            throw new UnsupportedOperationException();
          }

          @Override
          public <T> CompletableFuture<HttpResponse<T>> sendAsync(
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
    try (var stream = response.bodyStream()) {
      assertThat(stream.readAllBytes()).isEqualTo(new byte[] {1, 2, 3});
    } catch (IOException e) {
      throw new AssertionError(e);
    }
  }

  @Test
  void parsesRetryAfterHeader() {
    try (JdkMediaTransportTestClient testClient =
        new JdkMediaTransportTestClient() {
          @Override
          public <T> HttpResponse<T> send(
              HttpRequest request, HttpResponse.BodyHandler<T> responseBodyHandler) {
            @SuppressWarnings("unchecked")
            HttpResponse<T> response = (HttpResponse<T>) new StubRetryResponse();
            return response;
          }
        }) {
      HttpClient client = testClient.asClient();
      var transport = new JdkMediaTransport(client, HttpTransportConfig.builder().build());
      var response =
          transport.send(
              new MediaTransport.BinaryRequest("GET", "https://m/x", Map.of(), null, "a/b"));
      assertThat(response.retryAfterMs()).isEqualTo(7000L);
    }
  }

  private abstract static class JdkMediaTransportTestClient extends HttpClient {
    @Override
    public Optional<CookieHandler> cookieHandler() {
      throw new UnsupportedOperationException();
    }

    @Override
    public Optional<Duration> connectTimeout() {
      throw new UnsupportedOperationException();
    }

    @Override
    public Redirect followRedirects() {
      throw new UnsupportedOperationException();
    }

    @Override
    public Optional<ProxySelector> proxy() {
      throw new UnsupportedOperationException();
    }

    @Override
    public SSLContext sslContext() {
      throw new UnsupportedOperationException();
    }

    @Override
    public SSLParameters sslParameters() {
      throw new UnsupportedOperationException();
    }

    @Override
    public Optional<Authenticator> authenticator() {
      throw new UnsupportedOperationException();
    }

    @Override
    public HttpClient.Version version() {
      throw new UnsupportedOperationException();
    }

    @Override
    public Optional<Executor> executor() {
      throw new UnsupportedOperationException();
    }

    @Override
    public <T> CompletableFuture<HttpResponse<T>> sendAsync(
        HttpRequest request, HttpResponse.BodyHandler<T> responseBodyHandler) {
      throw new UnsupportedOperationException();
    }

    @Override
    public <T> CompletableFuture<HttpResponse<T>> sendAsync(
        HttpRequest request,
        HttpResponse.BodyHandler<T> responseBodyHandler,
        HttpResponse.PushPromiseHandler<T> pushPromiseHandler) {
      throw new UnsupportedOperationException();
    }

    HttpClient asClient() {
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
    public HttpHeaders headers() {
      return HttpHeaders.of(Map.of("Retry-After", List.of("7")), (name, value) -> true);
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
    public Optional<SSLSession> sslSession() {
      throw new UnsupportedOperationException();
    }
  }

  @Test
  void binaryRequestComparesBodyContent() {
    var first =
        new MediaTransport.BinaryRequest("POST", "https://m/x", Map.of(), new byte[] {1, 2}, "t");
    var second =
        new MediaTransport.BinaryRequest("POST", "https://m/x", Map.of(), new byte[] {1, 2}, "t");
    var differentBody =
        new MediaTransport.BinaryRequest("POST", "https://m/x", Map.of(), new byte[] {1}, "t");
    var differentType =
        new MediaTransport.BinaryRequest("POST", "https://m/x", Map.of(), new byte[] {1, 2}, "u");
    var differentUrl =
        new MediaTransport.BinaryRequest("POST", "https://m/y", Map.of(), new byte[] {1, 2}, "t");
    var differentMethod =
        new MediaTransport.BinaryRequest("GET", "https://m/x", Map.of(), new byte[] {1, 2}, "t");
    var differentHeaders =
        new MediaTransport.BinaryRequest(
            "POST", "https://m/x", Map.of("h", "v"), new byte[] {1, 2}, "t");
    assertThat(first)
        .isEqualTo(second)
        .hasSameHashCodeAs(second)
        .isNotEqualTo(differentBody)
        .isNotEqualTo(differentType)
        .isNotEqualTo(differentUrl)
        .isNotEqualTo(differentMethod)
        .isNotEqualTo(differentHeaders)
        .isNotNull();
  }

  @Test
  void binaryRequestBodyIsDefensive() {
    var body = new byte[] {1, 2};
    var request = new MediaTransport.BinaryRequest("POST", "https://m/x", Map.of(), body, "t");
    body[0] = 9;
    assertThat(request.body()).containsExactly(1, 2);
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
  void configConstructorBuildsAWorkingTransport() {
    var transport = new JdkMediaTransport(HttpTransportConfig.builder().build());
    var request =
        new MediaTransport.BinaryRequest("GET", "http://localhost:1/x", Map.of(), null, "a/b");
    assertThatThrownBy(() -> transport.send(request))
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
    public HttpHeaders headers() {
      return HttpHeaders.of(Map.of("Content-Type", List.of("image/png")), (name, value) -> true);
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
    public Optional<SSLSession> sslSession() {
      throw new UnsupportedOperationException();
    }
  }
}
