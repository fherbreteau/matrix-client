package io.github.fherbreteau.matrix.endpoint;

import io.github.fherbreteau.matrix.transport.HttpTransport;
import io.github.fherbreteau.matrix.transport.TransportException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.function.Consumer;

final class HttpTransportStub implements HttpTransport {

  private final Queue<Response> responses;
  private final Queue<RuntimeException> failures;
  private final Queue<String> urls;
  private final List<Consumer<Request>> recorders = new ArrayList<>();

  HttpTransportStub() {
    this(new ArrayDeque<>(), new ArrayDeque<>());
  }

  private HttpTransportStub(Queue<Response> responses, Queue<RuntimeException> failures) {
    this.responses = responses;
    this.failures = failures;
    this.urls = new ArrayDeque<>();
  }

  void recordInto(List<Request> requests) {
    recorders.add(requests::add);
  }

  static HttpTransportStub responding(int statusCode, String body) {
    return new HttpTransportStub(
        new ArrayDeque<>(List.of(new Response(statusCode, body))), new ArrayDeque<>());
  }

  static HttpTransportStub failing() {
    return new HttpTransportStub(
        new ArrayDeque<>(),
        new ArrayDeque<>(List.of(new TransportException("connection refused"))));
  }

  static HttpTransportStub recording() {
    return new HttpTransportStub(
        new ArrayDeque<>(List.of(new Response(404, "{}"))), new ArrayDeque<>());
  }

  void enqueue(Response response) {
    responses.add(response);
  }

  void enqueue(RuntimeException failure) {
    failures.add(failure);
  }

  String lastUrl() {
    return urls.peek();
  }

  @Override
  public Response send(Request request) {
    urls.add(request.url());
    for (Consumer<Request> recorder : recorders) {
      recorder.accept(request);
    }
    if (!failures.isEmpty()) {
      throw failures.remove();
    }
    return responses.remove();
  }
}
