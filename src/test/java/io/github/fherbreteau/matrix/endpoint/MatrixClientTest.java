package io.github.fherbreteau.matrix.endpoint;

import io.github.fherbreteau.matrix.error.MatrixServerException;
import io.github.fherbreteau.matrix.transport.HttpTransport;
import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MatrixClientTest {

    @Test
    void canInstantiateClient() {
        MatrixClient client = MatrixClient.builder("https://matrix.example.org").build();
        assertEquals("https://matrix.example.org", client.getHomeserverUrl());
    }

    @Test
    void rejectsBlankHomeserverUrl() {
        assertThrows(IllegalArgumentException.class, () -> MatrixClient.builder(" ").build());
    }

    @Test
    void getVersionsParsesResponse() {
        var responses = new ArrayDeque<>(List.of(
                new HttpTransport.Response(200, "{\"versions\":[\"v1.11\"]}")));
        MatrixClient client = MatrixClient.builder("https://matrix.example.org")
                .transport(request -> responses.pop())
                .build();
        var versions = client.getVersions();
        assertEquals("v1.11", versions.asObject().get("versions").asArray().get(0).asString());
    }

    @Test
    void serverErrorMapsToMatrixException() {
        MatrixClient client = MatrixClient.builder("https://matrix.example.org")
                .transport(request -> new HttpTransport.Response(403,
                        "{\"errcode\":\"M_FORBIDDEN\",\"error\":\"Invalid password\"}"))
                .build();
        var exception = assertThrows(MatrixServerException.class, client::getVersions);
        assertEquals(403, exception.getStatusCode());
        assertEquals("M_FORBIDDEN", exception.getErrcode());
        assertEquals("Invalid password", exception.getMessage());
    }
}
