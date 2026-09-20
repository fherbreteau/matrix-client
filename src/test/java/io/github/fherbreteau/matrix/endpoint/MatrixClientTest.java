package io.github.fherbreteau.matrix.endpoint;

import io.github.fherbreteau.matrix.error.MatrixServerException;
import io.github.fherbreteau.matrix.json.JsonObject;
import io.github.fherbreteau.matrix.transport.HttpTransport;
import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class MatrixClientTest {

    @Test
    void canInstantiateClient() {
        MatrixClient client = MatrixClient.builder("https://matrix.example.org/").build();
        assertThat(client.getHomeserverUrl()).isEqualTo("https://matrix.example.org");
        assertThat(client.getTransport()).isInstanceOf(HttpTransport.class);
    }

    @Test
    void rejectsBlankHomeserverUrl() {
        assertThatIllegalArgumentException().isThrownBy(() -> MatrixClient.builder(" ").build());
    }

    @Test
    void getVersionsParsesResponse() {
        var responses = new ArrayDeque<>(List.of(
                new HttpTransport.Response(200, "{\"versions\":[\"v1.11\"]}")));
        MatrixClient client = MatrixClient.builder("https://matrix.example.org")
                .transport(request -> responses.pop())
                .build();
        var versions = client.getVersions();
        assertThat(versions.asObject().get("versions").asArray().get(0).asString()).isEqualTo("v1.11");
    }

    @Test
    void blankResponseYieldsEmptyObject() {
        MatrixClient client = MatrixClient.builder("https://matrix.example.org")
                .transport(request -> new HttpTransport.Response(200, ""))
                .build();
        var result = client.post("test", null);
        assertThat(result.isObject()).isTrue();
        assertThat(result.asObject().names()).isEmpty();
    }

    @Test
    void serverErrorMapsToMatrixException() {
        MatrixClient client = MatrixClient.builder("https://matrix.example.org")
                .transport(request -> new HttpTransport.Response(403,
                        "{\"errcode\":\"M_FORBIDDEN\",\"error\":\"Invalid password\"}"))
                .build();
        var exception = assertThatExceptionOfType(MatrixServerException.class)
                .isThrownBy(client::getVersions)
                .actual();
        assertThat(exception.getStatusCode()).isEqualTo(403);
        assertThat(exception.getErrcode()).isEqualTo("M_FORBIDDEN");
        assertThat(exception.getMessage()).isEqualTo("Invalid password");
    }

    @Test
    void serverErrorWithNonJsonBodyFallsBack() {
        MatrixClient client = MatrixClient.builder("https://matrix.example.org")
                .transport(request -> new HttpTransport.Response(500, "oops"))
                .build();
        var exception = assertThatExceptionOfType(MatrixServerException.class)
                .isThrownBy(client::getVersions)
                .actual();
        assertThat(exception.getErrcode()).isEqualTo("M_UNRECOGNIZED");
        assertThat(exception.getMessage()).isEqualTo("HTTP 500");
    }

    @Test
    void stubTransportReceivesRequests() {
        MatrixClient client = MatrixClient.builder("https://matrix.example.org")
                .transport(request -> new HttpTransport.Response(200, "{}"))
                .build();
        var response = client.post("_matrix/client/r0/rooms/!a:b/send/m.room.message/1",
                new JsonObject().put("body", "hello"));
        assertThat(response.isObject()).isTrue();
    }
}
