package io.github.fherbreteau.matrix.transport;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.Authenticator;
import java.net.CookieHandler;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class JdkHttpTransportTest {

    private abstract static class TestHttpClient extends HttpClient {
        protected static <T> T unsupported() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<CookieHandler> cookieHandler() {
            return unsupported();
        }

        @Override
        public Optional<Duration> connectTimeout() {
            return unsupported();
        }

        @Override
        public Redirect followRedirects() {
            return unsupported();
        }

        @Override
        public Optional<ProxySelector> proxy() {
            return unsupported();
        }

        @Override
        public SSLContext sslContext() {
            return unsupported();
        }

        @Override
        public SSLParameters sslParameters() {
            return unsupported();
        }

        @Override
        public Optional<Authenticator> authenticator() {
            return unsupported();
        }

        @Override
        public HttpClient.Version version() {
            return unsupported();
        }

        @Override
        public Optional<Executor> executor() {
            return unsupported();
        }

        @Override
        public <T> CompletableFuture<HttpResponse<T>> sendAsync(HttpRequest request, HttpResponse.BodyHandler<T> responseBodyHandler) {
            return unsupported();
        }

        @Override
        public <T> CompletableFuture<HttpResponse<T>> sendAsync(HttpRequest request, HttpResponse.BodyHandler<T> responseBodyHandler, HttpResponse.PushPromiseHandler<T> pushPromiseHandler) {
            return unsupported();
        }
    }

    private static final class RecordingHttpClient extends TestHttpClient {
        private final AtomicInteger counter = new AtomicInteger();

        @Override
        public <T> HttpResponse<T> send(HttpRequest request, HttpResponse.BodyHandler<T> responseBodyHandler) {
            counter.incrementAndGet();
            assertThat(request.headers().firstValue("Accept")).contains("application/json");
            boolean hasBody = request.bodyPublisher().map(p -> p.contentLength() > 0).orElse(false);
            if (hasBody) {
                assertThat(request.headers().firstValue("Content-Type")).contains("application/json");
            }
            @SuppressWarnings("unchecked")
            HttpResponse<T> response = (HttpResponse<T>) new StubResponse(201, "{\"ok\":true}");
            return response;
        }
    }

    private static final class FailingHttpClient extends TestHttpClient {
        @Override
        public <T> HttpResponse<T> send(HttpRequest request, HttpResponse.BodyHandler<T> responseBodyHandler) throws IOException {
            throw new IOException("boom");
        }
    }

    private static final class InterruptingHttpClient extends TestHttpClient {
        @Override
        public <T> HttpResponse<T> send(HttpRequest request, HttpResponse.BodyHandler<T> responseBodyHandler) throws InterruptedException {
            throw new InterruptedException("interrupted");
        }
    }

    private static final class StubResponse implements HttpResponse<String> {
        private static <T> T unsupported() {
            throw new UnsupportedOperationException();
        }

        private final int statusCode;
        private final String body;

        private StubResponse(int statusCode, String body) {
            this.statusCode = statusCode;
            this.body = body;
        }

        @Override
        public int statusCode() {
            return statusCode;
        }

        @Override
        public java.net.http.HttpRequest request() {
            return unsupported();
        }

        @Override
        public Optional<HttpResponse<String>> previousResponse() {
            return unsupported();
        }

        @Override
        public java.net.http.HttpHeaders headers() {
            return unsupported();
        }

        @Override
        public String body() {
            return body;
        }

        @Override
        public URI uri() {
            return unsupported();
        }

        @Override
        public HttpClient.Version version() {
            return unsupported();
        }

        @Override
        public Optional<javax.net.ssl.SSLSession> sslSession() {
            return unsupported();
        }
    }

    @Test
    void sendsRequestAndMapsResponse() {
        var client = new RecordingHttpClient();
        var transport = new JdkHttpTransport(client);
        var ok = transport.send(new HttpTransport.Request("GET", "https://matrix.example.org/x", Map.of(), null));
        assertThat(ok.statusCode()).isEqualTo(201);
        assertThat(ok.body()).isEqualTo("{\"ok\":true}");
        assertThat(client.counter.get()).isEqualTo(1);

        var withBody = transport.send(new HttpTransport.Request("POST", "https://matrix.example.org/x", Map.of(), "{\"a\":1}"));
        assertThat(withBody.statusCode()).isEqualTo(201);
        assertThat(client.counter.get()).isEqualTo(2);
    }

    @Test
    void mapsIOException() {
        var transport = new JdkHttpTransport(new FailingHttpClient());
        var exception = assertThatExceptionOfType(UncheckedTransportException.class)
                .isThrownBy(() -> transport.send(new HttpTransport.Request("GET", "https://x", Map.of(), null)))
                .actual();
        assertThat(exception.getMessage()).isEqualTo("HTTP request failed");
    }

    @Test
    void mapsInterruptedException() {
        var transport = new JdkHttpTransport(new InterruptingHttpClient());
        try {
            assertThatExceptionOfType(IllegalStateException.class)
                    .isThrownBy(() -> transport.send(new HttpTransport.Request("GET", "https://x", Map.of(), null)));
            assertThat(Thread.currentThread().isInterrupted()).isTrue();
        } finally {
            Thread.interrupted();
        }
    }

    @Test
    void createReturnsDefaultTransport() {
        HttpTransport transport = HttpTransport.create();
        assertThat(transport).isInstanceOf(JdkHttpTransport.class);
    }
}
