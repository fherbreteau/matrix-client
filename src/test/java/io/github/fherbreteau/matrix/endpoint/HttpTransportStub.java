package io.github.fherbreteau.matrix.endpoint;

import java.util.ArrayDeque;
import java.util.Queue;

import io.github.fherbreteau.matrix.transport.HttpTransport;
import io.github.fherbreteau.matrix.transport.HttpTransport.Request;
import io.github.fherbreteau.matrix.transport.HttpTransport.Response;
import io.github.fherbreteau.matrix.transport.TransportException;

final class HttpTransportStub implements HttpTransport {

    private final Queue<Response> responses;
    private final Queue<RuntimeException> failures;
    private final Queue<String> urls;

    private HttpTransportStub(Queue<Response> responses, Queue<RuntimeException> failures) {
        this.responses = responses;
        this.failures = failures;
        this.urls = new ArrayDeque<>();
    }

    static HttpTransportStub responding(int statusCode, String body) {
        return new HttpTransportStub(new ArrayDeque<>(java.util.List.of(new Response(statusCode, body))),
                new ArrayDeque<>());
    }

    static HttpTransportStub failing() {
        return new HttpTransportStub(new ArrayDeque<>(),
                new ArrayDeque<>(java.util.List.of(new TransportException("connection refused"))));
    }

    static HttpTransportStub recording() {
        return new HttpTransportStub(new ArrayDeque<>(java.util.List.of(new Response(404, "{}"))),
                new ArrayDeque<>());
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
        if (!failures.isEmpty()) {
            throw failures.remove();
        }
        return responses.remove();
    }
}
