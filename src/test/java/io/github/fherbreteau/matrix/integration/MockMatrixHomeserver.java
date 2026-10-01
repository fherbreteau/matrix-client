package io.github.fherbreteau.matrix.integration;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import io.github.fherbreteau.matrix.transport.HttpTransport;
import io.github.fherbreteau.matrix.transport.HttpTransport.Response;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicBoolean;

final class MockMatrixHomeserver implements AutoCloseable {

  private final HttpServer server;
  private final Queue<Response> responses = new ArrayDeque<>();
  private final List<CapturedRequest> requests = new ArrayList<>();
  private final List<CapturedResponse> capturedResponses = new ArrayList<>();
  private final AtomicBoolean closed = new AtomicBoolean();

  MockMatrixHomeserver() {
    try {
      server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    } catch (IOException exception) {
      throw new UncheckedIOException(exception);
    }
    server.createContext("/", this::handle);
    server.start();
  }

  String baseUrl() {
    return "http://127.0.0.1:" + server.getAddress().getPort();
  }

  synchronized void enqueue(int statusCode, String body) {
    responses.add(new Response(statusCode, body));
  }

  synchronized void enqueue(Response response) {
    responses.add(response);
  }

  synchronized List<CapturedRequest> requests() {
    return List.copyOf(requests);
  }

  synchronized CapturedRequest lastRequest() {
    if (requests.isEmpty()) {
      throw new IllegalStateException("No request has been recorded");
    }
    return requests.getLast();
  }

  synchronized List<CapturedResponse> responses() {
    return List.copyOf(capturedResponses);
  }

  void assertWithDiagnostics(Runnable assertions) {
    try {
      assertions.run();
    } catch (AssertionError failure) {
      throw new AssertionError(
          failure.getMessage() + System.lineSeparator() + diagnosticContext(), failure);
    }
  }

  private synchronized String diagnosticContext() {
    return "Mock homeserver exchanges:"
        + System.lineSeparator()
        + java.util.stream.IntStream.range(0, requests.size())
            .mapToObj(
                index ->
                    "["
                        + index
                        + "] "
                        + requests.get(index).redactedDescription()
                        + " -> "
                        + (index < capturedResponses.size()
                            ? capturedResponses.get(index).redactedDescription()
                            : "response pending"))
            .collect(java.util.stream.Collectors.joining(System.lineSeparator()));
  }

  private void handle(HttpExchange exchange) throws IOException {
    String requestBody =
        new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
    Response response;
    synchronized (this) {
      requests.add(
          new CapturedRequest(
              exchange.getRequestMethod(),
              exchange.getRequestURI().getPath(),
              exchange.getRequestURI().getRawQuery(),
              exchange.getRequestHeaders().getFirst(HttpTransport.Request.AUTHORIZATION_HEADER),
              requestBody));
      response = responses.poll();
    }
    if (response == null) {
      response =
          new Response(500, "{\"errcode\":\"M_UNKNOWN\",\"error\":\"No mock response queued\"}");
    }
    exchange.getResponseHeaders().set("Content-Type", "application/json");
    response.headers().forEach((name, value) -> exchange.getResponseHeaders().set(name, value));
    if (response.retryAfterMs() != null) {
      exchange
          .getResponseHeaders()
          .set("Retry-After", Long.toString(response.retryAfterMs() / 1000));
    }
    byte[] bytes =
        response.body() == null ? new byte[0] : response.body().getBytes(StandardCharsets.UTF_8);
    synchronized (this) {
      capturedResponses.add(new CapturedResponse(response.statusCode(), bytes.length));
    }
    exchange.sendResponseHeaders(response.statusCode(), bytes.length);
    exchange.getResponseBody().write(bytes);
    exchange.close();
  }

  @Override
  public void close() {
    if (closed.compareAndSet(false, true)) {
      server.stop(0);
    }
  }

  record CapturedResponse(int statusCode, int bodyBytes) {

    String redactedDescription() {
      return "status=" + statusCode + " body=" + bodyBytes + " bytes";
    }
  }

  record CapturedRequest(
      String method, String path, String query, String authorization, String body) {

    String redactedDescription() {
      return method
          + " "
          + path
          + (query == null ? "" : "?***")
          + " authorization="
          + (authorization == null ? "<none>" : "Bearer ***")
          + " body="
          + (body == null ? 0 : body.length())
          + " bytes";
    }

    @Override
    public String toString() {
      return redactedDescription();
    }
  }
}
