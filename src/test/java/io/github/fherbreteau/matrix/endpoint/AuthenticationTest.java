package io.github.fherbreteau.matrix.endpoint;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.InstanceOfAssertFactories.BOOLEAN;
import static org.assertj.core.api.InstanceOfAssertFactories.type;

import io.github.fherbreteau.matrix.error.AuthenticationException;
import io.github.fherbreteau.matrix.error.DiscoveryException;
import io.github.fherbreteau.matrix.error.MatrixServerException;
import io.github.fherbreteau.matrix.error.RateLimitedException;
import io.github.fherbreteau.matrix.model.InMemorySessionStore;
import io.github.fherbreteau.matrix.model.PasswordCredentials;
import io.github.fherbreteau.matrix.model.Session;
import io.github.fherbreteau.matrix.model.SessionStore;
import io.github.fherbreteau.matrix.transport.HttpTransport.Request;
import io.github.fherbreteau.matrix.transport.HttpTransport.Response;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class AuthenticationTest {

  private static final String LOGIN_OK =
      "{\"user_id\":\"@alice:matrix.org\",\"access_token\":\"secret-token\",\"device_id\":\"DEV\"}";

  private static final String LOGIN_REFRESHABLE =
      "{\"user_id\":\"@alice:matrix.org\",\"access_token\":\"secret-token\","
          + "\"refresh_token\":\"refresh-it\",\"expires_in_ms\":3600000,\"device_id\":\"DEV\"}";

  @Test
  void loginPostsPasswordFlowAndStoresSession() {
    var requests = new ArrayList<Request>();
    MatrixClient client =
        MatrixClient.builder("https://matrix.example.org")
            .transport(recording(new Response(200, LOGIN_OK), requests))
            .build();
    Session session = client.login(new PasswordCredentials("@alice:matrix.org", "s3cret"));
    assertThat(session)
        .extracting(Session::userId, Session::accessToken, Session::deviceId)
        .containsExactly("@alice:matrix.org", "secret-token", "DEV");
    assertThat(requests.getFirst().url())
        .isEqualTo("https://matrix.example.org/_matrix/client/v3/login");
    var body = requests.getFirst().body();
    assertThat(body).contains("m.login.password").contains("@alice:matrix.org");
    assertThat(client.getSession()).isPresent();
  }

  @Test
  void loginWithDeviceDisplayName() {
    var requests = new ArrayList<Request>();
    MatrixClient client =
        MatrixClient.builder("https://matrix.example.org")
            .transport(recording(new Response(200, LOGIN_OK), requests))
            .build();
    client.login(new PasswordCredentials("@alice:matrix.org", "s3cret"), "My Device");
    assertThat(requests.getFirst().body())
        .contains("initial_device_display_name")
        .contains("My Device");
  }

  @Test
  void invalidCredentialsRaiseAuthenticationException() {
    MatrixClient client =
        MatrixClient.builder("https://matrix.example.org")
            .transport(
                stub ->
                    new Response(
                        403, "{\"errcode\":\"M_FORBIDDEN\",\"error\":\"Invalid password\"}"))
            .build();
    var credentials = new PasswordCredentials("@alice:matrix.org", "wrong");
    assertThatThrownBy(() -> client.login(credentials))
        .isInstanceOf(AuthenticationException.class)
        .asInstanceOf(type(AuthenticationException.class))
        .extracting(AuthenticationException::getErrcode, AuthenticationException::getMessage)
        .containsExactly("M_FORBIDDEN", "Invalid password");
  }

  @Test
  void loginErrorMessageNeverContainsCredentials() {
    MatrixClient client =
        MatrixClient.builder("https://matrix.example.org")
            .transport(
                stub ->
                    new Response(
                        403, "{\"errcode\":\"M_FORBIDDEN\",\"error\":\"Invalid password\"}"))
            .build();
    try {
      var credentials = new PasswordCredentials("@alice:matrix.org", "top-secret-password");
      client.login(credentials);
    } catch (AuthenticationException e) {
      assertThat(e.getMessage()).doesNotContain("top-secret-password");
      assertThat(e.toString()).doesNotContain("top-secret-password");
    }
  }

  @Test
  void rateLimitedLoginIsNotSwallowed() {
    MatrixClient client =
        MatrixClient.builder("https://matrix.example.org")
            .transport(
                stub ->
                    new Response(
                        429,
                        Map.of("retry-after", "10"),
                        "{\"errcode\":\"M_LIMIT_EXCEEDED\",\"error\":\"Too many\"}",
                        10000L))
            .build();
    var credentials = new PasswordCredentials("@alice:matrix.org", "x");
    assertThatThrownBy(() -> client.login(credentials)).isInstanceOf(RateLimitedException.class);
  }

  @Test
  void malformedLoginResponseRaisesDiscoveryException() {
    MatrixClient client =
        MatrixClient.builder("https://matrix.example.org")
            .transport(stub -> new Response(200, "{\"user_id\":\"@a:b\"}"))
            .build();
    var credentials = new PasswordCredentials("@alice:matrix.org", "x");
    assertThatThrownBy(() -> client.login(credentials)).isInstanceOf(DiscoveryException.class);
  }

  @Test
  void logoutSendsBearerTokenAndClearsSession() {
    var requests = new ArrayList<Request>();
    MatrixClient client =
        MatrixClient.builder("https://matrix.example.org")
            .transport(recording(new Response(200, LOGIN_OK), new Response(200, "{}"), requests))
            .build();
    client.login(new PasswordCredentials("@alice:matrix.org", "s3cret"));
    client.logout();
    assertThat(requests.getLast().url()).endsWith("/_matrix/client/v3/logout");
    assertThat(requests.getLast().headers())
        .containsEntry(Request.AUTHORIZATION_HEADER, "Bearer secret-token");
    assertThat(client.getSession()).isEmpty();
  }

  @Test
  void logoutAllSendsBearerTokenAndClearsSession() {
    var requests = new ArrayList<Request>();
    MatrixClient client =
        MatrixClient.builder("https://matrix.example.org")
            .transport(recording(new Response(200, LOGIN_OK), new Response(200, "{}"), requests))
            .build();
    client.login(new PasswordCredentials("@alice:matrix.org", "s3cret"));
    client.logoutAll();
    assertThat(requests.getLast().url()).endsWith("/_matrix/client/v3/logout/all");
    assertThat(requests.getLast().headers())
        .containsEntry(Request.AUTHORIZATION_HEADER, "Bearer secret-token");
    assertThat(client.getSession()).isEmpty();
  }

  @Test
  void logoutWithoutSessionRaisesAuthenticationException() {
    MatrixClient client =
        MatrixClient.builder("https://matrix.example.org")
            .transport(stub -> new Response(200, "{}"))
            .build();
    assertThatExceptionOfType(AuthenticationException.class)
        .isThrownBy(client::logout)
        .withMessageContaining("No authenticated session");
  }

  @Test
  void logoutWithUnknownTokenRaisesAuthenticationException() {
    MatrixClient client =
        MatrixClient.builder("https://matrix.example.org")
            .transport(
                queued(
                    new Response(200, LOGIN_OK),
                    new Response(401, "{\"errcode\":\"M_UNKNOWN_TOKEN\",\"error\":\"Unknown\"}")))
            .build();
    client.login(new PasswordCredentials("@alice:matrix.org", "s3cret"));
    assertThatThrownBy(client::logout).isInstanceOf(AuthenticationException.class);
  }

  @Test
  void logoutServerFailureDoesNotClearSession() {
    MatrixClient client =
        MatrixClient.builder("https://matrix.example.org")
            .transport(
                queued(
                    new Response(200, LOGIN_OK),
                    new Response(500, "{\"errcode\":\"M_UNKNOWN\",\"error\":\"boom\"}")))
            .build();
    client.login(new PasswordCredentials("@alice:matrix.org", "s3cret"));
    assertThatThrownBy(client::logout).isInstanceOf(MatrixServerException.class);
    assertThat(client.getSession()).isPresent();
  }

  @Test
  void refreshResponseWithoutRefreshTokenCarriesOverThePreviousOne() {
    MatrixClient client =
        MatrixClient.builder("https://matrix.example.org")
            .transport(
                queued(
                    new Response(200, LOGIN_REFRESHABLE),
                    new Response(
                        200, "{\"access_token\":\"new-token\",\"expires_in_ms\":7200000}")))
            .build();
    client.login(new PasswordCredentials("@alice:matrix.org", "s3cret"), null, true);
    Session refreshed = client.refresh();
    assertThat(refreshed.accessToken()).isEqualTo("new-token");
    assertThat(refreshed.refreshToken()).isEqualTo("refresh-it");
    assertThat(refreshed.userId()).isEqualTo("@alice:matrix.org");
    assertThat(refreshed.deviceId()).isEqualTo("DEV");
    assertThat(client.getSession()).contains(refreshed);
  }

  @Test
  void injectableSessionStoreReceivesLifecycle() {
    var store = new RecordingSessionStore();
    MatrixClient client =
        MatrixClient.builder("https://matrix.example.org")
            .transport(queued(new Response(200, LOGIN_OK), new Response(200, "{}")))
            .sessionStore(store)
            .build();
    client.login(new PasswordCredentials("@alice:matrix.org", "s3cret"));
    assertThat(store.events).containsExactly("save");
    client.logout();
    assertThat(store.events).containsExactly("save", "clear");
  }

  @Test
  void loginTwiceReplacesTheSession() {
    var store = new InMemorySessionStore();
    MatrixClient client =
        MatrixClient.builder("https://matrix.example.org")
            .transport(
                queued(
                    new Response(200, LOGIN_OK),
                    new Response(
                        200, "{\"user_id\":\"@alice:matrix.org\",\"access_token\":\"t2\"}")))
            .sessionStore(store)
            .build();
    Session first = client.login(new PasswordCredentials("@alice:matrix.org", "s3cret"));
    Session second = client.login(new PasswordCredentials("@alice:matrix.org", "s3cret"));
    assertThat(second.accessToken()).isEqualTo("t2");
    assertThat(client.getSession()).contains(second);
    assertThat(store.current()).hasValue(second);
    assertThat(first.accessToken()).isNotEqualTo(second.accessToken());
  }

  @Test
  void refreshableLoginRequestsAndParsesRefreshToken() {
    var requests = new ArrayList<Request>();
    MatrixClient client =
        MatrixClient.builder("https://matrix.example.org")
            .transport(recording(new Response(200, LOGIN_REFRESHABLE), requests))
            .build();
    Session session =
        client.login(new PasswordCredentials("@alice:matrix.org", "s3cret"), null, true);
    assertThat(session)
        .extracting(Session::isRefreshable, Session::refreshToken, Session::expiresInMs)
        .containsExactly(true, "refresh-it", 3600000L);
    assertThat(requests.getFirst().body()).contains("\"refresh_token\":true");
  }

  @Test
  void refreshableLoginWithoutDeviceName() {
    var requests = new ArrayList<Request>();
    MatrixClient client =
        MatrixClient.builder("https://matrix.example.org")
            .transport(recording(new Response(200, LOGIN_REFRESHABLE), requests))
            .build();
    Session session = client.login(new PasswordCredentials("@alice:matrix.org", "s3cret"), true);
    assertThat(session).extracting(Session::isRefreshable, BOOLEAN).isTrue();
    assertThat(requests.getFirst().body()).contains("\"refresh_token\":true");
    assertThat(requests.getFirst().body()).doesNotContain("initial_device_display_name");
  }

  @Test
  void nonRefreshableLoginDoesNotRequestRefreshToken() {
    var requests = new ArrayList<Request>();
    MatrixClient client =
        MatrixClient.builder("https://matrix.example.org")
            .transport(recording(new Response(200, LOGIN_OK), requests))
            .build();
    Session session = client.login(new PasswordCredentials("@alice:matrix.org", "s3cret"));
    assertThat(session).extracting(Session::isRefreshable, BOOLEAN).isFalse();
    assertThat(requests.getFirst().body()).doesNotContain("refresh_token");
  }

  @Test
  void refreshRotatesTokensAndReplacesSession() {
    var requests = new ArrayList<Request>();
    MatrixClient client =
        MatrixClient.builder("https://matrix.example.org")
            .transport(
                recording(
                    new Response(200, LOGIN_REFRESHABLE),
                    new Response(
                        200,
                        "{\"user_id\":\"@alice:matrix.org\",\"access_token\":\"new-token\","
                            + "\"refresh_token\":\"new-refresh\",\"expires_in_ms\":7200000}"),
                    requests))
            .build();
    client.login(new PasswordCredentials("@alice:matrix.org", "s3cret"), null, true);
    Session refreshed = client.refresh();
    assertThat(refreshed)
        .extracting(Session::accessToken, Session::refreshToken, Session::expiresInMs)
        .containsExactly("new-token", "new-refresh", 7200000L);
    Request refreshRequest = requests.getLast();
    assertThat(refreshRequest.url()).endsWith("/_matrix/client/v3/refresh");
    assertThat(refreshRequest.headers()).isEmpty();
    assertThat(refreshRequest.body()).isEqualTo("{\"refresh_token\":\"refresh-it\"}");
    assertThat(client.getSession()).contains(refreshed);
  }

  @Test
  void refreshWithoutSessionRaisesAuthenticationException() {
    MatrixClient client =
        MatrixClient.builder("https://matrix.example.org")
            .transport(stub -> new Response(200, "{}"))
            .build();
    assertThatThrownBy(client::refresh)
        .isInstanceOf(AuthenticationException.class)
        .hasMessageContaining("No authenticated session");
  }

  @Test
  void refreshOfNonRefreshableSessionRaisesAuthenticationException() {
    MatrixClient client =
        MatrixClient.builder("https://matrix.example.org")
            .transport(queued(new Response(200, LOGIN_OK), new Response(200, "{}")))
            .build();
    client.login(new PasswordCredentials("@alice:matrix.org", "s3cret"));
    assertThatThrownBy(client::refresh)
        .isInstanceOf(AuthenticationException.class)
        .hasMessageContaining("not refreshable");
  }

  @Test
  void refreshWithInvalidRefreshTokenRaisesAuthenticationException() {
    MatrixClient client =
        MatrixClient.builder("https://matrix.example.org")
            .transport(
                queued(
                    new Response(200, LOGIN_REFRESHABLE),
                    new Response(403, "{\"errcode\":\"M_FORBIDDEN\",\"error\":\"Invalid grant\"}")))
            .build();
    client.login(new PasswordCredentials("@alice:matrix.org", "s3cret"), null, true);
    assertThatThrownBy(client::refresh)
        .isInstanceOf(AuthenticationException.class)
        .hasMessageContaining("Invalid grant");
  }

  private static HttpTransportStub recording(Response response, List<Request> requests) {
    var stub = new HttpTransportStub();
    stub.enqueue(response);
    stub.recordInto(requests);
    return stub;
  }

  private static HttpTransportStub recording(
      Response first, Response second, List<Request> requests) {
    var stub = new HttpTransportStub();
    stub.enqueue(first);
    stub.enqueue(second);
    stub.recordInto(requests);
    return stub;
  }

  private static HttpTransportStub queued(Response... responses) {
    var stub = new HttpTransportStub();
    for (Response response : responses) {
      stub.enqueue(response);
    }
    return stub;
  }

  private static final class RecordingSessionStore implements SessionStore {

    private final List<String> events = new ArrayList<>();
    private Session session;

    @Override
    public void save(Session session) {
      events.add("save");
      this.session = session;
    }

    @Override
    public Optional<Session> current() {
      return Optional.ofNullable(session);
    }

    @Override
    public void clear() {
      events.add("clear");
      session = null;
    }
  }
}
