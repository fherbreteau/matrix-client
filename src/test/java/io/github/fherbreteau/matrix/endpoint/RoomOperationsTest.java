package io.github.fherbreteau.matrix.endpoint;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.InstanceOfAssertFactories.type;

import io.github.fherbreteau.matrix.error.AuthenticationException;
import io.github.fherbreteau.matrix.error.MatrixServerException;
import io.github.fherbreteau.matrix.model.JoinedMembers;
import io.github.fherbreteau.matrix.model.PasswordCredentials;
import io.github.fherbreteau.matrix.model.RoomAlias;
import io.github.fherbreteau.matrix.model.RoomAliasResolution;
import io.github.fherbreteau.matrix.model.RoomEvent;
import io.github.fherbreteau.matrix.model.RoomId;
import io.github.fherbreteau.matrix.model.UserId;
import io.github.fherbreteau.matrix.transport.HttpTransport.Request;
import io.github.fherbreteau.matrix.transport.HttpTransport.Response;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class RoomOperationsTest {

  private static final String LOGIN_OK =
      "{\"user_id\":\"@alice:matrix.org\",\"access_token\":\"secret-token\",\"device_id\":\"DEV\"}";

  @Test
  void createRoomReturnsTheRoomIdentifier() {
    var requests = new ArrayList<Request>();
    MatrixClient client =
        MatrixClient.builder("https://matrix.example.org")
            .transport(
                recording(
                    requests,
                    new Response(200, LOGIN_OK),
                    new Response(200, "{\"room_id\":\"!new:b\"}")))
            .build();
    client.login(new PasswordCredentials("@alice:matrix.org", "s3cret"));
    RoomId roomId = client.createRoom();
    assertThat(roomId).isEqualTo(RoomId.of("!new:b"));
    assertThat(requests.getLast().url()).endsWith("/_matrix/client/v3/createRoom");
    assertThat(requests.getLast().headers())
        .containsEntry(Request.AUTHORIZATION_HEADER, "Bearer secret-token");
  }

  @Test
  void createRoomSendsTheGivenParameters() {
    var requests = new ArrayList<Request>();
    MatrixClient client =
        MatrixClient.builder("https://matrix.example.org")
            .transport(
                recording(
                    requests,
                    new Response(200, LOGIN_OK),
                    new Response(200, "{\"room_id\":\"!new:b\"}")))
            .build();
    client.login(new PasswordCredentials("@alice:matrix.org", "s3cret"));
    RoomId roomId =
        client.createRoom(
            io.github.fherbreteau.matrix.model.RoomCreation.builder()
                .name("The Room")
                .topic("Everything")
                .visibility("public")
                .build());
    assertThat(roomId).isEqualTo(RoomId.of("!new:b"));
    assertThat(requests.getLast().body())
        .contains("\"name\":\"The Room\"")
        .contains("\"topic\":\"Everything\"")
        .contains("\"visibility\":\"public\"");
  }

  @Test
  void joinRoomByIdSendsTheEncodedIdentifier() {
    var requests = new ArrayList<Request>();
    MatrixClient client =
        MatrixClient.builder("https://matrix.example.org")
            .transport(
                recording(
                    requests,
                    new Response(200, LOGIN_OK),
                    new Response(200, "{\"room_id\":\"!a:b\"}")))
            .build();
    client.login(new PasswordCredentials("@alice:matrix.org", "s3cret"));
    assertThat(client.joinRoom(RoomId.of("!a:b"))).isEqualTo(RoomId.of("!a:b"));
    assertThat(requests.getLast().url()).endsWith("/_matrix/client/v3/join/%21a%3Ab");
  }

  @Test
  void joinRoomByAliasSendsTheEncodedAlias() {
    var requests = new ArrayList<Request>();
    MatrixClient client =
        MatrixClient.builder("https://matrix.example.org")
            .transport(
                recording(
                    requests,
                    new Response(200, LOGIN_OK),
                    new Response(200, "{\"room_id\":\"!a:b\"}")))
            .build();
    client.login(new PasswordCredentials("@alice:matrix.org", "s3cret"));
    assertThat(client.joinRoom(RoomAlias.of("#general:matrix.org"))).isEqualTo(RoomId.of("!a:b"));
    assertThat(requests.getLast().url())
        .endsWith("/_matrix/client/v3/join/%23general%3Amatrix.org");
  }

  @Test
  void leaveRoomPostsToTheLeaveEndpoint() {
    var requests = new ArrayList<Request>();
    MatrixClient client =
        MatrixClient.builder("https://matrix.example.org")
            .transport(recording(requests, new Response(200, LOGIN_OK), new Response(200, "{}")))
            .build();
    client.login(new PasswordCredentials("@alice:matrix.org", "s3cret"));
    client.leaveRoom(RoomId.of("!a:b"));
    assertThat(requests.getLast().url()).endsWith("/_matrix/client/v3/rooms/%21a%3Ab/leave");
  }

  @Test
  void inviteSendsTheUserId() {
    var requests = new ArrayList<Request>();
    MatrixClient client =
        MatrixClient.builder("https://matrix.example.org")
            .transport(recording(requests, new Response(200, LOGIN_OK), new Response(200, "{}")))
            .build();
    client.login(new PasswordCredentials("@alice:matrix.org", "s3cret"));
    client.invite(RoomId.of("!a:b"), UserId.of("@bob:matrix.org"));
    Request last = requests.getLast();
    assertThat(last.url()).endsWith("/_matrix/client/v3/rooms/%21a%3Ab/invite");
    assertThat(last.body()).isEqualTo("{\"user_id\":\"@bob:matrix.org\"}");
  }

  @Test
  void roomStatePreservesUnknownEventTypes() {
    MatrixClient client =
        MatrixClient.builder("https://matrix.example.org")
            .transport(
                queued(
                    new Response(200, LOGIN_OK),
                    new Response(
                        200,
                        """
                        [
                          {"type":"m.room.name","state_key":"","sender":"@u:b",
                           "content":{"name":"The Room"}},
                          {"type":"org.example.custom","state_key":"k1","sender":"@u:b",
                           "content":{"x":1}}
                        ]
                        """)))
            .build();
    client.login(new PasswordCredentials("@alice:matrix.org", "s3cret"));
    var roomId = RoomId.of("!a:b");
    List<RoomEvent> state = client.getRoomState(roomId);
    assertThat(state).hasSize(2);
    assertThat(state.getFirst().type()).isEqualTo("m.room.name");
    assertThat(state.getLast().type()).isEqualTo("org.example.custom");
    assertThat(state.getLast().content().asObject().get("x").asDouble()).isEqualTo(1.0);
  }

  @Test
  void joinedMembersAreParsed() {
    MatrixClient client =
        MatrixClient.builder("https://matrix.example.org")
            .transport(
                queued(
                    new Response(200, LOGIN_OK),
                    new Response(
                        200,
                        """
                        {"joined":{"@alice:matrix.org":{"display_name":"Alice"},
                                   "@bob:matrix.org":{}}}
                        """)))
            .build();
    client.login(new PasswordCredentials("@alice:matrix.org", "s3cret"));
    JoinedMembers members = client.getJoinedMembers(RoomId.of("!a:b"));
    assertThat(members.displayNameOf("@alice:matrix.org")).isEqualTo("Alice");
    assertThat(members.displayNameOf("@bob:matrix.org")).isNull();
  }

  @Test
  void roomNameIsReadFromState() {
    MatrixClient client =
        MatrixClient.builder("https://matrix.example.org")
            .transport(
                queued(new Response(200, LOGIN_OK), new Response(200, "{\"name\":\"The Room\"}")))
            .build();
    client.login(new PasswordCredentials("@alice:matrix.org", "s3cret"));
    assertThat(client.getRoomName(RoomId.of("!a:b"))).contains("The Room");
  }

  @Test
  void missingRoomNameYieldsEmpty() {
    MatrixClient client =
        MatrixClient.builder("https://matrix.example.org")
            .transport(
                queued(
                    new Response(200, LOGIN_OK),
                    new Response(
                        404, "{\"errcode\":\"M_NOT_FOUND\",\"error\":\"Event not found\"}")))
            .build();
    client.login(new PasswordCredentials("@alice:matrix.org", "s3cret"));
    assertThat(client.getRoomName(RoomId.of("!a:b"))).isEmpty();
  }

  @Test
  void canonicalAliasIsReadFromState() {
    MatrixClient client =
        MatrixClient.builder("https://matrix.example.org")
            .transport(
                queued(
                    new Response(200, LOGIN_OK),
                    new Response(200, "{\"alias\":\"#general:matrix.org\"}")))
            .build();
    client.login(new PasswordCredentials("@alice:matrix.org", "s3cret"));
    assertThat(client.getCanonicalAlias(RoomId.of("!a:b")))
        .contains(RoomAlias.of("#general:matrix.org"));
  }

  @Test
  void roomStateServerErrorsAreRethrown() {
    MatrixClient client =
        MatrixClient.builder("https://matrix.example.org")
            .transport(
                queued(
                    new Response(200, LOGIN_OK),
                    new Response(403, "{\"errcode\":\"M_FORBIDDEN\",\"error\":\"Not in room\"}")))
            .build();
    client.login(new PasswordCredentials("@alice:matrix.org", "s3cret"));
    var roomId = RoomId.of("!a:b");
    assertThatExceptionOfType(MatrixServerException.class)
        .isThrownBy(() -> client.getRoomName(roomId))
        .asInstanceOf(type(MatrixServerException.class))
        .extracting(MatrixServerException::getErrcode)
        .isEqualTo("M_FORBIDDEN");
  }

  @Test
  void resolveRoomAliasReturnsRoomIdAndServers() {
    var requests = new ArrayList<Request>();
    MatrixClient client =
        MatrixClient.builder("https://matrix.example.org")
            .transport(
                recording(
                    requests,
                    new Response(200, LOGIN_OK),
                    new Response(
                        200, "{\"room_id\":\"!a:b\",\"servers\":[\"matrix.org\",\"b.org\"]}")))
            .build();
    client.login(new PasswordCredentials("@alice:matrix.org", "s3cret"));
    RoomAliasResolution resolution = client.resolveRoomAlias(RoomAlias.of("#general:matrix.org"));
    assertThat(resolution.roomId()).isEqualTo(RoomId.of("!a:b"));
    assertThat(resolution.servers()).containsExactly("matrix.org", "b.org");
    assertThat(requests.getLast().url())
        .endsWith("/_matrix/client/v3/directory/room/%23general%3Amatrix.org");
  }

  @Test
  void roomOperationsWithoutSessionRaiseAuthenticationException() {
    MatrixClient client =
        MatrixClient.builder("https://matrix.example.org")
            .transport(stub -> new Response(200, "{}"))
            .build();
    assertThatThrownBy(client::createRoom).isInstanceOf(AuthenticationException.class);
    var roomId = RoomId.of("!a:b");
    assertThatThrownBy(() -> client.joinRoom(roomId)).isInstanceOf(AuthenticationException.class);
    assertThatThrownBy(() -> client.leaveRoom(roomId)).isInstanceOf(AuthenticationException.class);
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
