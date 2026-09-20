package io.github.fherbreteau.matrix.endpoint;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class HomeserverDiscoveryTest {

    @Test
    void normalizesUrls() {
        assertThat(HomeserverDiscovery.normalize("https://matrix.example.org/")).isEqualTo("https://matrix.example.org");
        assertThat(HomeserverDiscovery.normalize("https://matrix.example.org///"))
                .isEqualTo("https://matrix.example.org");
        assertThat(HomeserverDiscovery.normalize("http://localhost:8448/"))
                .isEqualTo("http://localhost:8448");
        assertThat(HomeserverDiscovery.normalize("  https://matrix.example.org  "))
                .isEqualTo("https://matrix.example.org");
    }

    @Test
    void rejectsInvalidUrls() {
        assertThatIllegalArgumentException().isThrownBy(() -> HomeserverDiscovery.normalize(null));
        assertThatIllegalArgumentException().isThrownBy(() -> HomeserverDiscovery.normalize(" "));
        assertThatIllegalArgumentException().isThrownBy(() -> HomeserverDiscovery.normalize("matrix.example.org"));
        assertThatIllegalArgumentException().isThrownBy(() -> HomeserverDiscovery.normalize("ftp://matrix.example.org"));
        assertThatIllegalArgumentException().isThrownBy(() -> HomeserverDiscovery.normalize("https://ex ample.org"));
    }

    @Test
    void discoversFromWellKnown() {
        var transport = HttpTransportStub.responding(200, """
                {"m.homeserver":{"base_url":"https://matrix.example.org:8448/"},
                 "m.identity_server":{"base_url":"https://id.example.org"},
                 "org.example.unknown":{"x":1}}
                """);
        var discovered = HomeserverDiscovery.discover(transport, "https://matrix.example.org");
        assertThat(discovered.homeserverUrl()).isEqualTo("https://matrix.example.org:8448");
        assertThat(discovered.identityServerUrl()).isEqualTo("https://id.example.org");
        assertThat(discovered.usedFallback()).isFalse();
        assertThat(discovered.wellKnown().asObject().get("org.example.unknown")).isNotNull();
    }

    @Test
    void fallsBackWhenWellKnownFails() {
        var transport = HttpTransportStub.failing();
        var discovered = HomeserverDiscovery.discover(transport, "https://matrix.example.org");
        assertThat(discovered.homeserverUrl()).isEqualTo("https://matrix.example.org");
        assertThat(discovered.identityServerUrl()).isNull();
        assertThat(discovered.usedFallback()).isTrue();
        assertThat(discovered.wellKnown()).isNull();
    }

    static List<HttpTransportStub> invalidWellKnownResponses() {
        return List.of(
                HttpTransportStub.responding(404, "{\"errcode\":\"M_NOT_FOUND\"}"),
                HttpTransportStub.responding(200, "not json"),
                HttpTransportStub.responding(200, "[1,2,3]"),
                HttpTransportStub.responding(200, "{\"m.identity_server\":{\"base_url\":\"https://id\"}}"),
                HttpTransportStub.responding(200, "{\"m.homeserver\":{}}"),
                HttpTransportStub.responding(200, "{\"m.homeserver\":{\"base_url\":42}}"),
                HttpTransportStub.responding(200, "{\"m.homeserver\":{\"base_url\":\"  \"}}"),
                HttpTransportStub.responding(200, "{\"m.homeserver\":{\"base_url\":\"ftp://bad\"}}"),
                HttpTransportStub.responding(200, ""));
    }

    @ParameterizedTest
    @MethodSource("invalidWellKnownResponses")
    void fallsBackOnInvalidWellKnownResponse(HttpTransportStub transport) {
        var discovered = HomeserverDiscovery.discover(transport, "https://matrix.example.org");
        assertThat(discovered.usedFallback()).isTrue();
        assertThat(discovered.homeserverUrl()).isEqualTo("https://matrix.example.org");
        assertThat(discovered.identityServerUrl()).isNull();
    }

    @Test
    void wellKnownUrlContainsTheRightPath() {
        var transport = HttpTransportStub.recording();
        HomeserverDiscovery.discover(transport, "https://matrix.example.org");
        assertThat(transport.lastUrl()).isEqualTo("https://matrix.example.org/.well-known/matrix/client");
    }
}
