package io.github.fherbreteau.matrix.endpoint;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.InstanceOfAssertFactories.type;

import io.github.fherbreteau.matrix.error.AuthenticationException;
import io.github.fherbreteau.matrix.error.MatrixServerException;
import io.github.fherbreteau.matrix.json.JsonParser;
import io.github.fherbreteau.matrix.model.EventId;
import io.github.fherbreteau.matrix.model.PasswordCredentials;
import io.github.fherbreteau.matrix.model.Presence;
import io.github.fherbreteau.matrix.model.PresenceStatus;
import io.github.fherbreteau.matrix.model.ReadMarkers;
import io.github.fherbreteau.matrix.model.RoomId;
import io.github.fherbreteau.matrix.model.UserId;
import io.github.fherbreteau.matrix.transport.HttpTransport.Request;
import io.github.fherbreteau.matrix.transport.HttpTransport.Response;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class PresenceReceiptsTypingAccountDataTest {

  private static final String LOGIN_OK =
      "{\"user_id\":\"@alice:matrix.org\",\"access_token\":\"secret-token\",\"device_id\":\"DEV\"}";

  @Test
  void presenceStatusIsParsedWithAllFields() {
    var status =
        PresenceStatus.from(
            JsonParser.parse(
                "{\"presence\":\"online\",\"status_msg\":\"Hello\",\"last_active_ago\":1234,"
                    + "\"currently_active\":true,\"org.example.unknown\":1}"));
    assertThat(status.presence()).isEqualTo(Presence.ONLINE);
    assertThat(status.statusMessage()).isEqualTo("Hello");
    assertThat(status.lastActiveAgo()).isEqualTo(1234L);
    assertThat(status.currentlyActive()).isTrue();
    assertThat(status.raw().asObject().get("org.example.unknown").asDouble()).isEqualTo(1.0);
  }

  @Test
  void presenceUpdateSerializesPresenceAndOptionalMessage() {
    assertThat(PresenceStatus.of(Presence.UNAVAILABLE, "brb").toUpdate().toJson())
        .isEqualTo("{\"presence\":\"unavailable\",\"status_msg\":\"brb\"}");
    assertThat(PresenceStatus.of(Presence.OFFLINE, null).toUpdate().toJson())
        .isEqualTo("{\"presence\":\"offline\"}");
  }

  @Test
  void presenceRejectsUnknownValues() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> PresenceStatus.from(JsonParser.parse("{\"presence\":\"busy\"}")));
    assertThatIllegalArgumentException()
        .isThrownBy(() -> PresenceStatus.from(JsonParser.parse("{}")));
    assertThatIllegalArgumentException()
        .isThrownBy(() -> PresenceStatus.from(JsonParser.parse("[]")));
  }

  @Test
  void presenceIsRetrievedAndUpdated() {
    var requests = new ArrayList<Request>();
    MatrixClient client =
        MatrixClient.builder("https://matrix.example.org")
            .transport(
                recording(
                    requests,
                    new Response(200, LOGIN_OK),
                    new Response(
                        200,
                        "{\"presence\":\"unavailable\",\"status_msg\":\"brb\",\"last_active_ago\":50}"),
                    new Response(200, "{}")))
            .build();
    client.login(new PasswordCredentials("@alice:matrix.org", "s3cret"));
    assertThat(client.getPresence(UserId.of("@bob:matrix.org")).presence())
        .isEqualTo(Presence.UNAVAILABLE);
    client.setPresence(Presence.ONLINE, null);
    Request update = requests.getLast();
    assertThat(update.url()).endsWith("/_matrix/client/v3/presence/%40alice%3Amatrix.org/status");
    assertThat(update.body()).isEqualTo("{\"presence\":\"online\"}");
    assertThat(update.headers()).containsEntry(Request.AUTHORIZATION_HEADER, "Bearer secret-token");
  }

  @Test
  void receiptsAreSentWithOptionalThreadId() {
    var requests = new ArrayList<Request>();
    MatrixClient client =
        MatrixClient.builder("https://matrix.example.org")
            .transport(
                recording(
                    requests,
                    new Response(200, LOGIN_OK),
                    new Response(200, "{}"),
                    new Response(200, "{}")))
            .build();
    client.login(new PasswordCredentials("@alice:matrix.org", "s3cret"));
    var roomId = RoomId.of("!a:b");
    client.sendReceipt(roomId, "m.read", EventId.of("$e1"), null);
    client.sendReceipt(roomId, "m.read.private", EventId.of("$e2"), "$thread");
    assertThat(requests.get(1).url())
        .endsWith("/_matrix/client/v3/rooms/%21a%3Ab/receipt/m.read/%24e1");
    assertThat(requests.get(1).body()).isNull();
    assertThat(requests.get(2).url())
        .endsWith("/_matrix/client/v3/rooms/%21a%3Ab/receipt/m.read.private/%24e2");
    assertThat(requests.get(2).body()).isEqualTo("{\"thread_id\":\"$thread\"}");
  }

  @Test
  void readMarkersSerializeOnlyTheSetFields() {
    var requests = new ArrayList<Request>();
    MatrixClient client =
        MatrixClient.builder("https://matrix.example.org")
            .transport(
                recording(
                    requests,
                    new Response(200, LOGIN_OK),
                    new Response(200, "{}"),
                    new Response(200, "{}")))
            .build();
    client.login(new PasswordCredentials("@alice:matrix.org", "s3cret"));
    var roomId = RoomId.of("!a:b");
    client.sendReadMarkers(
        roomId, ReadMarkers.builder().fullyRead("$e3").read("$e3").readPrivate("$e3").build());
    assertThat(requests.getLast().url()).endsWith("/_matrix/client/v3/rooms/%21a%3Ab/read_markers");
    assertThat(requests.getLast().body())
        .isEqualTo("{\"m.fully_read\":\"$e3\",\"m.read\":\"$e3\",\"m.read.private\":\"$e3\"}");
    client.sendReadMarkers(roomId, ReadMarkers.builder().fullyRead("$e4").build());
    assertThat(requests.getLast().body()).isEqualTo("{\"m.fully_read\":\"$e4\"}");
  }

  @Test
  void typingIsSentWithOptionalTimeout() {
    var requests = new ArrayList<Request>();
    MatrixClient client =
        MatrixClient.builder("https://matrix.example.org")
            .transport(
                recording(
                    requests,
                    new Response(200, LOGIN_OK),
                    new Response(200, "{}"),
                    new Response(200, "{}")))
            .build();
    client.login(new PasswordCredentials("@alice:matrix.org", "s3cret"));
    var roomId = RoomId.of("!a:b");
    client.setTyping(roomId, true, 30000);
    client.setTyping(roomId, false, 0);
    assertThat(requests.get(1).url())
        .endsWith("/_matrix/client/v3/rooms/%21a%3Ab/typing/%40alice%3Amatrix.org");
    assertThat(requests.get(1).body()).isEqualTo("{\"typing\":true,\"timeout\":30000}");
    assertThat(requests.get(2).body()).isEqualTo("{\"typing\":false}");
  }

  @Test
  void globalAccountDataIsReadAndWritten() {
    var requests = new ArrayList<Request>();
    MatrixClient client =
        MatrixClient.builder("https://matrix.example.org")
            .transport(
                recording(
                    requests,
                    new Response(200, LOGIN_OK),
                    new Response(200, "{\"widgets\":[\"a\"]}"),
                    new Response(200, "{}"),
                    new Response(404, "{\"errcode\":\"M_NOT_FOUND\",\"error\":\"No data\"}")))
            .build();
    client.login(new PasswordCredentials("@alice:matrix.org", "s3cret"));
    assertThat(client.getAccountData("org.example.widgets").isPresent()).isTrue();
    client.setAccountData("org.example.widgets", JsonParser.parse("{\"widgets\":[\"a\",\"b\"]}"));
    assertThat(requests.get(2).url())
        .endsWith("/_matrix/client/v3/user/%40alice%3Amatrix.org/account_data/org.example.widgets");
    assertThat(requests.get(2).body()).isEqualTo("{\"widgets\":[\"a\",\"b\"]}");
    assertThat(client.getAccountData("org.example.missing")).isEmpty();
  }

  @Test
  void roomAccountDataIsScopedToTheRoom() {
    var requests = new ArrayList<Request>();
    MatrixClient client =
        MatrixClient.builder("https://matrix.example.org")
            .transport(
                recording(
                    requests,
                    new Response(200, LOGIN_OK),
                    new Response(200, "{\"color\":\"red\"}"),
                    new Response(200, "{}")))
            .build();
    client.login(new PasswordCredentials("@alice:matrix.org", "s3cret"));
    var roomId = RoomId.of("!a:b");
    assertThat(client.getRoomAccountData(roomId, "org.example.color").isPresent()).isTrue();
    client.setRoomAccountData(roomId, "org.example.color", JsonParser.parse("{\"color\":\"red\"}"));
    assertThat(requests.get(2).url())
        .endsWith(
            "/_matrix/client/v3/user/%40alice%3Amatrix.org/rooms/%21a%3Ab/account_data/org.example.color");
  }

  @Test
  void accountDataServerErrorsAreRethrown() {
    MatrixClient client =
        MatrixClient.builder("https://matrix.example.org")
            .transport(
                queued(
                    new Response(200, LOGIN_OK),
                    new Response(403, "{\"errcode\":\"M_FORBIDDEN\",\"error\":\"Wrong user\"}")))
            .build();
    client.login(new PasswordCredentials("@alice:matrix.org", "s3cret"));
    assertThatExceptionOfType(MatrixServerException.class)
        .isThrownBy(() -> client.getAccountData("org.example.widgets"))
        .asInstanceOf(type(MatrixServerException.class))
        .extracting(MatrixServerException::getErrcode)
        .isEqualTo("M_FORBIDDEN");
  }

  @Test
  void presenceAndAccountDataWithoutSessionRaiseAuthenticationException() {
    MatrixClient client =
        MatrixClient.builder("https://matrix.example.org")
            .transport(stub -> new Response(200, "{}"))
            .build();
    assertThatThrownBy(() -> client.setPresence(Presence.ONLINE, null))
        .isInstanceOf(AuthenticationException.class);
    assertThatThrownBy(() -> client.getAccountData("org.example.x"))
        .isInstanceOf(AuthenticationException.class);
    assertThatThrownBy(() -> client.setTyping(RoomId.of("!a:b"), true, 1000))
        .isInstanceOf(AuthenticationException.class);
  }

  private static HttpTransportStub recording(List<Request> requests, Response... responses) {
    var stub = new HttpTransportStub();
    for (Response response : responses) {
      stub.enqueue(response);
    }
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
}
