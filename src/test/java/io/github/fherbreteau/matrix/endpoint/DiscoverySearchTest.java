package io.github.fherbreteau.matrix.endpoint;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.InstanceOfAssertFactories.type;

import io.github.fherbreteau.matrix.error.AuthenticationException;
import io.github.fherbreteau.matrix.error.MatrixServerException;
import io.github.fherbreteau.matrix.model.PasswordCredentials;
import io.github.fherbreteau.matrix.model.RoomAlias;
import io.github.fherbreteau.matrix.model.RoomEventsSearchCriteria;
import io.github.fherbreteau.matrix.model.RoomId;
import io.github.fherbreteau.matrix.model.SearchRequest;
import io.github.fherbreteau.matrix.model.SpaceHierarchyOptions;
import io.github.fherbreteau.matrix.model.UserDirectorySearchRequest;
import io.github.fherbreteau.matrix.transport.HttpTransport.Request;
import io.github.fherbreteau.matrix.transport.HttpTransport.Response;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class DiscoverySearchTest {

  private static final String LOGIN_OK =
      "{\"user_id\":\"@alice:matrix.org\",\"access_token\":\"secret-token\",\"device_id\":\"DEV\"}";

  @Test
  void roomSummaryUsesEncodedIdentifierRepeatedViaParametersAndBearerAuth() {
    var requests = new ArrayList<Request>();
    MatrixClient client =
        client(
            requests,
            new Response(200, LOGIN_OK),
            new Response(
                200,
                "{\"room_id\":\"!room:example.org\",\"guest_can_join\":false,"
                    + "\"num_joined_members\":3,\"world_readable\":true}"),
            new Response(
                200,
                "{\"room_id\":\"!room:example.org\",\"guest_can_join\":false,"
                    + "\"num_joined_members\":3,\"world_readable\":true}"));
    client.login(new PasswordCredentials("@alice:matrix.org", "password"));
    var roomAlias = RoomAlias.of("#general:example.org");

    var summary = client.getRoomSummary(roomAlias, List.of("hint.example"));
    var idSummary = client.getRoomSummary(RoomId.of("!room:example.org"));

    assertThat(summary.roomId()).isEqualTo(RoomId.of("!room:example.org"));
    assertThat(idSummary.roomId()).isEqualTo(RoomId.of("!room:example.org"));
    assertThat(requests.get(1).method()).isEqualTo("GET");
    assertThat(requests.get(1).url())
        .endsWith("/_matrix/client/v1/room_summary/%23general%3Aexample.org?via=hint.example");
    assertThat(requests.get(1).headers()).containsEntry("Authorization", "Bearer secret-token");
    assertThat(requests.get(2).url())
        .endsWith("/_matrix/client/v1/room_summary/%21room%3Aexample.org");
  }

  @Test
  void hierarchyEncodesRoomIdAndSendsOptionalQueryOptions() {
    var requests = new ArrayList<Request>();
    MatrixClient client =
        client(
            requests,
            new Response(200, LOGIN_OK),
            new Response(200, "{\"rooms\":[],\"next_batch\":\"opaque+/=\"}"));
    client.login(new PasswordCredentials("@alice:matrix.org", "password"));

    var page =
        client.getSpaceHierarchy(
            RoomId.of("!space:example.org"), new SpaceHierarchyOptions("from +/", 20, 2, true));

    assertThat(page.nextBatch()).isEqualTo("opaque+/=");
    assertThat(requests.getLast().url())
        .endsWith(
            "/_matrix/client/v1/rooms/%21space%3Aexample.org/hierarchy"
                + "?from=from%20%2B%2F&limit=20&max_depth=2&suggested_only=true");
  }

  @Test
  void userDirectorySearchSendsTypedBodyAndParsesUnknownFields() {
    var requests = new ArrayList<Request>();
    MatrixClient client =
        client(
            requests,
            new Response(200, LOGIN_OK),
            new Response(
                200,
                "{\"limited\":false,\"results\":[{\"user_id\":\"@bob:example.org\","
                    + "\"display_name\":\"Bob\",\"x_custom\":true}],\"future\":1}"));
    client.login(new PasswordCredentials("@alice:matrix.org", "password"));

    var response = client.searchUsers(new UserDirectorySearchRequest("bob", 7));

    assertThat(response.results())
        .singleElement()
        .satisfies(
            user -> {
              assertThat(user.userId().value()).isEqualTo("@bob:example.org");
              assertThat(user.raw().asObject().get("x_custom").asBoolean()).isTrue();
            });
    assertThat(response.raw().asObject().get("future").asLong()).isEqualTo(1);
    assertThat(requests.getLast().method()).isEqualTo("POST");
    assertThat(requests.getLast().url()).endsWith("/_matrix/client/v3/user_directory/search");
    assertThat(requests.getLast().body()).isEqualTo("{\"search_term\":\"bob\",\"limit\":7}");
    assertThat(requests.getLast().headers()).containsEntry("Authorization", "Bearer secret-token");
  }

  @Test
  void serverSearchSendsBodyAndOpaquePaginationQuery() {
    var requests = new ArrayList<Request>();
    MatrixClient client =
        client(
            requests,
            new Response(200, LOGIN_OK),
            new Response(
                200, "{\"search_categories\":{\"room_events\":{\"count\":0,\"results\":[]}}}"));
    client.login(new PasswordCredentials("@alice:matrix.org", "password"));

    client.search(
        new SearchRequest(
            new RoomEventsSearchCriteria("words", null, "recent", null, null, null, null),
            "opaque +/="));

    assertThat(requests.getLast().url())
        .endsWith("/_matrix/client/v3/search?next_batch=opaque%20%2B%2F%3D");
    assertThat(requests.getLast().body())
        .isEqualTo(
            "{\"search_categories\":{\"room_events\":{\"search_term\":\"words\","
                + "\"order_by\":\"recent\"}}}");
  }

  @Test
  void convenienceOverloadsUseDefaultOptions() {
    var requests = new ArrayList<Request>();
    MatrixClient client =
        client(
            requests,
            new Response(200, LOGIN_OK),
            new Response(
                200,
                "{\"room_id\":\"!room:example.org\",\"guest_can_join\":false,"
                    + "\"num_joined_members\":3,\"world_readable\":true}"),
            new Response(
                200,
                "{\"room_id\":\"!room:example.org\",\"guest_can_join\":false,"
                    + "\"num_joined_members\":3,\"world_readable\":true}"),
            new Response(200, "{\"rooms\":[]}"),
            new Response(200, "{\"limited\":false,\"results\":[]}"));
    client.login(new PasswordCredentials("@alice:matrix.org", "password"));

    client.getRoomSummary(RoomId.of("!room:example.org"), List.of("hint.example"));
    client.getRoomSummary(RoomAlias.of("#general:example.org"));
    client.getSpaceHierarchy(RoomId.of("!space:example.org"));
    client.searchUsers("bob");

    assertThat(requests.get(1).url())
        .endsWith("/room_summary/%21room%3Aexample.org?via=hint.example");
    assertThat(requests.get(2).url()).endsWith("/room_summary/%23general%3Aexample.org");
    assertThat(requests.get(3).url()).endsWith("/rooms/%21space%3Aexample.org/hierarchy");
    assertThat(requests.get(4).body()).isEqualTo("{\"search_term\":\"bob\"}");
  }

  @Test
  void newEndpointsRequireAuthenticationAndPreserveServerErrors() {
    MatrixClient unauthenticated = MatrixClient.builder("https://matrix.example.org").build();
    RoomId spaceId = RoomId.of("!space:example.org");
    SearchRequest searchRequest =
        new SearchRequest(
            new RoomEventsSearchCriteria("x", null, null, null, null, null, null), null);
    assertThatThrownBy(() -> unauthenticated.getRoomSummary(RoomId.of("!room:example.org")))
        .isInstanceOf(AuthenticationException.class);
    assertThatThrownBy(() -> unauthenticated.getSpaceHierarchy(spaceId))
        .isInstanceOf(AuthenticationException.class);
    assertThatThrownBy(() -> unauthenticated.searchUsers("bob"))
        .isInstanceOf(AuthenticationException.class);
    assertThatThrownBy(() -> unauthenticated.search(searchRequest))
        .isInstanceOf(AuthenticationException.class);

    MatrixClient forbidden =
        client(
            new ArrayList<>(),
            new Response(200, LOGIN_OK),
            new Response(403, "{\"errcode\":\"M_FORBIDDEN\",\"error\":\"denied\"}"));
    forbidden.login(new PasswordCredentials("@alice:matrix.org", "password"));
    RoomId roomId = RoomId.of("!room:example.org");
    assertThatExceptionOfType(MatrixServerException.class)
        .isThrownBy(() -> forbidden.getRoomSummary(roomId))
        .asInstanceOf(type(MatrixServerException.class))
        .extracting(MatrixServerException::getErrcode)
        .isEqualTo("M_FORBIDDEN");
  }

  private static MatrixClient client(List<Request> requests, Response... responses) {
    var transport = new HttpTransportStub();
    for (Response response : responses) {
      transport.enqueue(response);
    }
    transport.recordInto(requests);
    return MatrixClient.builder("https://matrix.example.org").transport(transport).build();
  }
}
