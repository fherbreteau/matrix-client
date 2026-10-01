package io.github.fherbreteau.matrix.transport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
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
            HttpResponse<T> response = (HttpResponse<T>) new StubResponse();
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
    assertRetryAfter("7", 7000L);
  }

  @Test
  void parsesHttpDateRetryAfterHeader() {
    String retryAt =
        java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME.format(
            java.time.Instant.now().plusSeconds(60).atZone(java.time.ZoneOffset.UTC));
    assertRetryAfter(retryAt, null);
  }

  private static void assertRetryAfter(String retryAt, Long expected) {
    try (JdkMediaTransportTestClient testClient =
        new JdkMediaTransportTestClient() {
          @Override
          public <T> HttpResponse<T> send(
              HttpRequest request, HttpResponse.BodyHandler<T> responseBodyHandler) {
            @SuppressWarnings("unchecked")
            HttpResponse<T> response = (HttpResponse<T>) new StubRetryResponse(retryAt);
            return response;
          }
        }) {
      HttpClient client = testClient.asClient();
      var transport = new JdkMediaTransport(client, HttpTransportConfig.builder().build());
      var response =
          transport.send(
              new MediaTransport.BinaryRequest("GET", "https://m/x", Map.of(), null, "a/b"));
      if (expected != null) {
        assertThat(response.retryAfterMs()).isEqualTo(expected);
      } else {
        assertThat(response.retryAfterMs()).isBetween(0L, 60_000L);
      }
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

  private static final class StubRetryResponse implements HttpResponse<InputStream> {

    private final String retryAfter;

    private StubRetryResponse(String retryAfter) {
      this.retryAfter = retryAfter;
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
    public Optional<HttpResponse<InputStream>> previousResponse() {
      throw new UnsupportedOperationException();
    }

    @Override
    public HttpHeaders headers() {
      return HttpHeaders.of(Map.of("Retry-After", List.of(retryAfter)), (name, value) -> true);
    }

    @Override
    public InputStream body() {
      return InputStream.nullInputStream();
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
  void transportConfigEnforcesUploadLimit() {
    var config = HttpTransportConfig.builder().maxMediaUploadBytes(1).build();
    var transport = new JdkMediaTransport(config);
    var request =
        new MediaTransport.StreamingBinaryRequest(
            "POST",
            "https://m/upload",
            Map.of(),
            new ByteArrayInputStream(new byte[2]),
            java.util.OptionalLong.of(2),
            "application/octet-stream");
    assertThatThrownBy(() -> transport.send(request, 0, 0))
        .isInstanceOf(MediaSizeLimitException.class);
  }

  @Test
  void uploadStreamSingleByteReadEnforcesLimit() {
    var limited =
        JdkMediaTransport.limitedUploadStream(new ByteArrayInputStream(new byte[] {1, 2}), 1);
    assertThatThrownBy(() -> limited.readNBytes(2)).isInstanceOf(MediaSizeLimitException.class);
  }

  @Test
  void uploadStreamAtLimitReturnsEndOfStream() throws IOException {
    var limited =
        JdkMediaTransport.limitedUploadStream(new ByteArrayInputStream(new byte[] {1}), 1);
    assertThat(limited.read()).isEqualTo(1);
    assertThat(limited.read()).isEqualTo(-1);
  }

  @Test
  void configSupportsMediaUploadLimit() {
    var config = HttpTransportConfig.builder().maxMediaUploadBytes(1024).build();
    assertThat(config.maxMediaUploadBytes()).isEqualTo(1024);
  }

  @Test
  void configRejectsNegativeMediaUploadLimit() {
    var builder = HttpTransportConfig.builder();
    assertThatThrownBy(() -> builder.maxMediaUploadBytes(-1))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void sizeLimitExceptionReportsTheConfiguredLimit() {
    assertThat(new MediaSizeLimitException(10))
        .hasMessage("Media transfer exceeds the configured maximum size of 10 bytes");
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
  void binaryRequestDescriptionRedactsQueryAndFragment() {
    var request =
        new MediaTransport.BinaryRequest(
            "GET",
            "https://media.example.org/path?access_token=secret#private",
            Map.of(),
            null,
            "application/octet-stream");
    assertThat(request.toString())
        .contains("https://media.example.org/path")
        .doesNotContain("access_token", "secret", "private", "#");
  }

  @Test
  void mapsIoExceptionWithoutExposingQueryValues() {
    var transport = new JdkMediaTransport(HttpTransportConfig.builder().build());
    var request =
        new MediaTransport.BinaryRequest(
            "GET",
            "http://localhost:1/path?access_token=secret",
            Map.of(),
            null,
            "application/octet-stream");
    assertThatThrownBy(() -> transport.send(request))
        .isInstanceOf(UncheckedTransportException.class)
        .hasMessageContaining("http://localhost:1/path")
        .hasMessageNotContaining("access_token")
        .hasMessageNotContaining("secret");
  }

  @Test
  void headersAreExposedCaseInsensitively() {
    var response =
        new MediaTransport.BinaryResponse(
            200, Map.of("Content-Type", "image/png"), new byte[0], null);
    assertThat(response.headers()).containsEntry("content-type", "image/png");
    assertThat(response.header("CONTENT-TYPE")).isEqualTo("image/png");
  }

  private static final class StubResponse implements HttpResponse<InputStream> {

    @Override
    public int statusCode() {
      return 200;
    }

    @Override
    public HttpRequest request() {
      throw new UnsupportedOperationException();
    }

    @Override
    public Optional<HttpResponse<InputStream>> previousResponse() {
      throw new UnsupportedOperationException();
    }

    @Override
    public HttpHeaders headers() {
      return HttpHeaders.of(
          Map.of("Content-Type", List.of("image/png"), "Content-Length", List.of("3")),
          (name, value) -> true);
    }

    @Override
    public InputStream body() {
      return new java.io.ByteArrayInputStream(new byte[] {1, 2, 3});
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
