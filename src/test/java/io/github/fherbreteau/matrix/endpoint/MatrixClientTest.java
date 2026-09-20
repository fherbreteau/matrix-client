package io.github.fherbreteau.matrix.endpoint;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.InstanceOfAssertFactories.BOOLEAN;
import static org.assertj.core.api.InstanceOfAssertFactories.collection;
import static org.assertj.core.api.InstanceOfAssertFactories.type;

import java.util.ArrayDeque;
import java.util.List;
import java.util.Map;

import io.github.fherbreteau.matrix.error.DiscoveryException;
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
        assertThat(exception.getErrcode()).isEqualTo("M_UNRECOGNIZED");
        assertThat(exception.getMessage()).isEqualTo("HTTP 500");
    }

    @Test
    void discoveryResolvesHomeserverAtBuildTime() {
        var stub = HttpTransportStub.responding(200,
            "{\"m.homeserver\":{\"base_url\":\"https://real.example.org:8448/\"}}");
        stub.enqueue(new HttpTransport.Response(200,
            "{\"versions\":[\"v1.11\"],\"unstable_features\":{\"x\":true}}"));
        MatrixClient client = MatrixClient.builder("https://matrix.example.org")
            .transport(stub)
            .discover()
            .validateVersions()
            .build();
        assertThat(client.getHomeserverUrl()).isEqualTo("https://real.example.org:8448");
        assertThat(client.getDiscovery().homeserverUrl()).isEqualTo("https://real.example.org:8448");
        assertThat(client.getDiscovery().usedFallback()).isFalse();
        assertThat(client.getCapabilities().supports("v1.11")).isTrue();
        assertThat(client.getCapabilities().getFields()).containsKey("unstable_features");
    }

    @Test
    void discoveryFallsBackToExplicitUrl() {
        var stub = HttpTransportStub.failing();
        MatrixClient client = MatrixClient.builder("https://matrix.example.org")
            .transport(stub)
            .discover()
            .build();
        assertThat(client.getHomeserverUrl()).isEqualTo("https://matrix.example.org");
        assertThat(client.getDiscovery().usedFallback()).isTrue();
        assertThat(client.getCapabilities()).isNull();
    }

    @Test
    void unsupportedVersionsResponseFailsFastAtBuildTime() {
        var stub = HttpTransportStub.failing();
        MatrixClient client = MatrixClient.builder("https://matrix.example.org")
            .transport(stub)
            .discover()
            .build();
        assertThat(client.getHomeserverUrl()).isEqualTo("https://matrix.example.org");
    }

    @Test
    void malformedVersionsFailFastAtBuildTime() {
        var builder = MatrixClient.builder("https://matrix.example.org")
            .transport(request -> new HttpTransport.Response(200, "{\"versions\":\"not-an-array\"}"))
            .validateVersions();
        assertThatThrownBy(builder::build)
                .isInstanceOf(DiscoveryException.class);
    }

    @Test
    void getSupportedVersionsValidates() {
        MatrixClient client = MatrixClient.builder("https://matrix.example.org")
            .transport(request -> new HttpTransport.Response(200,
            "{\"versions\":[\"v1.11\"],\"unstable_features\":{\"f\":true}}"))
            .build();
        var versions = client.getSupportedVersions();
        assertThat(versions.supports("v1.11")).isTrue();
        assertThat(versions.getFields()).containsKey("unstable_features");
    }

    @Test
    void malformedSupportedVersionsRaiseDiscoveryException() {
        MatrixClient client = MatrixClient.builder("https://matrix.example.org")
            .transport(request -> new HttpTransport.Response(200, "{\"nope\":true}"))
            .build();
        assertThatThrownBy(client::getSupportedVersions)
                .isInstanceOf(DiscoveryException.class);
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
