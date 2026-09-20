package io.github.fherbreteau.matrix.transport;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JdkHttpTransportHttpServerTest {

    private HttpServer server;

    @AfterEach
    void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    private String startServer(com.sun.net.httpserver.HttpHandler handler) {
        try {
            server = HttpServer.create(new InetSocketAddress(0), 0);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        server.createContext("/", handler);
        server.start();
        return "http://localhost:" + server.getAddress().getPort();
    }

    @Test
    void sendsGetAndDecodesUtf8Json() {
        String body = "{\"value\": \"héllo 🙂\"}";
        String base = startServer(exchange -> {
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        var transport = new JdkHttpTransport();
        var response = transport.send(new HttpTransport.Request("GET", base + "/anywhere", Map.of(), null));
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).isEqualTo(body);
        assertThat(response.header("content-type")).isEqualTo("application/json");
    }

    @Test
    void sendsPostWithBodyAndHeaders() {
        AtomicReference<String> receivedBody = new AtomicReference<>();
        AtomicReference<String> receivedAuth = new AtomicReference<>();
        AtomicReference<String> receivedContentType = new AtomicReference<>();
        String base = startServer(exchange -> {
            receivedBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            receivedAuth.set(exchange.getRequestHeaders().getFirst("Authorization"));
            receivedContentType.set(exchange.getRequestHeaders().getFirst("Content-Type"));
            byte[] bytes = "{}".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        var transport = new JdkHttpTransport(JdkHttpTransport.config()
                .accessToken("s3cret-token")
                .build());
        var response = transport.send(new HttpTransport.Request("POST", base + "/rooms/!a:b/send", Map.of(), "{\"msg\":\"hi\"}"));
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(receivedBody.get()).isEqualTo("{\"msg\":\"hi\"}");
        assertThat(receivedContentType.get()).isEqualTo("application/json");
        assertThat(receivedAuth.get()).isEqualTo("Bearer s3cret-token");
    }

    @Test
    void sendsPutAndDelete() {
        String base = startServer(exchange -> {
            byte[] bytes = "{}".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        var transport = new JdkHttpTransport();
        assertThat(transport.send(new HttpTransport.Request("PUT", base + "/_matrix/1", Map.of(), "{}")).statusCode()).isEqualTo(200);
        assertThat(transport.send(new HttpTransport.Request("DELETE", base + "/_matrix/1", Map.of(), null)).statusCode()).isEqualTo(200);
    }

    @Test
    void parsesRetryAfterHeader() {
        String base = startServer(exchange -> {
            exchange.getResponseHeaders().set("Retry-After", "7");
            byte[] bytes = "{}".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(429, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        var transport = new JdkHttpTransport();
        var response = transport.send(new HttpTransport.Request("GET", base + "/limited", Map.of(), null));
        assertThat(response.statusCode()).isEqualTo(429);
        assertThat(response.retryAfterMs()).isEqualTo(7000L);
    }

    @Test
    void followsRedirectsWhenConfigured() {
        String base = startServer(exchange -> {
            exchange.getResponseHeaders().set("Location", "/final");
            if (exchange.getRequestURI().getPath().equals("/redirect")) {
                exchange.sendResponseHeaders(302, -1);
            } else {
                byte[] bytes = "{\"ok\":true}".getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(200, bytes.length);
                exchange.getResponseBody().write(bytes);
            }
            exchange.close();
        });
        var transport = new JdkHttpTransport(JdkHttpTransport.config().build());
        var response = transport.send(new HttpTransport.Request("GET", base + "/redirect", Map.of(), null));
        assertThat(response.body()).isEqualTo("{\"ok\":true}");
    }

    @Test
    void doesNotFollowRedirectsWhenDisabled() {
        String base = startServer(exchange -> {
            exchange.getResponseHeaders().set("Location", "/final");
            exchange.sendResponseHeaders(302, -1);
            exchange.close();
        });
        var transport = new JdkHttpTransport(JdkHttpTransport.config().followRedirects(false).build());
        var response = transport.send(new HttpTransport.Request("GET", base + "/redirect", Map.of(), null));
        assertThat(response.statusCode()).isEqualTo(302);
    }

    @Test
    void requestTimeoutRaisesTimeoutException() {
        String base = startServer(exchange -> {
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            exchange.sendResponseHeaders(200, -1);
            exchange.close();
        });
        var transport = new JdkHttpTransport(JdkHttpTransport.config()
                .requestTimeout(Duration.ofMillis(100))
                .build());
        var request = new HttpTransport.Request("GET", base + "/slow", Map.of(), null);
        assertThatExceptionOfType(TransportTimeoutException.class).isThrownBy(() -> transport.send(request));
    }

    @Test
    void refusesConnectionAsTransportException() {
        var transport = new JdkHttpTransport();
        var request = new HttpTransport.Request("GET", "http://localhost:1/nowhere", Map.of(), null);
        assertThatThrownBy(() -> transport.send(request)).isInstanceOf(TransportException.class);
    }

    @Test
    void redactsAuthorizationHeaderInRequestToString() {
        var request = new HttpTransport.Request("GET", "https://matrix.example.org/x",
                Map.of(HttpTransport.Request.AUTHORIZATION_HEADER, "Bearer s3cret-token", "X-Custom", "visible"),
                "{\"a\":1}");
        assertThat(request.toString())
                .contains("Authorization:***")
                .doesNotContain("s3cret-token")
                .contains("X-Custom:'visible'")
                .contains("body=7 bytes");
        var response = new HttpTransport.Response(200, Map.of(), "secret-body", null);
        assertThat(response.toString())
                .contains("statusCode=200")
                .doesNotContain("secret-body");
    }

    @Test
    void handlesNullHeadersGracefully() {
        var request = new HttpTransport.Request("GET", "https://x", null, null);
        assertThat(request.headers()).isEmpty();
        var response = new HttpTransport.Response(200, null, "body", null);
        assertThat(response.headers()).isEmpty();
    }
}
