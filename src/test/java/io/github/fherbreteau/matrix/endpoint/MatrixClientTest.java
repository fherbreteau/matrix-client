package io.github.fherbreteau.matrix.endpoint;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.InstanceOfAssertFactories.BOOLEAN;
import static org.assertj.core.api.InstanceOfAssertFactories.collection;
import static org.assertj.core.api.InstanceOfAssertFactories.type;

import java.util.ArrayDeque;
import java.util.List;
import java.util.Map;

import io.github.fherbreteau.matrix.error.MatrixServerException;
import io.github.fherbreteau.matrix.error.RateLimitedException;
import io.github.fherbreteau.matrix.json.JsonObject;
import io.github.fherbreteau.matrix.json.JsonValue;
import io.github.fherbreteau.matrix.transport.HttpTransport;
import org.junit.jupiter.api.Test;

class MatrixClientTest {

    @Test
    void canInstantiateClient() {
        MatrixClient client = MatrixClient.builder("https://matrix.example.org/").build();
        assertThat(client).extracting(MatrixClient::getHomeserverUrl).isEqualTo("https://matrix.example.org");
        assertThat(client).extracting(MatrixClient::getTransport).isInstanceOf(HttpTransport.class);
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
        assertThat(versions).extracting(JsonValue::asObject)
                        .extracting(x -> x.get("versions"))
                        .extracting(JsonValue::asArray)
                        .extracting(x -> x.get(0))
                        .extracting(JsonValue::asString)
                        .isEqualTo("v1.11");
    }

    @Test
    void blankResponseYieldsEmptyObject() {
        MatrixClient client = MatrixClient.builder("https://matrix.example.org")
            .transport(request -> new HttpTransport.Response(200, ""))
            .build();
        var result = client.post("test", null);
        assertThat(result).extracting(JsonValue::isObject, BOOLEAN).isTrue();
        assertThat(result).extracting(JsonValue::asObject, type(JsonObject.class))
                        .extracting(JsonObject::names, collection(String.class)).isEmpty();
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
        assertThat(exception).extracting(MatrixServerException::getStatusCode).isEqualTo(403);
        assertThat(exception).extracting(MatrixServerException::getErrcode).isEqualTo("M_FORBIDDEN");
        assertThat(exception).extracting(MatrixServerException::getMessage).isEqualTo("Invalid password");
    }

    @Test
    void rateLimitedResponseRaisesRateLimitedException() {
        MatrixClient client = MatrixClient.builder("https://matrix.example.org")
            .transport(request -> new HttpTransport.Response(429,
            Map.of("retry-after", "30"),
            "{\"errcode\":\"M_LIMIT_EXCEEDED\",\"error\":\"Too many\"}",
            30000L))
            .build();
        var exception = assertThatExceptionOfType(RateLimitedException.class)
            .isThrownBy(client::getVersions)
            .actual();
        assertThat(exception).extracting(RateLimitedException::getRetryAfterMs).isEqualTo(30000L);
        assertThat(exception).extracting(RateLimitedException::getErrcode).isEqualTo("M_LIMIT_EXCEEDED");
        assertThat(exception).extracting(RateLimitedException::isRetryable, BOOLEAN).isTrue();
    }

    @Test
    void serverErrorWithNonJsonBodyFallsBack() {
        MatrixClient client = MatrixClient.builder("https://matrix.example.org")
            .transport(request -> new HttpTransport.Response(500, "oops"))
            .build();
        var exception = assertThatExceptionOfType(MatrixServerException.class)
            .isThrownBy(client::getVersions)
            .actual();
        assertThat(exception).extracting(MatrixServerException::getErrcode).isEqualTo("M_UNRECOGNIZED");
        assertThat(exception).extracting(MatrixServerException::getMessage).isEqualTo("HTTP 500");
    }

    @Test
    void stubTransportReceivesRequests() {
        MatrixClient client = MatrixClient.builder("https://matrix.example.org")
            .transport(request -> new HttpTransport.Response(200, "{}"))
            .build();
        var response = client.post("_matrix/client/r0/rooms/!a:b/send/m.room.message/1",
            new JsonObject().put("body", "hello"));
        assertThat(response).extracting(JsonValue::isObject, BOOLEAN).isTrue();
    }
}
