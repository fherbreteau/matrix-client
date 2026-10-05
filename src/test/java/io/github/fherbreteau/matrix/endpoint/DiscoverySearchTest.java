package io.github.fherbreteau.matrix.endpoint;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.InstanceOfAssertFactories.type;

import io.github.fherbreteau.matrix.error.AuthenticationException;
import io.github.fherbreteau.matrix.error.MatrixServerException;
import io.github.fherbreteau.matrix.json.JsonParser;
import io.github.fherbreteau.matrix.model.MutualRoomsResponse;
import io.github.fherbreteau.matrix.model.PasswordCredentials;
import io.github.fherbreteau.matrix.model.RoomAlias;
import io.github.fherbreteau.matrix.model.RoomEventsSearchCriteria;
import io.github.fherbreteau.matrix.model.RoomId;
import io.github.fherbreteau.matrix.model.SearchRequest;
import io.github.fherbreteau.matrix.model.SpaceHierarchyOptions;
import io.github.fherbreteau.matrix.model.ThirdPartyLocations;
import io.github.fherbreteau.matrix.model.ThirdPartyProtocol;
import io.github.fherbreteau.matrix.model.ThirdPartyProtocols;
import io.github.fherbreteau.matrix.model.ThirdPartyUsers;
import io.github.fherbreteau.matrix.model.UserDirectorySearchRequest;
import io.github.fherbreteau.matrix.model.UserId;
import io.github.fherbreteau.matrix.transport.HttpTransport.Request;
import io.github.fherbreteau.matrix.transport.HttpTransport.Response;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
    RoomId unauthenticatedRoomId = RoomId.of("!room:example.org");
    assertThatThrownBy(() -> unauthenticated.getRoomSummary(unauthenticatedRoomId))
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

  @Test
  void thirdPartyLookupEndpointsEncodeRequestsAndPreserveRawFields() {
    var requests = new ArrayList<Request>();
    MatrixClient client =
        client(
            requests,
            new Response(200, LOGIN_OK),
            new Response(200, protocolMapJson()),
            new Response(200, protocolJson()),
            new Response(
                200,
                "[{\"alias\":\"#mapped:example.org\",\"fields\":{\"room\":\"matrix-spec\"},"
                    + "\"protocol\":\"gitter\",\"future\":true}]"),
            new Response(
                200,
                "[{\"alias\":\"#mapped:example.org\",\"fields\":{\"room\":\"matrix-spec\"},"
                    + "\"protocol\":\"gitter\"}]"),
            new Response(
                200,
                "[{\"fields\":{\"username\":\"@bob\"},\"protocol\":\"gitter\","
                    + "\"userid\":\"@bob:example.org\"}]"),
            new Response(
                200,
                "[{\"fields\":{\"username\":\"@bob\"},\"protocol\":\"gitter\","
                    + "\"userid\":\"@bob:example.org\"}]"));
    client.login(new PasswordCredentials("@alice:matrix.org", "password"));

    ThirdPartyProtocols protocols = client.getThirdPartyProtocols();
    ThirdPartyProtocol protocol = client.getThirdPartyProtocol("gitter/example");
    var aliasLocations = client.getThirdPartyLocations(RoomAlias.of("#matrix:example.org"));
    var fieldLocations = client.getThirdPartyLocations("gitter", Map.of("room", "matrix spec"));
    var mappedUsers = client.getThirdPartyUsers(UserId.of("@bob:example.org"));
    var fieldUsers = client.getThirdPartyUsers("gitter", Map.of("username", "@bob"));

    assertThat(protocols.protocols()).containsOnlyKeys("gitter");
    assertThat(protocol.raw().asObject().get("future_protocol_field").asBoolean()).isTrue();
    assertThat(aliasLocations.locations()).hasSize(1);
    assertThat(aliasLocations.locations().getFirst().raw().asObject().get("future").asBoolean())
        .isTrue();
    assertThat(fieldLocations.locations()).hasSize(1);
    assertThat(mappedUsers.users())
        .singleElement()
        .extracting(user -> user.userId())
        .isEqualTo(UserId.of("@bob:example.org"));
    assertThat(fieldUsers.users()).hasSize(1);
    assertThat(requests.get(1).url()).endsWith("/_matrix/client/v3/thirdparty/protocols");
    assertThat(requests.get(2).url())
        .endsWith("/_matrix/client/v3/thirdparty/protocol/gitter%2Fexample");
    assertThat(requests.get(3).url())
        .endsWith("/_matrix/client/v3/thirdparty/location?alias=%23matrix%3Aexample.org");
    assertThat(requests.get(4).url())
        .endsWith("/_matrix/client/v3/thirdparty/location/gitter?room=matrix%20spec");
    assertThat(requests.get(5).url())
        .endsWith("/_matrix/client/v3/thirdparty/user?userid=%40bob%3Aexample.org");
    assertThat(requests.get(6).url())
        .endsWith("/_matrix/client/v3/thirdparty/user/gitter?username=%40bob");
    assertThat(requests.getLast().headers()).containsEntry("Authorization", "Bearer secret-token");
  }

  @Test
  void thirdPartyLookupSupportsEmptyResultsAndRejectsMalformedResponses() {
    var requests = new ArrayList<Request>();
    MatrixClient client = client(requests, new Response(200, LOGIN_OK), new Response(200, "{}"));
    client.login(new PasswordCredentials("@alice:matrix.org", "password"));
    assertThat(client.getThirdPartyProtocols().protocols()).isEmpty();

    var malformedArray = JsonParser.parse("[]");
    var malformedObject = JsonParser.parse("{}");
    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(() -> ThirdPartyProtocols.from(malformedArray));
    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(() -> ThirdPartyProtocol.from(malformedObject));
    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(() -> ThirdPartyLocations.from(malformedObject));
    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(() -> ThirdPartyUsers.from(malformedObject));
  }

  @Test
  void thirdPartyLookupRequiresAuthenticationAndMapsServerErrors() {
    MatrixClient unauthenticated = MatrixClient.builder("https://matrix.example.org").build();
    RoomAlias alias = RoomAlias.of("#a:hs");
    UserId userId = UserId.of("@a:hs");
    assertThatThrownBy(unauthenticated::getThirdPartyProtocols)
        .isInstanceOf(AuthenticationException.class);
    assertThatThrownBy(() -> unauthenticated.getThirdPartyProtocol("irc"))
        .isInstanceOf(AuthenticationException.class);
    assertThatThrownBy(() -> unauthenticated.getThirdPartyLocations(alias))
        .isInstanceOf(AuthenticationException.class);
    assertThatThrownBy(() -> unauthenticated.getThirdPartyLocations("irc", Map.of()))
        .isInstanceOf(AuthenticationException.class);
    assertThatThrownBy(() -> unauthenticated.getThirdPartyUsers(userId))
        .isInstanceOf(AuthenticationException.class);
    assertThatThrownBy(() -> unauthenticated.getThirdPartyUsers("irc", Map.of()))
        .isInstanceOf(AuthenticationException.class);

    MatrixClient notFound =
        client(
            new ArrayList<>(),
            new Response(200, LOGIN_OK),
            new Response(404, "{\"errcode\":\"M_NOT_FOUND\",\"error\":\"unknown protocol\"}"));
    notFound.login(new PasswordCredentials("@alice:matrix.org", "password"));
    assertThatExceptionOfType(MatrixServerException.class)
        .isThrownBy(() -> notFound.getThirdPartyProtocol("unknown"))
        .asInstanceOf(type(MatrixServerException.class))
        .extracting(MatrixServerException::getErrcode)
        .isEqualTo("M_NOT_FOUND");
  }

  @Test
  void mutualRoomsUsesEncodedUserAndOpaquePagination() {
    var requests = new ArrayList<Request>();
    MatrixClient client =
        client(
            requests,
            new Response(200, LOGIN_OK),
            new Response(
                200,
                "{\"count\":2,\"joined\":[\"!one:example.org\"],"
                    + "\"next_batch\":\"opaque +/=\",\"future\":true}"),
            new Response(200, "{\"count\":2,\"joined\":[]}"));
    client.login(new PasswordCredentials("@alice:matrix.org", "password"));
    UserId target = UserId.of("@bob:example.org");

    MutualRoomsResponse firstPage = client.getMutualRooms(target);
    MutualRoomsResponse lastPage = client.getMutualRooms(target, firstPage.nextBatch());

    assertThat(firstPage.count()).isEqualTo(2);
    assertThat(firstPage.joined()).containsExactly(RoomId.of("!one:example.org"));
    assertThat(firstPage.hasNextBatch()).isTrue();
    assertThat(firstPage.raw().asObject().get("future").asBoolean()).isTrue();
    assertThat(lastPage.joined()).isEmpty();
    assertThat(lastPage.hasNextBatch()).isFalse();
    assertThat(requests.get(1).url())
        .endsWith("/_matrix/client/v1/mutual_rooms?user_id=%40bob%3Aexample.org");
    assertThat(requests.get(2).url())
        .endsWith(
            "/_matrix/client/v1/mutual_rooms?user_id=%40bob%3Aexample.org"
                + "&from=opaque%20%2B%2F%3D");
  }

  @Test
  void mutualRoomsRequiresAuthenticationAndPreservesServerErrors() {
    MatrixClient unauthenticated = MatrixClient.builder("https://matrix.example.org").build();
    assertThatThrownBy(() -> unauthenticated.getMutualRooms(UserId.of("@bob:example.org")))
        .isInstanceOf(AuthenticationException.class);

    MatrixClient client =
        client(
            new ArrayList<>(),
            new Response(200, LOGIN_OK),
            new Response(400, "{\"errcode\":\"M_INVALID_PARAM\",\"error\":\"bad from\"}"));
    client.login(new PasswordCredentials("@alice:matrix.org", "password"));
    assertThatExceptionOfType(MatrixServerException.class)
        .isThrownBy(() -> client.getMutualRooms(UserId.of("@bob:example.org"), "invalid"))
        .asInstanceOf(type(MatrixServerException.class))
        .extracting(MatrixServerException::getErrcode)
        .isEqualTo("M_INVALID_PARAM");
  }

  private static String protocolJson() {
    return "{\"field_types\":{\"room\":{\"placeholder\":\"room name\","
        + "\"regexp\":\"[^ ]+\"}},\"icon\":\"mxc://example.org/icon\","
        + "\"instances\":[{\"desc\":\"Gitter\",\"fields\":{},"
        + "\"network_id\":\"gitter\"}],\"location_fields\":[\"room\"],"
        + "\"user_fields\":[\"username\"],\"future_protocol_field\":true}";
  }

  private static String protocolMapJson() {
    return "{\"gitter\":" + protocolJson() + "}";
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
