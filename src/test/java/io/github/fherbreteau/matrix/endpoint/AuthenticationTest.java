package io.github.fherbreteau.matrix.endpoint;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.InstanceOfAssertFactories.BOOLEAN;
import static org.assertj.core.api.InstanceOfAssertFactories.MAP;
import static org.assertj.core.api.InstanceOfAssertFactories.type;

import io.github.fherbreteau.matrix.error.AuthenticationException;
import io.github.fherbreteau.matrix.error.DiscoveryException;
import io.github.fherbreteau.matrix.error.MatrixServerException;
import io.github.fherbreteau.matrix.error.RateLimitedException;
import io.github.fherbreteau.matrix.json.JsonParser;
import io.github.fherbreteau.matrix.model.AccountRequest;
import io.github.fherbreteau.matrix.model.AuthenticationApi;
import io.github.fherbreteau.matrix.model.PasswordCredentials;
import io.github.fherbreteau.matrix.model.RegistrationRequest;
import io.github.fherbreteau.matrix.model.Session;
import io.github.fherbreteau.matrix.model.ThreePidTokenRequest;
import io.github.fherbreteau.matrix.store.SessionStore;
import io.github.fherbreteau.matrix.store.memory.InMemorySessionStore;
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
  void loginErrorRedactsCredentialsEchoedByTheHomeserver() {
    String password = "top-secret-password";
    String identifier = "@alice:matrix.org";
    MatrixClient client =
        MatrixClient.builder("https://matrix.example.org")
            .transport(
                stub ->
                    new Response(
                        403,
                        "{\"errcode\":\"M_FORBIDDEN\",\"error\":\"Rejected "
                            + identifier
                            + " with password "
                            + password
                            + "\"}"))
            .build();
    var credentials = new PasswordCredentials(identifier, password);
    assertThatThrownBy(() -> client.login(credentials))
        .isInstanceOf(AuthenticationException.class)
        .hasMessageNotContaining(password)
        .hasMessageNotContaining(identifier);
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

  @Test
  void usernameAvailabilityEncodesQueryAndPreservesUnknownFields() {
    var requests = new ArrayList<Request>();
    MatrixClient client =
        MatrixClient.builder("https://matrix.example.org")
            .transport(recording(new Response(200, "{\"available\":true,\"future\":1}"), requests))
            .build();
    var result = client.isUsernameAvailable("alice smith");
    assertThat(result.available()).isTrue();
    assertThat(result.raw().asObject().get("future").asLong()).isEqualTo(1L);
    assertThat(requests.getFirst().url())
        .isEqualTo(
            "https://matrix.example.org/_matrix/client/v3/register/available?username=alice%20smith");
    assertThat(requests.getFirst().headers()).isEmpty();
  }

  @Test
  void registrationSupportsAuthRetryAndInhibitLogin() {
    var requests = new ArrayList<Request>();
    HttpTransportStub transport =
        recording(
            new Response(401, "{\"flows\":[{\"stages\":[\"m.login.terms\"]}],\"session\":\"s\"}"),
            requests);
    transport.enqueue(new Response(200, "{\"user_id\":\"@alice:example.org\",\"future\":true}"));
    MatrixClient client =
        MatrixClient.builder("https://matrix.example.org").transport(transport).build();
    var first = RegistrationRequest.builder().username("alice").inhibitLogin(true).build();
    assertThatThrownBy(() -> client.register(first))
        .isInstanceOf(MatrixServerException.class)
        .asInstanceOf(type(MatrixServerException.class))
        .extracting(MatrixServerException::getFields)
        .asInstanceOf(MAP)
        .extractingByKey("flows")
        .isNotNull();
    var retry =
        RegistrationRequest.builder()
            .username("alice")
            .inhibitLogin(true)
            .put("auth", JsonParser.parse("{\"type\":\"m.login.terms\",\"session\":\"s\"}"))
            .build();
    var result = client.register(retry);
    assertThat(result.userId()).isEqualTo("@alice:example.org");
    assertThat(result.accessToken()).isNull();
    assertThat(result.raw().asObject().get("future").asBoolean()).isTrue();
    assertThat(requests).hasSize(2);
    assertThat(requests.getLast().body()).contains("m.login.terms").contains("inhibit_login");
    assertThat(requests.getLast().headers()).isEmpty();
  }

  @Test
  void registrationStoresSessionWhenHomeserverReturnsCredentials() {
    var requests = new ArrayList<Request>();
    MatrixClient client =
        MatrixClient.builder("https://matrix.example.org")
            .transport(
                recording(
                    new Response(
                        200,
                        "{\"user_id\":\"@alice:example.org\",\"access_token\":\"token\","
                            + "\"device_id\":\"D\"}"),
                    requests))
            .build();
    var result = client.register(RegistrationRequest.builder().username("alice").build());
    assertThat(result.accessToken()).isEqualTo("token");
    assertThat(client.getSession()).isPresent();
  }

  @Test
  void registrationTokenAndAuthMetadataUseTypedRawResponses() {
    var requests = new ArrayList<Request>();
    HttpTransportStub transport = recording(new Response(200, "{\"valid\":true}"), requests);
    transport.enqueue(new Response(200, "{\"issuer\":\"https://issuer\",\"extension\":1}"));
    MatrixClient client =
        MatrixClient.builder("https://matrix.example.org").transport(transport).build();
    assertThat(client.isRegistrationTokenValid("invite token").valid()).isTrue();
    var metadata = client.getAuthMetadata();
    assertThat(metadata.get("issuer").asString()).isEqualTo("https://issuer");
    assertThat(metadata.get("extension").asLong()).isEqualTo(1L);
    assertThat(metadata.fields()).containsKey("issuer");
    assertThat(requests.getFirst().url())
        .isEqualTo(
            "https://matrix.example.org/_matrix/client/v1/register/m.login.registration_token/validity?token=invite%20token");
    assertThat(requests.getLast().url()).endsWith("/_matrix/client/v1/auth_metadata");
  }

  @Test
  void passwordChangeWithoutSessionRaisesAuthenticationException() {
    MatrixClient client =
        MatrixClient.builder("https://matrix.example.org")
            .transport(stub -> new Response(200, "{}"))
            .build();
    var request = AccountRequest.builder().newPassword("next").build();
    assertThatThrownBy(() -> client.changePassword(request))
        .isInstanceOf(AuthenticationException.class)
        .hasMessageContaining("No authenticated session");
  }

  @Test
  void passwordChangeUsesBearerTokenAndAllowsUiAuthRetry() {
    var requests = new ArrayList<Request>();
    HttpTransportStub transport =
        recording(
            new Response(200, LOGIN_OK),
            new Response(
                401, "{\"flows\":[{\"stages\":[\"m.login.password\"]}],\"session\":\"s\"}"),
            requests);
    transport.enqueue(new Response(200, "{}"));
    MatrixClient client =
        MatrixClient.builder("https://matrix.example.org").transport(transport).build();
    client.login(new PasswordCredentials("@alice:matrix.org", "p"));
    var request = AccountRequest.builder().newPassword("new").build();
    assertThatThrownBy(() -> client.changePassword(request))
        .isInstanceOf(MatrixServerException.class)
        .asInstanceOf(type(MatrixServerException.class))
        .extracting(MatrixServerException::getUserInteractiveAuthChallenge)
        .isNotNull();
    var retry =
        AccountRequest.builder()
            .newPassword("new")
            .put("auth", JsonParser.parse("{\"type\":\"m.login.password\",\"session\":\"s\"}"))
            .build();
    client.changePassword(retry);
    assertThat(requests.getLast().url()).endsWith("/_matrix/client/v3/account/password");
    assertThat(requests.getLast().headers())
        .containsEntry(Request.AUTHORIZATION_HEADER, "Bearer secret-token");
    assertThat(requests.getLast().body()).contains("new_password").contains("m.login.password");
  }

  @Test
  void accountDeactivationClearsSessionAfterSuccess() {
    var requests = new ArrayList<Request>();
    MatrixClient client =
        MatrixClient.builder("https://matrix.example.org")
            .transport(
                recording(
                    new Response(200, LOGIN_OK),
                    new Response(200, "{\"id_server_unbind_result\":\"success\"}"),
                    requests))
            .build();
    client.login(new PasswordCredentials("@alice:matrix.org", "p"));
    var response = client.deactivateAccount(AccountRequest.builder().erase(true).build());
    assertThat(response.idServerUnbindResult()).isEqualTo("success");
    assertThat(client.getSession()).isEmpty();
    assertThat(requests.getLast().url()).endsWith("/_matrix/client/v3/account/deactivate");
    assertThat(requests.getLast().headers())
        .containsEntry(Request.AUTHORIZATION_HEADER, "Bearer secret-token");
  }

  @Test
  void thirdPartyIdentifierMethodsUseSpecPathsAndBearerAuthentication() {
    var requests = new ArrayList<Request>();
    HttpTransportStub transport = recording(new Response(200, LOGIN_OK), requests);
    transport.enqueue(new Response(200, "{\"threepids\":[]}"));
    transport.enqueue(new Response(200, "{}"));
    MatrixClient client =
        MatrixClient.builder("https://matrix.example.org").transport(transport).build();
    client.login(new PasswordCredentials("@alice:matrix.org", "p"));
    client.getThreePids();
    client.addThreePid(AccountRequest.builder().clientSecret("c").sessionId("sid").build());
    assertThat(requests.get(1).url()).endsWith("/_matrix/client/v3/account/3pid");
    assertThat(requests.getLast().url()).endsWith("/_matrix/client/v3/account/3pid/add");
    assertThat(requests.getLast().headers())
        .containsEntry(Request.AUTHORIZATION_HEADER, "Bearer secret-token");
  }

  @Test
  void emailTokenRequestUsesTypedResponseAndExactBody() {
    var requests = new ArrayList<Request>();
    MatrixClient client =
        MatrixClient.builder("https://matrix.example.org")
            .transport(
                recording(
                    new Response(200, "{\"sid\":\"sid1\",\"submit_url\":\"https://verify\"}"),
                    requests))
            .build();
    var response =
        client.requestRegistrationEmailToken(
            ThreePidTokenRequest.builder()
                .clientSecret("secret")
                .email("a@example.org")
                .sendAttempt(1)
                .build());
    assertThat(response)
        .extracting(
            io.github.fherbreteau.matrix.model.ThreePidTokenResponse::sid,
            io.github.fherbreteau.matrix.model.ThreePidTokenResponse::submitUrl)
        .containsExactly("sid1", "https://verify");
    assertThat(requests.getFirst().url())
        .endsWith("/_matrix/client/v3/register/email/requestToken");
    assertThat(requests.getFirst().body()).contains("a@example.org").contains("send_attempt");
    assertThat(requests.getFirst().headers()).isEmpty();
  }

  @Test
  void accountOperationsSupportBothBindingMutationsAndIdentifierManagement() {
    var requests = new ArrayList<Request>();
    HttpTransportStub transport = recording(new Response(200, LOGIN_OK), requests);
    transport.enqueue(
        new Response(
            200,
            "{\"threepids\":[{\"address\":\"a@example.org\",\"medium\":\"email\",\"added_at\":1,"
                + "\"validated_at\":2,\"extension\":true}]}"));
    transport.enqueue(new Response(200, "{}"));
    transport.enqueue(new Response(200, "{\"id_server_unbind_result\":\"success\"}"));
    transport.enqueue(new Response(200, "{\"id_server_unbind_result\":\"success\"}"));
    transport.enqueue(new Response(200, "{\"id_server_unbind_result\":\"no-support\"}"));
    MatrixClient client =
        MatrixClient.builder("https://matrix.example.org").transport(transport).build();
    client.login(new PasswordCredentials("@alice:matrix.org", "p"));
    var threepids = client.getThreePids();
    assertThat(threepids.threepids())
        .singleElement()
        .satisfies(
            threePid -> {
              assertThat(threePid.address()).isEqualTo("a@example.org");
              assertThat(threePid.medium()).isEqualTo("email");
              assertThat(threePid.addedAt()).isEqualTo(1L);
              assertThat(threePid.validatedAt()).isEqualTo(2L);
              assertThat(threePid.raw().asObject().get("extension").asBoolean()).isTrue();
            });
    var request =
        AccountRequest.builder()
            .clientSecret("secret")
            .sessionId("sid")
            .identityServer("id.example.org")
            .identityAccessToken("identity-token")
            .address("a@example.org")
            .medium("email")
            .build();
    client.bindThreePid(request);
    client.deleteThreePid(request);
    client.unbindThreePid(request);
    assertThat(requests.get(2).url()).endsWith("/_matrix/client/v3/account/3pid/bind");
    assertThat(requests.get(3).url()).endsWith("/_matrix/client/v3/account/3pid/delete");
    assertThat(requests.get(4).url()).endsWith("/_matrix/client/v3/account/3pid/unbind");
    assertThat(requests.getLast().headers())
        .containsEntry(Request.AUTHORIZATION_HEADER, "Bearer secret-token");
  }

  @Test
  void tokenRequestMethodsUseAllSupportedSpecPathsWithoutAuthentication() {
    var requests = new ArrayList<Request>();
    HttpTransportStub transport = recording(new Response(200, "{\"sid\":\"s\"}"), requests);
    for (int i = 0; i < 5; i++) {
      transport.enqueue(new Response(200, "{\"sid\":\"s\"}"));
    }
    MatrixClient client =
        MatrixClient.builder("https://matrix.example.org").transport(transport).build();
    var email =
        ThreePidTokenRequest.builder()
            .clientSecret("c")
            .email("a@example.org")
            .sendAttempt(1)
            .build();
    var phone =
        ThreePidTokenRequest.builder()
            .clientSecret("c")
            .country("US")
            .phoneNumber("5550000")
            .sendAttempt(2)
            .build();
    client.requestRegistrationEmailToken(email);
    client.requestRegistrationMsisdnToken(phone);
    client.requestThreePidEmailToken(email);
    client.requestThreePidMsisdnToken(phone);
    client.requestPasswordResetEmailToken(email);
    client.requestPasswordResetMsisdnToken(phone);
    assertThat(requests).hasSize(6);
    assertThat(requests)
        .extracting(Request::url)
        .containsExactly(
            "https://matrix.example.org/_matrix/client/v3/register/email/requestToken",
            "https://matrix.example.org/_matrix/client/v3/register/msisdn/requestToken",
            "https://matrix.example.org/_matrix/client/v3/account/3pid/email/requestToken",
            "https://matrix.example.org/_matrix/client/v3/account/3pid/msisdn/requestToken",
            "https://matrix.example.org/_matrix/client/v3/account/password/email/requestToken",
            "https://matrix.example.org/_matrix/client/v3/account/password/msisdn/requestToken");
    assertThat(requests).allSatisfy(request -> assertThat(request.headers()).isEmpty());
  }

  @Test
  void requestModelBuildersSerializeAllFieldsAndExtensions() {
    var registration =
        RegistrationRequest.builder()
            .username("alice")
            .password("secret")
            .deviceId("D")
            .initialDeviceDisplayName("Phone")
            .inhibitLogin(false)
            .requestRefreshToken(true)
            .putAll(Map.of("auth", JsonParser.parse("{\"type\":\"m.login.dummy\"}")))
            .build();
    assertThat(registration.toJson().asObject().names())
        .contains(
            "username",
            "password",
            "device_id",
            "initial_device_display_name",
            "inhibit_login",
            "refresh_token",
            "auth");
    var account =
        AccountRequest.builder()
            .newPassword("next")
            .logoutDevices(false)
            .erase(true)
            .identityServer("id.example.org")
            .clientSecret("c")
            .sessionId("sid")
            .identityAccessToken("id-token")
            .address("a@example.org")
            .medium("email")
            .putAll(Map.of("auth", JsonParser.parse("{}")))
            .build();
    assertThat(account.toJson().asObject().names())
        .contains(
            "new_password",
            "logout_devices",
            "erase",
            "id_server",
            "client_secret",
            "sid",
            "id_access_token",
            "address",
            "medium",
            "auth");
    var tokenRequest =
        ThreePidTokenRequest.builder()
            .clientSecret("c")
            .email("a@example.org")
            .country("GB")
            .phoneNumber("123")
            .sendAttempt(2)
            .identityServer("id.example.org")
            .identityAccessToken("id-token")
            .nextLink("https://next")
            .put("extension", JsonParser.parse("true"))
            .putAll(Map.of("another_extension", JsonParser.parse("false")))
            .build();
    assertThat(tokenRequest.toJson().asObject().names())
        .contains(
            "client_secret",
            "email",
            "country",
            "phone_number",
            "send_attempt",
            "id_server",
            "id_access_token",
            "next_link",
            "extension");
  }

  @Test
  void typedUiAuthChallengeExposesFlowsAndCompletedStages() {
    var requests = new ArrayList<Request>();
    MatrixClient client =
        MatrixClient.builder("https://matrix.example.org")
            .transport(
                recording(
                    new Response(
                        401,
                        "{\"flows\":[{\"stages\":[\"m.login.terms\",\"m.login.dummy\"],"
                            + "\"future\":1}],\"completed\":[\"m.login.terms\"],"
                            + "\"params\":{\"terms\":true},\"session\":\"s\"}"),
                    requests))
            .build();
    var request = RegistrationRequest.builder().build();
    assertThatThrownBy(() -> client.register(request))
        .isInstanceOf(MatrixServerException.class)
        .asInstanceOf(type(MatrixServerException.class))
        .extracting(MatrixServerException::getUserInteractiveAuthChallenge)
        .satisfies(
            challenge -> {
              assertThat(challenge.session()).isEqualTo("s");
              assertThat(challenge.completed()).containsExactly("m.login.terms");
              assertThat(challenge.flows())
                  .singleElement()
                  .extracting("stages")
                  .isEqualTo(List.of("m.login.terms", "m.login.dummy"));
              assertThat(challenge.raw().asObject().has("params")).isTrue();
            });
  }

  @Test
  void typedAccountModelsRejectMalformedRequiredFields() {
    var empty = JsonParser.parse("{}");
    assertThatThrownBy(
            () -> io.github.fherbreteau.matrix.model.RegistrationAvailability.from(empty))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(
            () -> io.github.fherbreteau.matrix.model.RegistrationTokenValidity.from(empty))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> io.github.fherbreteau.matrix.model.RegistrationResponse.from(empty))
        .isInstanceOf(DiscoveryException.class);
    assertThatThrownBy(() -> io.github.fherbreteau.matrix.model.ThreePidTokenResponse.from(empty))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(
            () -> io.github.fherbreteau.matrix.model.AccountOperationResponse.from(empty))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> io.github.fherbreteau.matrix.model.ThreePidResponse.from(empty))
        .isInstanceOf(IllegalArgumentException.class);
    var threepidsWithoutRequiredFields = JsonParser.parse("{\"threepids\":[{}]}");
    assertThatThrownBy(
            () ->
                io.github.fherbreteau.matrix.model.ThreePidResponse.from(
                    threepidsWithoutRequiredFields))
        .isInstanceOf(IllegalArgumentException.class);
    var invalidFlow = JsonParser.parse("{\"flows\":[{}]}");
    assertThatThrownBy(
            () -> io.github.fherbreteau.matrix.model.UserInteractiveAuthChallenge.from(invalidFlow))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void registrationKindAndDeactivationFailureBehaveAsSpecified() {
    var requests = new ArrayList<Request>();
    HttpTransportStub transport =
        recording(new Response(200, "{\"user_id\":\"@alice:example.org\"}"), requests);
    transport.enqueue(new Response(200, LOGIN_OK));
    transport.enqueue(new Response(500, "{\"errcode\":\"M_UNKNOWN\",\"error\":\"retry\"}"));
    MatrixClient client =
        MatrixClient.builder("https://matrix.example.org").transport(transport).build();
    client.register(RegistrationRequest.builder().build(), "guest");
    assertThat(requests.getFirst().url()).endsWith("/_matrix/client/v3/register?kind=guest");
    var registrationRequest = RegistrationRequest.builder().build();
    var invalidKind = "service";
    assertThatThrownBy(() -> client.register(registrationRequest, invalidKind))
        .isInstanceOf(IllegalArgumentException.class);
    client.login(new PasswordCredentials("@alice:matrix.org", "p"));
    var accountRequest = AccountRequest.builder().build();
    assertThatThrownBy(() -> client.deactivateAccount(accountRequest))
        .isInstanceOf(MatrixServerException.class);
    assertThat(client.getSession()).isPresent();
  }

  @Test
  void matrixServerExceptionReturnsNullWhenUiAuthChallengeIsAbsent() {
    var exception = new MatrixServerException(400, "M_BAD_JSON", "bad");
    assertThat(exception.getUserInteractiveAuthChallenge()).isNull();
  }

  @Test
  void oauthSessionProvenanceIsPreservedAcrossRefreshAndFilePersistence() {
    Session legacy = Session.from(JsonParser.parse(LOGIN_OK));
    Session oauth = Session.from(JsonParser.parse(LOGIN_OK), AuthenticationApi.OAUTH);
    assertThat(legacy.authenticationApi()).isEqualTo(AuthenticationApi.LEGACY);
    assertThat(oauth.usesOauth()).isTrue();
    assertThat(oauth.withAuthenticationApi(null).authenticationApi())
        .isEqualTo(AuthenticationApi.LEGACY);
    MatrixClient clientWithoutSession =
        MatrixClient.builder("https://matrix.example.org")
            .transport(stub -> new Response(200, "{}"))
            .build();
    assertThatThrownBy(clientWithoutSession::usesOauthSession)
        .isInstanceOf(AuthenticationException.class);
    Session legacyConstructorSession =
        new Session("@legacy:example.org", "access", null, null, null, null, null);
    assertThat(legacyConstructorSession.authenticationApi()).isEqualTo(AuthenticationApi.LEGACY);
    Session nullProvenanceSession =
        new Session("@legacy:example.org", "access", null, null, null, null, null, null);
    assertThat(nullProvenanceSession.authenticationApi()).isEqualTo(AuthenticationApi.LEGACY);
    var sessionStore = new InMemorySessionStore();
    sessionStore.save(oauth);
    var client =
        MatrixClient.builder("https://matrix.example.org")
            .transport(stub -> new Response(200, "{}"))
            .sessionStore(sessionStore)
            .build();
    assertThat(client.usesOauthSession()).isTrue();
    Session refreshed =
        Session.fromRefresh(
            oauth, JsonParser.parse("{\"access_token\":\"next\",\"refresh_token\":\"refresh\"}"));
    assertThat(refreshed.authenticationApi()).isEqualTo(AuthenticationApi.OAUTH);
  }

  @Test
  void oauthSessionCannotUseLegacyAccountManagementEndpoints() {
    var store = new InMemorySessionStore();
    store.save(Session.from(JsonParser.parse(LOGIN_OK), AuthenticationApi.OAUTH));
    var requests = new ArrayList<Request>();
    MatrixClient client =
        MatrixClient.builder("https://matrix.example.org")
            .transport(recording(new Response(200, "{}"), requests))
            .sessionStore(store)
            .build();
    var request = AccountRequest.builder().newPassword("next").build();
    assertThatThrownBy(() -> client.changePassword(request))
        .isInstanceOf(UnsupportedOperationException.class)
        .hasMessageContaining("account-management URL");
    var accountRequest = AccountRequest.builder().build();
    assertThatThrownBy(() -> client.deactivateAccount(accountRequest))
        .isInstanceOf(UnsupportedOperationException.class);
    assertThatThrownBy(client::getThreePids).isInstanceOf(UnsupportedOperationException.class);
    assertThatThrownBy(() -> client.addThreePid(accountRequest))
        .isInstanceOf(UnsupportedOperationException.class);
    assertThatThrownBy(() -> client.bindThreePid(accountRequest))
        .isInstanceOf(UnsupportedOperationException.class);
    assertThatThrownBy(() -> client.deleteThreePid(accountRequest))
        .isInstanceOf(UnsupportedOperationException.class);
    assertThatThrownBy(() -> client.unbindThreePid(accountRequest))
        .isInstanceOf(UnsupportedOperationException.class);
    var threePidTokenRequest = ThreePidTokenRequest.builder().build();
    assertThatThrownBy(() -> client.requestRegistrationEmailToken(threePidTokenRequest))
        .isInstanceOf(UnsupportedOperationException.class);
    assertThatThrownBy(() -> client.requestRegistrationMsisdnToken(threePidTokenRequest))
        .isInstanceOf(UnsupportedOperationException.class);
    assertThatThrownBy(() -> client.requestThreePidEmailToken(threePidTokenRequest))
        .isInstanceOf(UnsupportedOperationException.class);
    assertThatThrownBy(() -> client.requestThreePidMsisdnToken(threePidTokenRequest))
        .isInstanceOf(UnsupportedOperationException.class);
    assertThatThrownBy(() -> client.requestPasswordResetEmailToken(threePidTokenRequest))
        .isInstanceOf(UnsupportedOperationException.class);
    assertThatThrownBy(() -> client.requestPasswordResetMsisdnToken(threePidTokenRequest))
        .isInstanceOf(UnsupportedOperationException.class);
    assertThat(requests).isEmpty();
  }

  @Test
  void authMetadataHelperReturnsAccountManagementUri() {
    MatrixClient client =
        MatrixClient.builder("https://matrix.example.org")
            .transport(
                stub ->
                    new Response(
                        200, "{\"account_management_uri\":\"https://account.example.org/\"}"))
            .build();
    assertThat(client.getAccountManagementUri()).isEqualTo("https://account.example.org/");
  }

  @Test
  void authMetadataHelperReturnsNullWhenAccountManagementUriIsNotAdvertised() {
    MatrixClient client =
        MatrixClient.builder("https://matrix.example.org")
            .transport(stub -> new Response(200, "{\"issuer\":\"https://issuer.example.org\"}"))
            .build();
    assertThat(client.getAccountManagementUri()).isNull();
  }

  @Test
  void authMetadata404MeansOAuthIsUnsupported() {
    MatrixClient client =
        MatrixClient.builder("https://matrix.example.org")
            .transport(
                request ->
                    new Response(
                        404, "{\"errcode\":\"M_UNRECOGNIZED\",\"error\":\"Not supported\"}"))
            .build();
    assertThat(client.findAuthMetadata()).isEmpty();
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
