package io.github.fherbreteau.matrix.endpoint;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.InstanceOfAssertFactories.list;
import static org.assertj.core.api.InstanceOfAssertFactories.type;

import io.github.fherbreteau.matrix.error.AuthenticationException;
import io.github.fherbreteau.matrix.error.MatrixServerException;
import io.github.fherbreteau.matrix.json.JsonParser;
import io.github.fherbreteau.matrix.model.EventId;
import io.github.fherbreteau.matrix.model.PasswordCredentials;
import io.github.fherbreteau.matrix.model.PublicRoom;
import io.github.fherbreteau.matrix.model.PublicRoomsResponse;
import io.github.fherbreteau.matrix.model.RoomAlias;
import io.github.fherbreteau.matrix.model.RoomEvent;
import io.github.fherbreteau.matrix.model.RoomId;
import io.github.fherbreteau.matrix.model.UserId;
import io.github.fherbreteau.matrix.model.UserProfile;
import io.github.fherbreteau.matrix.model.WhoamiResponse;
import io.github.fherbreteau.matrix.transport.HttpTransport.Request;
import io.github.fherbreteau.matrix.transport.HttpTransport.Response;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class ClientServerOperationsTest {

  private static final String LOGIN_OK =
      "{\"user_id\":\"@alice:matrix.org\",\"access_token\":\"secret-token\",\"device_id\":\"DEV\"}";

  @Test
  void kickSendsUserIdAndOptionalReason() {
    var requests = new ArrayList<Request>();
    MatrixClient client =
        MatrixClient.builder("https://matrix.example.org")
            .transport(recording(requests, new Response(200, LOGIN_OK), new Response(200, "{}")))
            .build();
    client.login(new PasswordCredentials("@alice:matrix.org", "s3cret"));
    client.kick(RoomId.of("!a:b"), UserId.of("@bob:matrix.org"), "bye");
    Request last = requests.getLast();
    assertThat(last.url()).endsWith("/_matrix/client/v3/rooms/%21a%3Ab/kick");
    assertThat(last.body()).isEqualTo("{\"user_id\":\"@bob:matrix.org\",\"reason\":\"bye\"}");
  }

  @Test
  void banSendsUserIdWithoutReasonWhenAbsent() {
    var requests = new ArrayList<Request>();
    MatrixClient client =
        MatrixClient.builder("https://matrix.example.org")
            .transport(recording(requests, new Response(200, LOGIN_OK), new Response(200, "{}")))
            .build();
    client.login(new PasswordCredentials("@alice:matrix.org", "s3cret"));
    client.ban(RoomId.of("!a:b"), UserId.of("@bob:matrix.org"), null);
    assertThat(requests.getLast().body()).isEqualTo("{\"user_id\":\"@bob:matrix.org\"}");
  }

  @Test
  void unbanAndForgetPostToTheirEndpoints() {
    var requests = new ArrayList<Request>();
    MatrixClient client =
        MatrixClient.builder("https://matrix.example.org")
            .transport(
                recording(
                    requests,
                    new Response(200, LOGIN_OK),
                    new Response(200, "{}"),
                    new Response(200, "{}"),
                    new Response(200, "{}")))
            .build();
    client.login(new PasswordCredentials("@alice:matrix.org", "s3cret"));
    client.unban(RoomId.of("!a:b"), UserId.of("@bob:matrix.org"));
    client.forget(RoomId.of("!a:b"));
    assertThat(requests.get(1).url()).endsWith("/_matrix/client/v3/rooms/%21a%3Ab/unban");
    assertThat(requests.get(2).url()).endsWith("/_matrix/client/v3/rooms/%21a%3Ab/forget");
    assertThat(requests.get(1).body()).isEqualTo("{\"user_id\":\"@bob:matrix.org\"}");
  }

  @Test
  void getMembersParsesTheChunk() {
    MatrixClient client =
        MatrixClient.builder("https://matrix.example.org")
            .transport(
                queued(
                    new Response(200, LOGIN_OK),
                    new Response(
                        200,
                        """
                        {"chunk":[
                          {"type":"m.room.member","state_key":"@bob:matrix.org",
                           "sender":"@bob:matrix.org","content":{"membership":"join"}},
                          {"type":"m.room.member","state_key":"@carol:matrix.org",
                           "sender":"@carol:matrix.org","content":{"membership":"invite"}}
                        ]}
                        """)))
            .build();
    client.login(new PasswordCredentials("@alice:matrix.org", "s3cret"));
    List<RoomEvent> members = client.getMembers(RoomId.of("!a:b"));
    assertThat(members).hasSize(2);
    assertThat(members.getLast().content().asObject().get("membership").asString())
        .isEqualTo("invite");
  }

  @Test
  void joinedRoomsParsesTheRoomList() {
    MatrixClient client =
        MatrixClient.builder("https://matrix.example.org")
            .transport(
                queued(
                    new Response(200, LOGIN_OK),
                    new Response(200, "{\"joined_rooms\":[\"!a:b\",\"!c:d\"]}")))
            .build();
    client.login(new PasswordCredentials("@alice:matrix.org", "s3cret"));
    assertThat(client.getJoinedRooms()).extracting(RoomId::value).containsExactly("!a:b", "!c:d");
  }

  @Test
  void sendEventReturnsTheEventIdentifier() {
    var requests = new ArrayList<Request>();
    MatrixClient client =
        MatrixClient.builder("https://matrix.example.org")
            .transport(
                recording(
                    requests,
                    new Response(200, LOGIN_OK),
                    new Response(200, "{\"event_id\":\"$e1\"}")))
            .build();
    client.login(new PasswordCredentials("@alice:matrix.org", "s3cret"));
    EventId eventId =
        client.sendEvent(
            RoomId.of("!a:b"), "m.room.message", JsonParser.parse("{\"body\":\"hi\"}"), "txn-1");
    assertThat(eventId).isEqualTo(EventId.of("$e1"));
    Request last = requests.getLast();
    assertThat(last.method()).isEqualTo("PUT");
    assertThat(last.url()).endsWith("/_matrix/client/v3/rooms/%21a%3Ab/send/m.room.message/txn-1");
  }

  @Test
  void sendMessageEventGeneratesATransactionIdentifier() {
    var requests = new ArrayList<Request>();
    MatrixClient client =
        MatrixClient.builder("https://matrix.example.org")
            .transport(
                recording(
                    requests,
                    new Response(200, LOGIN_OK),
                    new Response(200, "{\"event_id\":\"$e1\"}")))
            .build();
    client.login(new PasswordCredentials("@alice:matrix.org", "s3cret"));
    assertThat(client.sendMessageEvent(RoomId.of("!a:b"), "m.room.message", JsonParser.parse("{}")))
        .isEqualTo(EventId.of("$e1"));
    assertThat(requests.getLast().url()).contains("/send/m.room.message/");
  }

  @Test
  void stateEventsCarryTheStateKey() {
    var requests = new ArrayList<Request>();
    MatrixClient client =
        MatrixClient.builder("https://matrix.example.org")
            .transport(
                recording(
                    requests,
                    new Response(200, LOGIN_OK),
                    new Response(200, "{\"event_id\":\"$e1\"}")))
            .build();
    client.login(new PasswordCredentials("@alice:matrix.org", "s3cret"));
    assertThat(
            client.sendStateEvent(
                RoomId.of("!a:b"),
                "m.room.member",
                "@bob:matrix.org",
                JsonParser.parse("{\"membership\":\"invite\"}")))
        .isEqualTo(EventId.of("$e1"));
    assertThat(requests.getLast().url())
        .endsWith("/_matrix/client/v3/rooms/%21a%3Ab/state/m.room.member/%40bob%3Amatrix.org");
  }

  @Test
  void stateEventsWithEmptyStateKeyUseTheTypedOverload() {
    MatrixClient client =
        MatrixClient.builder("https://matrix.example.org")
            .transport(
                queued(new Response(200, LOGIN_OK), new Response(200, "{\"event_id\":\"$e2\"}")))
            .build();
    client.login(new PasswordCredentials("@alice:matrix.org", "s3cret"));
    assertThat(
            client.sendStateEvent(
                RoomId.of("!a:b"), "m.room.topic", JsonParser.parse("{\"topic\":\"t\"}")))
        .isEqualTo(EventId.of("$e2"));
  }

  @Test
  void redactSendsTheReasonAndReturnsTheRedactionIdentifier() {
    var requests = new ArrayList<Request>();
    MatrixClient client =
        MatrixClient.builder("https://matrix.example.org")
            .transport(
                recording(
                    requests,
                    new Response(200, LOGIN_OK),
                    new Response(200, "{\"event_id\":\"$r1\"}")))
            .build();
    client.login(new PasswordCredentials("@alice:matrix.org", "s3cret"));
    assertThat(client.redact(RoomId.of("!a:b"), EventId.of("$e1"), "spam"))
        .isEqualTo(EventId.of("$r1"));
    Request last = requests.getLast();
    assertThat(last.url()).contains("/_matrix/client/v3/rooms/%21a%3Ab/redact/%24e1/");
    assertThat(last.body()).isEqualTo("{\"reason\":\"spam\"}");
  }

  @Test
  void profileIsParsedAndFieldsAreSet() {
    var requests = new ArrayList<Request>();
    MatrixClient client =
        MatrixClient.builder("https://matrix.example.org")
            .transport(
                recording(
                    requests,
                    new Response(200, LOGIN_OK),
                    new Response(200, "{\"displayname\":\"Alice\",\"avatar_url\":\"mxc://a\"}"),
                    new Response(200, "{}"),
                    new Response(200, "{}")))
            .build();
    client.login(new PasswordCredentials("@alice:matrix.org", "s3cret"));
    UserProfile profile = client.getProfile(UserId.of("@alice:matrix.org"));
    assertThat(profile.displayName()).isEqualTo("Alice");
    assertThat(profile.avatarUrl()).isEqualTo("mxc://a");
    client.setDisplayName(UserId.of("@alice:matrix.org"), "Alice");
    client.setAvatarUrl(UserId.of("@alice:matrix.org"), "mxc://a");
    assertThat(requests.get(2).url())
        .endsWith("/_matrix/client/v3/profile/%40alice%3Amatrix.org/displayname");
    assertThat(requests.get(3).url())
        .endsWith("/_matrix/client/v3/profile/%40alice%3Amatrix.org/avatar_url");
  }

  @Test
  void roomAliasesAreCreatedAndDeleted() {
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
    client.createRoomAlias(RoomAlias.of("#general:matrix.org"), RoomId.of("!a:b"));
    client.deleteRoomAlias(RoomAlias.of("#general:matrix.org"));
    Request created = requests.get(1);
    assertThat(created.method()).isEqualTo("PUT");
    assertThat(created.url()).endsWith("/_matrix/client/v3/directory/room/%23general%3Amatrix.org");
    assertThat(created.body()).isEqualTo("{\"room_id\":\"!a:b\"}");
    assertThat(requests.getLast().method()).isEqualTo("DELETE");
  }

  @Test
  void publicRoomsAreParsedWithUnknownFieldsPreserved() {
    MatrixClient client =
        MatrixClient.builder("https://matrix.example.org")
            .transport(
                queued(
                    new Response(
                        200,
                        """
                        {"chunk":[{"room_id":"!a:b","name":"The Room","guest_can_join":false,
                                    "org.example.custom":true}],
                         "total_room_count_estimate":42,"next_batch":"NEXT"}
                        """)))
            .build();
    PublicRoomsResponse rooms = client.getPublicRooms();
    assertThat(rooms).extracting(PublicRoomsResponse::totalRoomCountEstimate).isEqualTo(42L);
    assertThat(rooms).extracting(PublicRoomsResponse::nextBatch).isEqualTo("NEXT");
    assertThat(rooms).extracting(PublicRoomsResponse::chunk, list(PublicRoom.class)).hasSize(1);
    PublicRoom room = rooms.chunk().getFirst();
    assertThat(room.roomId()).isEqualTo(RoomId.of("!a:b"));
    assertThat(room.name()).isEqualTo("The Room");
    assertThat(room.guestCanJoin()).isFalse();
    assertThat(room.raw().asObject().get("org.example.custom").asBoolean()).isTrue();
  }

  @Test
  void filteredPublicRoomsSendsTheSearchBody() {
    var requests = new ArrayList<Request>();
    MatrixClient client =
        MatrixClient.builder("https://matrix.example.org")
            .transport(
                recording(
                    requests, new Response(200, LOGIN_OK), new Response(200, "{\"chunk\":[]}")))
            .build();
    client.login(new PasswordCredentials("@alice:matrix.org", "s3cret"));
    PublicRoomsResponse rooms = client.getPublicRooms(10, "general", null);
    assertThat(rooms).extracting(PublicRoomsResponse::chunk, list(PublicRoom.class)).isEmpty();
    assertThat(requests.getLast().body())
        .isEqualTo("{\"limit\":10,\"filter\":{\"generic_search_term\":\"general\"}}");
  }

  @Test
  void whoamiReturnsTheTokenOwner() {
    MatrixClient client =
        MatrixClient.builder("https://matrix.example.org")
            .transport(
                queued(
                    new Response(200, LOGIN_OK),
                    new Response(200, "{\"user_id\":\"@alice:matrix.org\",\"device_id\":\"DEV\"}")))
            .build();
    client.login(new PasswordCredentials("@alice:matrix.org", "s3cret"));
    assertThat(client.whoami()).extracting(WhoamiResponse::userId).isEqualTo("@alice:matrix.org");
  }

  @Test
  void operationsWithoutSessionRaiseAuthenticationException() {
    MatrixClient client =
        MatrixClient.builder("https://matrix.example.org")
            .transport(stub -> new Response(200, "{}"))
            .build();
    assertThatThrownBy(client::whoami).isInstanceOf(AuthenticationException.class);
    var roomId = RoomId.of("!a:b");
    assertThatThrownBy(() -> client.getRoomState(roomId))
        .isInstanceOf(AuthenticationException.class);
    assertThatThrownBy(() -> client.leaveRoom(roomId)).isInstanceOf(AuthenticationException.class);
  }

  @Test
  void serverErrorsMapToMatrixServerException() {
    MatrixClient client =
        MatrixClient.builder("https://matrix.example.org")
            .transport(
                queued(
                    new Response(200, LOGIN_OK),
                    new Response(403, "{\"errcode\":\"M_FORBIDDEN\",\"error\":\"Not allowed\"}")))
            .build();
    client.login(new PasswordCredentials("@alice:matrix.org", "s3cret"));
    var roomId = RoomId.of("!a:b");
    var userId = UserId.of("@bob:matrix.org");
    assertThatExceptionOfType(MatrixServerException.class)
        .isThrownBy(() -> client.kick(roomId, userId, null))
        .asInstanceOf(type(MatrixServerException.class))
        .extracting(MatrixServerException::getErrcode)
        .isEqualTo("M_FORBIDDEN");
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
