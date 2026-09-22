package io.github.fherbreteau.matrix.endpoint;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.InstanceOfAssertFactories.type;

import io.github.fherbreteau.matrix.error.AuthenticationException;
import io.github.fherbreteau.matrix.error.MatrixServerException;
import io.github.fherbreteau.matrix.model.Direction;
import io.github.fherbreteau.matrix.model.EventId;
import io.github.fherbreteau.matrix.model.PasswordCredentials;
import io.github.fherbreteau.matrix.model.RoomAlias;
import io.github.fherbreteau.matrix.model.RoomEvent;
import io.github.fherbreteau.matrix.model.RoomId;
import io.github.fherbreteau.matrix.transport.HttpTransport.Response;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class EventRetrievalTest {

  private static final String LOGIN_OK =
      "{\"user_id\":\"@alice:matrix.org\",\"access_token\":\"secret-token\",\"device_id\":\"DEV\"}";

  @Test
  void singleEventIsRetrievedByItsIdentifier() {
    var requests = new ArrayList<io.github.fherbreteau.matrix.transport.HttpTransport.Request>();
    MatrixClient client =
        MatrixClient.builder("https://matrix.example.org")
            .transport(
                recording(
                    requests,
                    new Response(200, LOGIN_OK),
                    new Response(
                        200,
                        """
                        {"event_id":"$e1","sender":"@u:b","type":"m.room.message",
                         "origin_server_ts":1720000000000,"room_id":"!a:b",
                         "content":{"body":"hi"}}\
                        """)))
            .build();
    client.login(new PasswordCredentials("@alice:matrix.org", "s3cret"));
    RoomEvent event = client.getRoomEvent(RoomId.of("!a:b"), EventId.of("$e1"));
    assertThat(event)
        .extracting(RoomEvent::eventId, RoomEvent::sender, RoomEvent::type)
        .containsExactly("$e1", "@u:b", "m.room.message");
    assertThat(event).extracting(RoomEvent::originServerTs).isEqualTo(1720000000000L);
    assertThat(event).extracting(RoomEvent::roomId).isEqualTo("!a:b");
    assertThat(requests.getLast().url()).endsWith("/_matrix/client/v3/rooms/%21a%3Ab/event/%24e1");
  }

  @Test
  void timestampToEventReturnsTheClosestEvent() {
    var requests = new ArrayList<io.github.fherbreteau.matrix.transport.HttpTransport.Request>();
    MatrixClient client =
        MatrixClient.builder("https://matrix.example.org")
            .transport(
                recording(
                    requests,
                    new Response(200, LOGIN_OK),
                    new Response(200, "{\"event_id\":\"$e9\",\"origin_server_ts\":1720000005000}")))
            .build();
    client.login(new PasswordCredentials("@alice:matrix.org", "s3cret"));
    RoomEvent event =
        client.getEventForTimestamp(RoomId.of("!a:b"), 1720000000000L, Direction.FORWARD);
    assertThat(event).extracting(RoomEvent::eventId).isEqualTo("$e9");
    assertThat(event).extracting(RoomEvent::originServerTs).isEqualTo(1720000005000L);
    assertThat(requests.getLast().url())
        .contains("/_matrix/client/v1/rooms/%21a%3Ab/timestamp_to_event?ts=1720000000000&dir=f");
  }

  @Test
  void roomAliasesAreRetrieved() {
    MatrixClient client =
        MatrixClient.builder("https://matrix.example.org")
            .transport(
                queued(
                    new Response(200, LOGIN_OK),
                    new Response(
                        200, "{\"aliases\":[\"#general:matrix.org\",\"#dev:matrix.org\"]}")))
            .build();
    client.login(new PasswordCredentials("@alice:matrix.org", "s3cret"));
    var roomId = RoomId.of("!a:b");
    assertThat(client.getRoomAliases(roomId))
        .containsExactly(RoomAlias.of("#general:matrix.org"), RoomAlias.of("#dev:matrix.org"));
  }

  @Test
  void roomAliasesCanBeEmpty() {
    MatrixClient client =
        MatrixClient.builder("https://matrix.example.org")
            .transport(queued(new Response(200, LOGIN_OK), new Response(200, "{\"aliases\":[]}")))
            .build();
    client.login(new PasswordCredentials("@alice:matrix.org", "s3cret"));
    assertThat(client.getRoomAliases(RoomId.of("!a:b"))).isEmpty();
  }

  @Test
  void eventRetrievalWithoutSessionRaisesAuthenticationException() {
    MatrixClient client =
        MatrixClient.builder("https://matrix.example.org")
            .transport(stub -> new Response(200, "{}"))
            .build();
    var roomId = RoomId.of("!a:b");
    assertThatThrownBy(() -> client.getRoomEvent(roomId, EventId.of("$e1")))
        .isInstanceOf(AuthenticationException.class);
  }

  @Test
  void eventRetrievalServerErrorsAreMapped() {
    MatrixClient client =
        MatrixClient.builder("https://matrix.example.org")
            .transport(
                queued(
                    new Response(200, LOGIN_OK),
                    new Response(404, "{\"errcode\":\"M_NOT_FOUND\",\"error\":\"Not found\"}")))
            .build();
    client.login(new PasswordCredentials("@alice:matrix.org", "s3cret"));
    assertThatExceptionOfType(MatrixServerException.class)
        .isThrownBy(() -> client.getRoomEvent(RoomId.of("!a:b"), EventId.of("$missing")))
        .asInstanceOf(type(MatrixServerException.class))
        .extracting(MatrixServerException::getErrcode)
        .isEqualTo("M_NOT_FOUND");
  }

  private static HttpTransportStub recording(
      List<io.github.fherbreteau.matrix.transport.HttpTransport.Request> requests,
      Response... responses) {
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
