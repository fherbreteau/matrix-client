package io.github.fherbreteau.matrix.endpoint;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.fherbreteau.matrix.error.DiscoveryException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class HomeserverDiscoveryTest {

  @Test
  void legacyDiscoveryResultConstructorMapsFallbackToIgnore() {
    var discovered = new DiscoveredHomeserver("https://matrix.example.org", null, null, true);
    assertThat(discovered)
        .extracting(DiscoveredHomeserver::outcome, DiscoveredHomeserver::failureReason)
        .containsExactly(DiscoveryOutcome.IGNORE, null);
  }

  @Test
  void normalizesUrls() {
    assertThat(HomeserverDiscovery.normalize("https://matrix.example.org/"))
        .isEqualTo("https://matrix.example.org");
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
    assertThatIllegalArgumentException()
        .isThrownBy(() -> HomeserverDiscovery.normalize("matrix.example.org"));
    assertThatIllegalArgumentException()
        .isThrownBy(() -> HomeserverDiscovery.normalize("ftp://matrix.example.org"));
    assertThatIllegalArgumentException()
        .isThrownBy(() -> HomeserverDiscovery.normalize("https://ex ample.org"));
    assertThatIllegalArgumentException()
        .isThrownBy(
            () -> HomeserverDiscovery.normalize("https://matrix.example.org?access_token=x"));
    assertThatIllegalArgumentException()
        .isThrownBy(() -> HomeserverDiscovery.normalize("https://matrix.example.org#fragment"));
  }

  @Test
  void successfulDiscoveryReportsDiscoveredOutcome() {
    var transport =
        HttpTransportStub.responding(
            200, "{\"m.homeserver\":{\"base_url\":\"https://matrix.example.org\"}}");
    var result = HomeserverDiscovery.discover(transport, "https://matrix.example.org");
    assertThat(result)
        .extracting(DiscoveredHomeserver::outcome, DiscoveredHomeserver::usedFallback)
        .containsExactly(DiscoveryOutcome.DISCOVERED, false);
  }

  @Test
  void invalidHomeserverBaseUrlIsAnErrorOutcome() {
    var transport =
        HttpTransportStub.responding(200, "{\"m.homeserver\":{\"base_url\":\"not a url\"}}");
    var result = HomeserverDiscovery.discover(transport, "https://matrix.example.org");
    assertThat(result)
        .extracting(DiscoveredHomeserver::outcome, DiscoveredHomeserver::usedFallback)
        .containsExactly(DiscoveryOutcome.FAIL_ERROR, false);
  }

  @Test
  void discoversFromWellKnown() {
    var transport =
        HttpTransportStub.responding(
            200,
            """
            {"m.homeserver":{"base_url":"https://matrix.example.org:8448/"},
             "m.identity_server":{"base_url":"https://id.example.org"},
             "org.example.unknown":{"x":1}}
            """);
    var discovered = HomeserverDiscovery.discover(transport, "https://matrix.example.org");
    assertThat(discovered)
        .extracting(
            DiscoveredHomeserver::homeserverUrl,
            DiscoveredHomeserver::identityServerUrl,
            DiscoveredHomeserver::usedFallback)
        .containsExactly("https://matrix.example.org:8448", "https://id.example.org", false);
    assertThat(discovered.wellKnown().asObject().get("org.example.unknown")).isNotNull();
  }

  @ParameterizedTest
  @MethodSource("discoveryBasesAndExpectedUrls")
  void wellKnownRequestUsesHttpsHostnameOnly(String baseUrl, String expectedUrl) {
    var transport = HttpTransportStub.recording();
    HomeserverDiscovery.discover(transport, baseUrl);
    assertThat(transport.lastUrl()).isEqualTo(expectedUrl);
  }

  static List<org.junit.jupiter.params.provider.Arguments> discoveryBasesAndExpectedUrls() {
    return List.of(
        org.junit.jupiter.params.provider.Arguments.of(
            "http://matrix.example.org:8080/prefix",
            "https://matrix.example.org/.well-known/matrix/client"),
        org.junit.jupiter.params.provider.Arguments.of(
            "http://matrix.example.org:8448/base/path",
            "https://matrix.example.org/.well-known/matrix/client"),
        org.junit.jupiter.params.provider.Arguments.of(
            "https://matrix.example.org", "https://matrix.example.org/.well-known/matrix/client"),
        org.junit.jupiter.params.provider.Arguments.of(
            "https://matrix.example.org:443/prefix",
            "https://matrix.example.org/.well-known/matrix/client"));
  }

  @Test
  void discoveryRejectsBaseWithoutHostname() {
    var transport = HttpTransportStub.recording();
    assertThatThrownBy(() -> HomeserverDiscovery.discover(transport, "https:opaque-base"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("must contain a hostname");
  }

  @Test
  void wellKnown404IsIgnored() {
    var transport = HttpTransportStub.responding(404, "{}");
    var discovered = HomeserverDiscovery.discover(transport, "https://matrix.example.org");
    assertThat(discovered)
        .extracting(
            DiscoveredHomeserver::outcome,
            DiscoveredHomeserver::homeserverUrl,
            DiscoveredHomeserver::usedFallback)
        .containsExactly(DiscoveryOutcome.IGNORE, "https://matrix.example.org", true);
  }

  @Test
  void wellKnownFailureRequiresCallerDecision() {
    var transport = HttpTransportStub.failing();
    var discovered = HomeserverDiscovery.discover(transport, "https://matrix.example.org");
    assertThat(discovered)
        .extracting(
            DiscoveredHomeserver::outcome,
            DiscoveredHomeserver::usedFallback,
            DiscoveredHomeserver::failureReason)
        .containsExactly(DiscoveryOutcome.FAIL_PROMPT, false, "Well-known request failed");
    var failingBuilder =
        MatrixClient.builder("https://matrix.example.org")
            .transport(HttpTransportStub.failing())
            .discover();
    assertThatThrownBy(failingBuilder::build)
        .isInstanceOf(DiscoveryException.class)
        .hasMessageContaining("Well-known request failed");
  }

  static List<HttpTransportStub> invalidWellKnownResponses() {
    return List.of(
        HttpTransportStub.responding(500, "{\"errcode\":\"M_UNKNOWN\"}"),
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
    assertThat(discovered.outcome())
        .isIn(DiscoveryOutcome.FAIL_PROMPT, DiscoveryOutcome.FAIL_ERROR);
    assertThat(discovered.usedFallback()).isFalse();
    assertThat(discovered.homeserverUrl()).isEqualTo("https://matrix.example.org");
    assertThat(discovered.identityServerUrl()).isNull();
  }

  @Test
  void discoveryDistinguishesFailPromptFromFailError() {
    var missingHomeserver =
        HomeserverDiscovery.discover(
            HttpTransportStub.responding(200, "{}"), "https://matrix.example.org");
    assertThat(missingHomeserver.outcome()).isEqualTo(DiscoveryOutcome.FAIL_PROMPT);

    var invalidUrl =
        HomeserverDiscovery.discover(
            HttpTransportStub.responding(200, "{\"m.homeserver\":{\"base_url\":\"not a url\"}}"),
            "https://matrix.example.org");
    assertThat(invalidUrl.outcome()).isEqualTo(DiscoveryOutcome.FAIL_ERROR);
  }

  @Test
  void wellKnownUrlContainsTheRightPath() {
    var transport = HttpTransportStub.recording();
    HomeserverDiscovery.discover(transport, "https://matrix.example.org");
    assertThat(transport.lastUrl())
        .isEqualTo("https://matrix.example.org/.well-known/matrix/client");
  }
}
