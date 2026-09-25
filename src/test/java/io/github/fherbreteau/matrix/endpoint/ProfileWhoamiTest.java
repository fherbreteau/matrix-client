package io.github.fherbreteau.matrix.endpoint;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import io.github.fherbreteau.matrix.json.JsonParser;
import io.github.fherbreteau.matrix.model.PasswordCredentials;
import io.github.fherbreteau.matrix.model.UserId;
import io.github.fherbreteau.matrix.model.UserProfile;
import io.github.fherbreteau.matrix.model.WhoamiResponse;
import io.github.fherbreteau.matrix.transport.HttpTransport.Request;
import io.github.fherbreteau.matrix.transport.HttpTransport.Response;
import java.util.ArrayList;
import org.junit.jupiter.api.Test;

class ProfileWhoamiTest {

  private static HttpTransportStub queued(Response... responses) {
    var stub = new HttpTransportStub();
    for (Response response : responses) {
      stub.enqueue(response);
    }
    return stub;
  }

  private static final String LOGIN_OK =
      "{\"user_id\":\"@alice:matrix.org\",\"access_token\":\"secret-token\",\"device_id\":\"DEV\"}";

  @Test
  void profileFieldIsReadFromItsOwnEndpoint() {
    MatrixClient client =
        MatrixClient.builder("https://matrix.example.org")
            .transport(
                queued(
                    new Response(200, LOGIN_OK),
                    new Response(200, "{\"displayname\":\"Alice\"}"),
                    new Response(200, "{}")))
            .build();
    client.login(new PasswordCredentials("@alice:matrix.org", "s3cret"));
    var userId = UserId.of("@alice:matrix.org");
    assertThat(client.getProfileField(userId, "displayname")).contains("Alice");
    assertThat(client.getProfileField(userId, "avatar_url")).isEmpty();
  }

  @Test
  void profileFieldSetAndClearUseTheGenericEndpoint() {
    var requests = new ArrayList<Request>();
    var stub = new HttpTransportStub();
    stub.enqueue(new Response(200, LOGIN_OK));
    stub.enqueue(new Response(200, "{}"));
    stub.enqueue(new Response(200, "{}"));
    stub.enqueue(new Response(200, "{}"));
    stub.enqueue(new Response(200, "{}"));
    stub.recordInto(requests);
    MatrixClient client =
        MatrixClient.builder("https://matrix.example.org").transport(stub).build();
    client.login(new PasswordCredentials("@alice:matrix.org", "s3cret"));
    var userId = UserId.of("@alice:matrix.org");
    client.setDisplayName(userId, "Alice");
    client.setDisplayName(userId, null);
    client.setAvatarUrl(userId, "mxc://a");
    client.setAvatarUrl(userId, null);
    assertThat(requests.get(1).method()).isEqualTo("PUT");
    assertThat(requests.get(1).url())
        .endsWith("/_matrix/client/v3/profile/%40alice%3Amatrix.org/displayname");
    assertThat(requests.get(1).body()).isEqualTo("{\"displayname\":\"Alice\"}");
    assertThat(requests.get(2).method()).isEqualTo("DELETE");
    assertThat(requests.get(2).url())
        .endsWith("/_matrix/client/v3/profile/%40alice%3Amatrix.org/displayname");
    assertThat(requests.get(3).body()).isEqualTo("{\"avatar_url\":\"mxc://a\"}");
    assertThat(requests.get(4).method()).isEqualTo("DELETE");
  }

  @Test
  void fullProfileIsStillParsed() {
    MatrixClient client =
        MatrixClient.builder("https://matrix.example.org")
            .transport(
                queued(
                    new Response(200, LOGIN_OK),
                    new Response(200, "{\"displayname\":\"Alice\",\"avatar_url\":\"mxc://a\"}")))
            .build();
    client.login(new PasswordCredentials("@alice:matrix.org", "s3cret"));
    UserProfile profile = client.getProfile(UserId.of("@alice:matrix.org"));
    assertThat(profile.displayName()).isEqualTo("Alice");
    assertThat(profile.avatarUrl()).isEqualTo("mxc://a");
  }

  @Test
  void whoamiParsesIsGuest() {
    var resp = "{\"user_id\":\"@alice:matrix.org\",\"device_id\":\"DEV\",\"is_guest\":false}";
    MatrixClient client =
        MatrixClient.builder("https://matrix.example.org")
            .transport(queued(new Response(200, LOGIN_OK), new Response(200, resp)))
            .build();
    client.login(new PasswordCredentials("@alice:matrix.org", "s3cret"));
    WhoamiResponse whoami = client.whoami();
    assertThat(whoami.userId()).isEqualTo("@alice:matrix.org");
    assertThat(whoami.deviceId()).isEqualTo("DEV");
    assertThat(whoami.guest()).isFalse();
  }

  @Test
  void whoamiRejectsMissingUserId() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> WhoamiResponse.from(JsonParser.parse("{}")));
  }
}
