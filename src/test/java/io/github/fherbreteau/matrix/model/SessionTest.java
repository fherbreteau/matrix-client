package io.github.fherbreteau.matrix.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.fherbreteau.matrix.error.DiscoveryException;
import io.github.fherbreteau.matrix.json.JsonParser;
import org.junit.jupiter.api.Test;

class SessionTest {

  private static final String LOGIN_BODY =
      """
      {"user_id":"@alice:matrix.org","access_token":"secret-token","device_id":"DEV123",
       "home_server":"matrix.org","expires_in_ms":3600000,"well_known":{}}
      """;

  @Test
  void parsesLoginResponseAndKeepsUnknownFields() {
    Session session = Session.from(JsonParser.parse(LOGIN_BODY));
    assertThat(session.userId()).isEqualTo("@alice:matrix.org");
    assertThat(session.accessToken()).isEqualTo("secret-token");
    assertThat(session.deviceId()).isEqualTo("DEV123");
    assertThat(session.homeserver()).isEqualTo("matrix.org");
    assertThat(session.raw().asObject().get("expires_in_ms").asLong()).isEqualTo(3600000L);
  }

  @Test
  void parsesMinimalLoginResponse() {
    Session session =
        Session.from(JsonParser.parse("{\"user_id\":\"@bob:x\",\"access_token\":\"t\"}"));
    assertThat(session.userId()).isEqualTo("@bob:x");
    assertThat(session.accessToken()).isEqualTo("t");
    assertThat(session.deviceId()).isNull();
    assertThat(session.homeserver()).isNull();
  }

  @Test
  void rejectsNullBody() {
    assertThatThrownBy(() -> Session.from(null))
        .isInstanceOf(DiscoveryException.class)
        .hasMessageContaining("JSON object");
  }

  @Test
  void rejectsNonObjectBody() {
    assertThatThrownBy(() -> Session.from(JsonParser.parse("[]")))
        .isInstanceOf(DiscoveryException.class)
        .hasMessageContaining("JSON object");
  }

  @Test
  void rejectsMissingAccessToken() {
    assertThatThrownBy(() -> Session.from(JsonParser.parse("{\"user_id\":\"@a:b\"}")))
        .isInstanceOf(DiscoveryException.class)
        .hasMessageContaining("access_token");
  }

  @Test
  void rejectsMissingUserId() {
    assertThatThrownBy(() -> Session.from(JsonParser.parse("{\"access_token\":\"t\"}")))
        .isInstanceOf(DiscoveryException.class)
        .hasMessageContaining("access_token");
  }

  @Test
  void toStringRedactsAccessTokenAndRawBody() {
    Session session = Session.from(JsonParser.parse(LOGIN_BODY));
    assertThat(session.toString())
        .contains("@alice:matrix.org")
        .contains("DEV123")
        .contains("accessToken=***")
        .doesNotContain("secret-token")
        .doesNotContain("expires_in_ms");
  }

  @Test
  void rejectsBlankCredentials() {
    assertThatIllegalArgumentException().isThrownBy(() -> new PasswordCredentials(null, "x"));
    assertThatIllegalArgumentException().isThrownBy(() -> new PasswordCredentials(" ", "x"));
    assertThatIllegalArgumentException().isThrownBy(() -> new PasswordCredentials("user", null));
    assertThatIllegalArgumentException().isThrownBy(() -> new PasswordCredentials("user", " "));
  }

  @Test
  void passwordCredentialsSerializeToLoginBody() {
    var credentials = new PasswordCredentials("@alice:matrix.org", "secret");
    var body = credentials.toJson();
    assertThat(body.asObject().get("type").asString()).isEqualTo("m.login.password");
    assertThat(body.asObject().get("identifier").asObject().get("user").asString())
        .isEqualTo("@alice:matrix.org");
    assertThat(body.asObject().get("password").asString()).isEqualTo("secret");
  }

  @Test
  void passwordCredentialsToStringRedactsPassword() {
    var credentials = new PasswordCredentials("@alice:matrix.org", "secret");
    assertThat(credentials.toString())
        .contains("@alice:matrix.org")
        .contains("password=***")
        .doesNotContain("secret");
  }

  @Test
  void inMemorySessionStoreIsDeterministic() {
    SessionStore store = SessionStore.create();
    assertThat(store.current()).isEmpty();
    Session first =
        Session.from(JsonParser.parse("{\"user_id\":\"@a:b\",\"access_token\":\"t1\"}"));
    Session second =
        Session.from(JsonParser.parse("{\"user_id\":\"@a:b\",\"access_token\":\"t2\"}"));
    store.save(first);
    assertThat(store.current()).contains(first);
    store.save(second);
    assertThat(store.current()).contains(second);
    store.clear();
    assertThat(store.current()).isEmpty();
    store.clear();
    assertThat(store.current()).isEmpty();
  }
}
