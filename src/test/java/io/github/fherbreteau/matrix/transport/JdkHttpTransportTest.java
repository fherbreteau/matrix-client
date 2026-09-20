package io.github.fherbreteau.matrix.transport;


import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.InstanceOfAssertFactories.BOOLEAN;
import static org.assertj.core.api.InstanceOfAssertFactories.optional;

import java.io.IOException;
import java.net.Authenticator;
import java.net.CookieHandler;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;

import io.github.fherbreteau.matrix.transport.HttpTransport.Response;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSession;
import org.junit.jupiter.api.Test;

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
        public <T> CompletableFuture<HttpResponse<T>> sendAsync(HttpRequest request,
            HttpResponse.BodyHandler<T> responseBodyHandler) {
            return unsupported();
        }

        @Override
        public <T> CompletableFuture<HttpResponse<T>> sendAsync(HttpRequest request,
            HttpResponse.BodyHandler<T> responseBodyHandler,
            HttpResponse.PushPromiseHandler<T> pushPromiseHandler) {
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
                assertThat(request).extracting(HttpRequest::headers).extracting(x -> x.firstValue("Content-Type"), optional(String.class)).contains("application/json");
            }
            @SuppressWarnings("unchecked")
            HttpResponse<T> response = (HttpResponse<T>) new StubResponse(201, "{\"ok\":true}");
            return response;
        }
    }

    private static final class FailingHttpClient extends TestHttpClient {
        @Override
        public <T> HttpResponse<T> send(HttpRequest request, HttpResponse.BodyHandler<T> responseBodyHandler)
            throws IOException {
            throw new IOException("boom");
        }
    }

    private static final class TimingOutHttpClient extends TestHttpClient {
        @Override
        public <T> HttpResponse<T> send(HttpRequest request, HttpResponse.BodyHandler<T> responseBodyHandler)
            throws IOException {
            throw new HttpTimeoutException("timed out");
        }
    }

    private static final class InterruptingHttpClient extends TestHttpClient {
        @Override
        public <T> HttpResponse<T> send(HttpRequest request, HttpResponse.BodyHandler<T> responseBodyHandler)
            throws InterruptedException {
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
        public HttpRequest request() {
            return unsupported();
        }

        @Override
        public Optional<HttpResponse<String>> previousResponse() {
            return unsupported();
        }

        @Override
        public HttpHeaders headers() {
            return HttpHeaders.of(Map.of(), (name, value) -> true);
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
        public Optional<SSLSession> sslSession() {
            return unsupported();
        }
    }

    @Test
    void sendsRequestAndMapsResponse() {
        var client = new RecordingHttpClient();
        var transport = new JdkHttpTransport(client);
        var ok = transport.send(new HttpTransport.Request("GET", "https://matrix.example.org/x", Map.of(), null));
        assertThat(ok).extracting(Response::statusCode).isEqualTo(201);
        assertThat(ok).extracting(Response::body).isEqualTo("{\"ok\":true}");
        assertThat(client.counter).hasValue(1);

        var withBody = transport
            .send(new HttpTransport.Request("POST", "https://matrix.example.org/x", Map.of(), "{\"a\":1}"));
        assertThat(withBody).extracting(Response::statusCode).isEqualTo(201);
        assertThat(client.counter).hasValue(2);
    }

    @Test
    void mapsIOException() {
        var transport = new JdkHttpTransport(new FailingHttpClient());
        var request = new HttpTransport.Request("GET", "https://x", Map.of(), null);
        var exception = assertThatExceptionOfType(UncheckedTransportException.class)
            .isThrownBy(() -> transport.send(request))
            .actual();
        assertThat(exception).hasMessage("HTTP request failed: GET https://x")
            .hasCauseInstanceOf(IOException.class);
    }

    @Test
    void mapsTimeoutException() {
        var transport = new JdkHttpTransport(new TimingOutHttpClient());
        var request = new HttpTransport.Request("GET", "https://x", Map.of(), null);
        var exception = assertThatExceptionOfType(TransportTimeoutException.class)
            .isThrownBy(() -> transport.send(request))
            .actual();
        assertThat(exception).hasCauseInstanceOf(HttpTimeoutException.class);
    }

    @Test
    void mapsInterruptedException() {
        var transport = new JdkHttpTransport(new InterruptingHttpClient());
        var request = new HttpTransport.Request("GET", "https://x", Map.of(), null);
        try {
            assertThatExceptionOfType(TransportInterruptedException.class)
                    .isThrownBy(() -> transport.send(request));
            assertThat(Thread.currentThread()).extracting(Thread::isInterrupted, BOOLEAN).isTrue();
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
