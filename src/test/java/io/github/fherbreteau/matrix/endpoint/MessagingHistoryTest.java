package io.github.fherbreteau.matrix.endpoint;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.InstanceOfAssertFactories.list;
import static org.assertj.core.api.InstanceOfAssertFactories.type;

import io.github.fherbreteau.matrix.error.AuthenticationException;
import io.github.fherbreteau.matrix.error.MatrixServerException;
import io.github.fherbreteau.matrix.model.Direction;
import io.github.fherbreteau.matrix.model.EventId;
import io.github.fherbreteau.matrix.model.MessageBody;
import io.github.fherbreteau.matrix.model.PasswordCredentials;
import io.github.fherbreteau.matrix.model.RoomEvent;
import io.github.fherbreteau.matrix.model.RoomId;
import io.github.fherbreteau.matrix.model.RoomMessagesPage;
import io.github.fherbreteau.matrix.transport.HttpTransport.Request;
import io.github.fherbreteau.matrix.transport.HttpTransport.Response;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class MessagingHistoryTest {

  private static final String LOGIN_OK =
      "{\"user_id\":\"@alice:matrix.org\",\"access_token\":\"secret-token\",\"device_id\":\"DEV\"}";

  @Test
  void sendTextPostsAnMRoomMessageEvent() {
    var requests = new ArrayList<Request>();
    MatrixClient client =
        MatrixClient.builder("https://matrix.example.org")
            .transport(
                recording(
                    requests,
                    new Response(200, LOGIN_OK),
                    new Response(200, "{\"event_id\":\"$t1\"}")))
            .build();
    client.login(new PasswordCredentials("@alice:matrix.org", "s3cret"));
    assertThat(client.sendText(RoomId.of("!a:b"), "Hello")).isEqualTo(EventId.of("$t1"));
    Request last = requests.getLast();
    assertThat(last.url()).contains("/_matrix/client/v3/rooms/%21a%3Ab/send/m.room.message/");
    assertThat(last.body()).isEqualTo("{\"msgtype\":\"m.text\",\"body\":\"Hello\"}");
  }

  @Test
  void repeatedSendTextUsesDifferentTransactionIdentifiers() {
    var requests = new ArrayList<Request>();
    MatrixClient client =
        MatrixClient.builder("https://matrix.example.org")
            .transport(
                recording(
                    requests,
                    new Response(200, LOGIN_OK),
                    new Response(200, "{\"event_id\":\"$t1\"}"),
                    new Response(200, "{\"event_id\":\"$t1\"}")))
            .build();
    client.login(new PasswordCredentials("@alice:matrix.org", "s3cret"));
    EventId first = client.sendText(RoomId.of("!a:b"), "Hello");
    EventId second = client.sendText(RoomId.of("!a:b"), "Hello");
    String firstTxn = transactionId(requests.get(1));
    String secondTxn = transactionId(requests.get(2));
    assertThat(firstTxn).isNotEqualTo(secondTxn);
    assertThat(first).isEqualTo(second);
  }

  @Test
  void explicitTransactionIdentifiersAreIdempotent() {
    var requests = new ArrayList<Request>();
    MatrixClient client =
        MatrixClient.builder("https://matrix.example.org")
            .transport(
                recording(
                    requests,
                    new Response(200, LOGIN_OK),
                    new Response(200, "{\"event_id\":\"$t1\"}"),
                    new Response(200, "{\"event_id\":\"$t1\"}")))
            .build();
    client.login(new PasswordCredentials("@alice:matrix.org", "s3cret"));
    EventId first =
        client.sendEvent(
            RoomId.of("!a:b"), "m.room.message", MessageBody.text("x").toJson(), "txn-42");
    EventId second =
        client.sendEvent(
            RoomId.of("!a:b"), "m.room.message", MessageBody.text("x").toJson(), "txn-42");
    assertThat(transactionId(requests.get(1))).isEqualTo("txn-42");
    assertThat(transactionId(requests.get(2))).isEqualTo("txn-42");
    assertThat(first).isEqualTo(second);
  }

  @Test
  void customMessageBodiesCarryFormattedContent() {
    var requests = new ArrayList<Request>();
    MatrixClient client =
        MatrixClient.builder("https://matrix.example.org")
            .transport(
                recording(
                    requests,
                    new Response(200, LOGIN_OK),
                    new Response(200, "{\"event_id\":\"$t1\"}")))
            .build();
    client.login(new PasswordCredentials("@alice:matrix.org", "s3cret"));
    MessageBody message =
        MessageBody.builder()
            .msgtype("m.notice")
            .body("hi *there*")
            .formattedBody("hi <b>there</b>")
            .format("org.matrix.custom.html")
            .build();
    assertThat(client.sendMessage(RoomId.of("!a:b"), message)).isEqualTo(EventId.of("$t1"));
    assertThat(requests.getLast().body())
        .isEqualTo(
            "{\"msgtype\":\"m.notice\",\"body\":\"hi *there*\","
                + "\"formatted_body\":\"hi <b>there</b>\",\"format\":\"org.matrix.custom.html\"}");
  }

  @Test
  void messageBodyRequiresABody() {
    assertThatIllegalArgumentException().isThrownBy(() -> MessageBody.builder().build());
    assertThatIllegalArgumentException().isThrownBy(() -> MessageBody.text(null));
    assertThat(MessageBody.notice("Careful"))
        .extracting(m -> m.toJson().asObject().get("msgtype").asString())
        .isEqualTo("m.notice");
  }

  @Test
  void customEventTypesAreSentVerbatim() {
    var requests = new ArrayList<Request>();
    MatrixClient client =
        MatrixClient.builder("https://matrix.example.org")
            .transport(
                recording(
                    requests,
                    new Response(200, LOGIN_OK),
                    new Response(200, "{\"event_id\":\"$c1\"}")))
            .build();
    client.login(new PasswordCredentials("@alice:matrix.org", "s3cret"));
    assertThat(
            client.sendEvent(
                RoomId.of("!a:b"),
                "org.example.custom",
                io.github.fherbreteau.matrix.json.JsonParser.parse("{\"x\":1}"),
                "txn-1"))
        .isEqualTo(EventId.of("$c1"));
    assertThat(requests.getLast().url()).contains("/send/org.example.custom/txn-1");
  }

  @Test
  void roomHistoryIsPaginatedWithTokens() {
    var requests = new ArrayList<Request>();
    MatrixClient client =
        MatrixClient.builder("https://matrix.example.org")
            .transport(
                recording(
                    requests,
                    new Response(200, LOGIN_OK),
                    new Response(
                        200,
                        """
                        {"start":"t0","end":"t1","chunk":[
                          {"event_id":"$e1","sender":"@u:b","type":"m.room.message",
                           "content":{"body":"older"}},
                          {"event_id":"$e2","sender":"@u:b","type":"org.example.custom",
                           "content":{"x":1}}]}\
                        """)))
            .build();
    client.login(new PasswordCredentials("@alice:matrix.org", "s3cret"));
    RoomMessagesPage page = client.getRoomMessages(RoomId.of("!a:b"), "t0", Direction.BACKWARD, 2);
    Request last = requests.getLast();
    assertThat(last.url())
        .contains("/_matrix/client/v3/rooms/%21a%3Ab/messages?from=t0&dir=b&limit=2");
    assertThat(page.start()).isEqualTo("t0");
    assertThat(page.end()).isEqualTo("t1");
    assertThat(page.hasEnd()).isTrue();
    assertThat(page).extracting(RoomMessagesPage::chunk, list(RoomEvent.class)).hasSize(2);
    assertThat(page.chunk().getFirst().content().asObject().get("body").asString())
        .isEqualTo("older");
    assertThat(page.chunk().getLast().type()).isEqualTo("org.example.custom");
  }

  @Test
  void extendedPaginationCarriesTheStopTokenAndFilter() {
    var requests = new ArrayList<Request>();
    MatrixClient client =
        MatrixClient.builder("https://matrix.example.org")
            .transport(
                recording(
                    requests,
                    new Response(200, LOGIN_OK),
                    new Response(200, "{\"start\":\"t0\",\"end\":\"t1\",\"chunk\":[]}")))
            .build();
    client.login(new PasswordCredentials("@alice:matrix.org", "s3cret"));
    var filter =
        io.github.fherbreteau.matrix.json.JsonParser.parse("{\"types\":[\"m.room.message\"]}");
    RoomMessagesPage page =
        client.getRoomMessages(RoomId.of("!a:b"), "t0", "t5", Direction.BACKWARD, 5, filter);
    Request last = requests.getLast();
    assertThat(last.url())
        .contains("from=t0&dir=b&to=t5&limit=5&filter=")
        .contains("%7B%22types%22%3A%5B%22m.room.message%22%5D%7D");
    assertThat(page.start()).isEqualTo("t0");
    assertThat(page.end()).isEqualTo("t1");
  }

  @Test
  void lazyLoadStateArrayIsParsedAsEvents() {
    MatrixClient client =
        MatrixClient.builder("https://matrix.example.org")
            .transport(
                queued(
                    new Response(200, LOGIN_OK),
                    new Response(
                        200,
                        """
                        {"start":"t0","chunk":[],
                         "state":[{"type":"m.room.member","state_key":"@u:b",
                                    "sender":"@u:b","content":{"membership":"join"}}]}\
                        """)))
            .build();
    client.login(new PasswordCredentials("@alice:matrix.org", "s3cret"));
    RoomMessagesPage page = client.getLatestRoomMessages(RoomId.of("!a:b"), 0);
    assertThat(page).extracting(RoomMessagesPage::state, list(RoomEvent.class)).hasSize(1);
    assertThat(page.state().getFirst().stateKey()).isEqualTo("@u:b");
  }

  @Test
  void latestHistoryOmitsTheFromToken() {
    var requests = new ArrayList<Request>();
    MatrixClient client =
        MatrixClient.builder("https://matrix.example.org")
            .transport(
                recording(
                    requests,
                    new Response(200, LOGIN_OK),
                    new Response(200, "{\"start\":\"t9\",\"chunk\":[]}")))
            .build();
    client.login(new PasswordCredentials("@alice:matrix.org", "s3cret"));
    RoomMessagesPage page = client.getLatestRoomMessages(RoomId.of("!a:b"), 10);
    assertThat(requests.getLast().url()).contains("/messages?dir=b&limit=10");
    assertThat(page.start()).isEqualTo("t9");
    assertThat(page.end()).isNull();
    assertThat(page.hasEnd()).isFalse();
  }

  @Test
  void forwardPaginationUsesTheEndTokenOfThePreviousPage() {
    var requests = new ArrayList<Request>();
    MatrixClient client =
        MatrixClient.builder("https://matrix.example.org")
            .transport(
                recording(
                    requests,
                    new Response(200, LOGIN_OK),
                    new Response(200, "{\"start\":\"t0\",\"end\":\"t1\",\"chunk\":[]}")))
            .build();
    client.login(new PasswordCredentials("@alice:matrix.org", "s3cret"));
    RoomMessagesPage page = client.getRoomMessages(RoomId.of("!a:b"), "t0", Direction.FORWARD, 0);
    assertThat(requests.getLast().url()).contains("from=t0&dir=f");
    assertThat(page.start()).isEqualTo("t0");
  }

  @Test
  void malformedHistoryResponsesRaiseIllegalArgumentException() {
    MatrixClient client =
        MatrixClient.builder("https://matrix.example.org")
            .transport(
                queued(
                    new Response(200, LOGIN_OK),
                    new Response(200, "{\"chunk\":[]}"),
                    new Response(200, "not-json")))
            .build();
    client.login(new PasswordCredentials("@alice:matrix.org", "s3cret"));
    var roomId = RoomId.of("!a:b");
    assertThatIllegalArgumentException().isThrownBy(() -> client.getLatestRoomMessages(roomId, 10));
  }

  @Test
  void historyServerErrorsAreMapped() {
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
        .isThrownBy(() -> client.getLatestRoomMessages(roomId, 10))
        .asInstanceOf(type(MatrixServerException.class))
        .extracting(MatrixServerException::getErrcode)
        .isEqualTo("M_FORBIDDEN");
  }

  @Test
  void historyWithoutSessionRaisesAuthenticationException() {
    MatrixClient client =
        MatrixClient.builder("https://matrix.example.org")
            .transport(stub -> new Response(200, "{}"))
            .build();
    var roomId = RoomId.of("!a:b");
    assertThatThrownBy(() -> client.getLatestRoomMessages(roomId, 10))
        .isInstanceOf(AuthenticationException.class);
  }

  private static String transactionId(Request request) {
    String url = request.url();
    int marker = url.indexOf("/send/");
    String tail = url.substring(marker + "/send/".length());
    return tail.substring(tail.indexOf('/') + 1);
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
