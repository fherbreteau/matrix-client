package io.github.fherbreteau.matrix.endpoint;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.InstanceOfAssertFactories.type;

import io.github.fherbreteau.matrix.error.AuthenticationException;
import io.github.fherbreteau.matrix.error.MatrixServerException;
import io.github.fherbreteau.matrix.json.JsonParser;
import io.github.fherbreteau.matrix.model.CrossSigningKey;
import io.github.fherbreteau.matrix.model.DeviceId;
import io.github.fherbreteau.matrix.model.DeviceInformation;
import io.github.fherbreteau.matrix.model.DeviceSigningUploadRequest;
import io.github.fherbreteau.matrix.model.EncryptionRequest;
import io.github.fherbreteau.matrix.model.EncryptionRequest.KeyBackupData;
import io.github.fherbreteau.matrix.model.EncryptionRequest.RoomKeyBackup;
import io.github.fherbreteau.matrix.model.KeySignaturesUploadRequest;
import io.github.fherbreteau.matrix.model.KeysClaimRequest;
import io.github.fherbreteau.matrix.model.KeysClaimResponse;
import io.github.fherbreteau.matrix.model.KeysQueryRequest;
import io.github.fherbreteau.matrix.model.KeysUploadRequest;
import io.github.fherbreteau.matrix.model.MutualRoomsResponse;
import io.github.fherbreteau.matrix.model.PasswordCredentials;
import io.github.fherbreteau.matrix.model.RoomAlias;
import io.github.fherbreteau.matrix.model.RoomEventsSearchCriteria;
import io.github.fherbreteau.matrix.model.RoomId;
import io.github.fherbreteau.matrix.model.RoomKeyBackupVersionRequest;
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
    UserId targetUser = UserId.of("@bob:example.org");
    assertThatThrownBy(() -> unauthenticated.getMutualRooms(targetUser))
        .isInstanceOf(AuthenticationException.class);

    MatrixClient client =
        client(
            new ArrayList<>(),
            new Response(200, LOGIN_OK),
            new Response(400, "{\"errcode\":\"M_INVALID_PARAM\",\"error\":\"bad from\"}"));
    client.login(new PasswordCredentials("@alice:matrix.org", "password"));
    String invalidToken = "invalid";
    assertThatExceptionOfType(MatrixServerException.class)
        .isThrownBy(() -> client.getMutualRooms(targetUser, invalidToken))
        .asInstanceOf(type(MatrixServerException.class))
        .extracting(MatrixServerException::getErrcode)
        .isEqualTo("M_INVALID_PARAM");
  }

  @Test
  void keyUploadQueryClaimAndChangesMatchMatrixPathsAndParseMaps() {
    var requests = new ArrayList<Request>();
    MatrixClient client =
        client(
            requests,
            new Response(200, LOGIN_OK),
            new Response(200, "{}"),
            new Response(200, "{\"one_time_key_counts\":{\"signed_curve25519\":17}}"),
            new Response(
                200,
                "{\"device_keys\":{\"@bob:example.org\":{\"D\":{\"algorithms\":[\"m.megolm.v1.aes-"
                    + "sha2\"],\"device_id\":\"D\",\"keys\":{\"ed25519:D\":\"opaque\"},\"signature"
                    + "s\":{\"@bob:example.org\":{\"ed25519:D\":\"signature\"}},\"user_id\":\"@bob"
                    + ":example.org\",\"unsigned\":{\"device_display_name\":\"Phone\"}}}},\"failur"
                    + "es\":{\"@other:hs\":{\"errcode\":\"M_TIMEOUT\",\"error\":\"timeout\"}}}"),
            new Response(
                200,
                "{\"device_keys\":{},\"failures\":{\"remote\":{\"errcode\":\"M_TIMEOUT\",\"error\""
                    + ":\"timeout\"}},\"master_keys\":{\"@bob:example.org\":{\"keys\":{\"ed25519:D"
                    + "\":\"opaque\"},\"usage\":[\"master\"],\"user_id\":\"@bob:example.org\"}},\""
                    + "self_signing_keys\":{},\"user_signing_keys\":{}}"),
            new Response(
                200,
                "{\"one_time_keys\":{\"@bob:example.org\":{\"D\":{\"curve25519:k\":\"public\"}}},"
                    + "\"failures\":{\"remote\":{\"errcode\":\"M_TIMEOUT\",\"error\":\"timeout\"}"
                    + "}}"),
            new Response(200, "{\"changed\":[\"@bob:example.org\"],\"left\":[]}"));
    client.login(new PasswordCredentials("@alice:matrix.org", "password"));

    var alice = UserId.of("@alice:matrix.org");
    var bob = UserId.of("@bob:example.org");

    var masterKey =
        new CrossSigningKey(List.of("master"), alice, Map.of("ed25519:master", "master"), Map.of());
    var selfSigningKey =
        new CrossSigningKey(
            List.of("self_signing"),
            alice,
            Map.of("ed25519:self_signing", "self_signing"),
            Map.of());
    var userSigningKey =
        new CrossSigningKey(
            List.of("user_signing"),
            alice,
            Map.of("ed25519:user_signing", "user_signing"),
            Map.of());
    var deviceSigning = new DeviceSigningUploadRequest(masterKey, selfSigningKey, userSigningKey);
    client.uploadDeviceSigningKeys(deviceSigning);

    var deviceId = DeviceId.of("D");
    var deviceKeys =
        new DeviceInformation(
            List.of("m.megolm.v1.aes-sha2"),
            deviceId,
            Map.of("ed25519:D", "key"),
            Map.of(),
            new DeviceInformation.UnsignedDeviceData("iPad"),
            alice);
    var uploadRequest =
        new KeysUploadRequest(
            deviceKeys,
            Map.of(
                "signed_curve25519:k",
                new KeysUploadRequest.SignedKey(
                    "key", Map.of(alice, Map.of("ed25519:D", "sig")), false)),
            Map.of("signed_curve25519", new KeysUploadRequest.PlainKey("fallback")));
    assertThat(client.uploadKeys(uploadRequest).oneTimeKeyCounts())
        .containsEntry("signed_curve25519", 17L);
    var query = client.queryKeys(new KeysQueryRequest(Map.of(bob, List.of()), null));
    assertThat(query.deviceKeys().get(bob).get("D").unsigned().deviceDisplayName())
        .isEqualTo("Phone");
    var queryResponse = client.queryKeys(new KeysQueryRequest(Map.of(), null));
    assertThat(queryResponse.failures().get("remote").errcode()).isEqualTo("M_TIMEOUT");
    assertThat(queryResponse.masterKeys().get(bob).usage()).containsExactly("master");
    var claim =
        client.claimKeys(new KeysClaimRequest(Map.of(bob, Map.of("D", "signed_curve25519")), null));
    assertThat(claim.oneTimeKeys().get(bob).get("D").get("curve25519:k"))
        .isEqualTo(new KeysClaimResponse.PlainKey("public"));
    assertThat(claim.failures().get("remote").errcode()).isEqualTo("M_TIMEOUT");
    assertThat(client.getKeyChanges("s0 +/=", "s1").changed()).containsExactly(bob);

    assertThat(requests.get(1).body()).contains("\"master_key\"");
    assertThat(requests.get(1).body()).contains("\"self_signing_key\"");
    assertThat(requests.get(1).body()).contains("\"user_signing_key\"");
    assertThat(requests.get(2).body()).contains("\"device_keys\"");
    assertThat(requests.get(2).body()).contains("\"one_time_keys\"");
    assertThat(requests.get(2).body()).contains("\"fallback_keys\"");
    assertThat(requests.get(2).body())
        .contains("\"key\":\"key\"", "\"fallback\":false", "\"signatures\"");
    assertThat(requests.get(3).body()).isEqualTo("{\"device_keys\":{\"@bob:example.org\":[]}}");
    assertThat(requests.get(4).body()).contains("\"device_keys\"");
    assertThat(requests.get(5).body()).contains("\"one_time_keys\"");
    assertThat(requests.get(6).body()).isNull();
    assertThat(requests.get(1).url()).endsWith("/_matrix/client/v3/keys/device_signing/upload");
    assertThat(requests.get(2).url()).endsWith("/_matrix/client/v3/keys/upload");
    assertThat(requests.get(3).url()).endsWith("/_matrix/client/v3/keys/query");
    assertThat(requests.get(4).url()).endsWith("/_matrix/client/v3/keys/query");
    assertThat(requests.get(5).url()).endsWith("/_matrix/client/v3/keys/claim");
    assertThat(requests.get(6).url())
        .endsWith("/_matrix/client/v3/keys/changes?from=s0%20%2B%2F%3D&to=s1");
  }

  @Test
  void signingAndBackupVersionRequestsSupportUiAuthAndOpaqueVersions() {
    var requests = new ArrayList<Request>();
    MatrixClient client =
        client(
            requests,
            new Response(200, LOGIN_OK),
            new Response(401, "{\"flows\":[{\"stages\":[\"m.login.dummy\"]}],\"session\":\"S\"}"),
            new Response(200, "{}"),
            new Response(
                200,
                "{\"algorithm\":\"m.megolm_backup.v1\",\"auth_data\":{\"public_key\":\"public\"},"
                    + "\"count\":0,\"etag\":\"e\",\"version\":\"v/1 +\"}"),
            new Response(
                200,
                "{\"algorithm\":\"m.megolm_backup.v1\",\"auth_data\":{\"public_key\":\"public\","
                    + "\"my_key\":\"secret\"},\"count\":0,\"etag\":\"e\",\"version\":\"v/1 +\"}"),
            new Response(
                200,
                "{\"algorithm\":\"m.megolm_backup.v1\",\"auth_data\":{\"public_key\":\"public\"},"
                    + "\"count\":0,\"etag\":\"e\",\"version\":\"v/1 +\", \"extra\":{\"multi\":"
                    + "[\"one\", \"two\"]}}"),
            new Response(200, "{}"));
    client.login(new PasswordCredentials("@alice:matrix.org", "password"));

    var alice = UserId.of("@alice:matrix.org");
    var deviceId = DeviceId.of("D");

    var masterKey =
        new CrossSigningKey(List.of("master"), alice, Map.of("ed25519:master", "master"), Map.of());
    var signing =
        new DeviceSigningUploadRequest(
            masterKey,
            null,
            null,
            new DeviceSigningUploadRequest.UiAuth("m.login.dummy", "S", Map.of()));
    var typedDeviceKeys =
        new DeviceInformation(
            List.of("m.megolm.v1.aes-sha2"), deviceId, Map.of("ed25519:D", "key"), Map.of(), alice);
    var typedUpload =
        new KeysUploadRequest(
            typedDeviceKeys,
            Map.of(),
            Map.of("signed_curve25519", new KeysUploadRequest.PlainKey("fallback")));
    assertThat(typedUpload.deviceKeys().algorithms()).containsExactly("m.megolm.v1.aes-sha2");
    assertThatExceptionOfType(MatrixServerException.class)
        .isThrownBy(() -> client.uploadDeviceSigningKeys(signing))
        .satisfies(
            exception ->
                assertThat(exception.getUserInteractiveAuthChallenge().session()).isEqualTo("S"));
    client.uploadDeviceSigningKeysWithAuth(signing);
    var createRequest =
        new RoomKeyBackupVersionRequest(
            "m.megolm_backup.v1",
            new RoomKeyBackupVersionRequest.BackupAuthData(
                "public", null, Map.of("my_key", "secret")),
            null);
    assertThat(client.createRoomKeyBackupVersion(createRequest).version()).isEqualTo("v/1 +");
    assertThat(client.getRoomKeyBackupVersionById("v/1 +").version()).isEqualTo("v/1 +");
    var currentBackup = client.getRoomKeyBackupVersion();
    assertThat(currentBackup.version()).isEqualTo("v/1 +");
    assertThat(currentBackup.extraFields())
        .containsEntry("extra", Map.of("multi", List.of("one", "two")));
    client.updateRoomKeyBackupVersion(
        "v/1 +",
        new RoomKeyBackupVersionRequest(
            "m.megolm_backup.v1",
            new RoomKeyBackupVersionRequest.BackupAuthData("public", null),
            null));

    assertThat(requests.get(1).url()).endsWith("/_matrix/client/v3/keys/device_signing/upload");
    assertThat(requests.get(2).url()).endsWith("/_matrix/client/v3/keys/device_signing/upload");
    assertThat(requests.get(2).body()).contains("\"session\":\"S\"");
    assertThat(requests.get(2).body()).contains("\"master_key\"");
    assertThat(requests.get(2).body()).contains("\"master\"");
    assertThat(requests.get(3).url()).endsWith("/_matrix/client/v3/room_keys/version");
    assertThat(requests.get(4).url()).endsWith("/_matrix/client/v3/room_keys/version/v%2F1%20%2B");
    assertThat(requests.get(6).body()).contains("\"version\":\"v/1 +\"");
  }

  @Test
  void signatureAndRoomKeyBackupGranularityUseExactPaths() {
    var alice = UserId.of("@alice:matrix.org");
    var bob = UserId.of("@bob:example.org");

    var requests = new ArrayList<Request>();
    MatrixClient client =
        client(
            requests,
            new Response(200, LOGIN_OK),
            new Response(200, "{}"),
            new Response(
                200,
                "{\"failures\":{\"@bob:example.org\":{\"ed25519:k\":{\"errcode\":\"M_INVALID_PARAM"
                    + "\",\"error\":\"bad signature\"}}}}"),
            new Response(
                200,
                "{\"failures\":{\"@bob:example.org\":{\"ed25519:k\":{\"errcode\":\"M_INVALID_PARAM"
                    + "\",\"error\":\"bad signature\"}}}}"),
            new Response(
                200,
                "{\"device_keys\":{},\"failures\":{\"remote\":{\"errcode\":\"M_TIMEOUT\",\"error\""
                    + ":\"timeout\"}}}"),
            new Response(200, "{\"rooms\":{\"!r:hs\":{\"sessions\":{}}}}"),
            new Response(200, "{\"count\":1,\"etag\":\"e1\"}"),
            new Response(200, "{\"count\":0,\"etag\":\"e2\"}"),
            new Response(
                200,
                "{\"sessions\":{\"s\":{\"first_message_index\":1,\"forwarded_count\":0,\"is_verifi"
                    + "ed\":true,\"session_data\":{\"ciphertext\":\"c\",\"ephemeral\":\"e\",\"mac"
                    + "\":\"m\"}}}}"),
            new Response(200, "{\"count\":1,\"etag\":\"e2\"}"),
            new Response(200, "{\"count\":0,\"etag\":\"e3\"}"),
            new Response(
                200,
                "{\"first_message_index\":0,\"forwarded_count\":0,"
                    + "\"is_verified\":true,\"session_data\":{\"ciphertext\":\"opaque\","
                    + "\"ephemeral\":\"key\",\"mac\":\"tag\"}}"),
            new Response(200, "{\"count\":1,\"etag\":\"e3\"}"),
            new Response(200, "{\"count\":0,\"etag\":\"e4\"}"),
            new Response(200, "{\"count\":0,\"etag\":\"e5\"}"));
    client.login(new PasswordCredentials("@alice:matrix.org", "password"));
    client.uploadDeviceSigningKeys(
        new DeviceSigningUploadRequest(
            new CrossSigningKey(List.of("master"), alice, Map.of("ed25519:m", "public"), Map.of()),
            null,
            null,
            null));
    var signatureRequest =
        new KeySignaturesUploadRequest(
            Map.of(
                bob,
                Map.of(
                    "ed25519:k",
                    new CrossSigningKey(
                        List.of("master"),
                        bob,
                        Map.of("ed25519:k", "public"),
                        Map.of(bob, Map.of("ed25519:k", "signature"))))));
    assertThat(requests.get(1).body()).contains("\"usage\":[\"master\"]");
    assertThat(
            client
                .uploadKeySignatures(signatureRequest)
                .failures()
                .get(bob)
                .get("ed25519:k")
                .errcode())
        .isEqualTo("M_INVALID_PARAM");
    assertThat(
            client
                .uploadKeySignatures(signatureRequest)
                .failures()
                .get(bob)
                .get("ed25519:k")
                .error())
        .isEqualTo("bad signature");
    assertThat(
            client
                .queryKeys(new KeysQueryRequest(Map.of(), null))
                .failures()
                .get("remote")
                .errcode())
        .isEqualTo("M_TIMEOUT");
    var room = RoomId.of("!r:hs");
    assertThat(client.getRoomKeyBackup("v").rooms().get(room).sessions()).isEmpty();
    var keyData =
        new KeyBackupData(
            0,
            0,
            true,
            new EncryptionRequest.EncryptedSessionData(
                "opaque", "ephemeral", "mac", Map.of("alg_extra", "value")));
    var roomSession = new RoomKeyBackup(Map.of("s", keyData));
    client.uploadRoomKeyBackup("v", new EncryptionRequest(Map.of(room, roomSession)));
    assertThat(requests.get(6).body()).contains("\"alg_extra\":\"value\"");
    client.deleteRoomKeyBackup("v");
    assertThat(client.getRoomKeyBackupForRoom("v", room).sessions().get("s").sessionData().mac())
        .isEqualTo("m");
    client.uploadRoomKeyBackupForRoom("v", room, roomSession);
    client.deleteRoomKeyBackupForRoom("v", room);
    assertThat(client.getRoomKeyBackupSession("v", room, "s")).isNotNull();
    var data =
        new KeyBackupData(
            1, 0, false, new EncryptionRequest.EncryptedSessionData("cipher", "key", "check"));
    client.uploadRoomKeyBackupSession("v", room, "s", data);
    client.deleteRoomKeyBackupSession("v", room, "s");
    client.deleteRoomKeyBackupVersion("v");

    assertThat(requests.get(1).url()).endsWith("/_matrix/client/v3/keys/device_signing/upload");
    assertThat(requests.get(2).url()).endsWith("/_matrix/client/v3/keys/signatures/upload");
    assertThat(requests.get(3).url()).endsWith("/_matrix/client/v3/keys/signatures/upload");
    assertThat(requests.get(4).url()).endsWith("/_matrix/client/v3/keys/query");
    assertThat(requests.get(5).url()).endsWith("/_matrix/client/v3/room_keys/keys?version=v");
    assertThat(requests.get(6).url()).endsWith("/_matrix/client/v3/room_keys/keys?version=v");
    assertThat(requests.get(7).url()).endsWith("/_matrix/client/v3/room_keys/keys?version=v");
    assertThat(requests.get(8).url())
        .endsWith("/_matrix/client/v3/room_keys/keys/%21r%3Ahs?version=v");
    assertThat(requests.get(9).url())
        .endsWith("/_matrix/client/v3/room_keys/keys/%21r%3Ahs?version=v");
    assertThat(requests.get(10).url())
        .endsWith("/_matrix/client/v3/room_keys/keys/%21r%3Ahs?version=v");
    assertThat(requests.get(11).url())
        .endsWith("/_matrix/client/v3/room_keys/keys/%21r%3Ahs/s?version=v");
    assertThat(requests.get(12).url())
        .endsWith("/_matrix/client/v3/room_keys/keys/%21r%3Ahs/s?version=v");
    assertThat(requests.get(13).url())
        .endsWith("/_matrix/client/v3/room_keys/keys/%21r%3Ahs/s?version=v");
    assertThat(requests.get(14).url()).endsWith("/_matrix/client/v3/room_keys/version/v");
  }

  @Test
  void keyEndpointsRequireAuthenticationAndPreserveServerErrors() {
    MatrixClient client = MatrixClient.builder("https://matrix.example.org").build();
    var uploadRequest = new KeysUploadRequest(null, Map.of(), null);
    var queryRequest = new KeysQueryRequest(Map.of(), null);
    assertThatThrownBy(() -> client.queryKeys(queryRequest))
        .isInstanceOf(AuthenticationException.class);
    var claimRequest = new KeysClaimRequest(Map.of(), null);
    assertThatThrownBy(() -> client.uploadKeys(uploadRequest))
        .isInstanceOf(AuthenticationException.class);
    assertThatThrownBy(() -> client.queryKeys(queryRequest))
        .isInstanceOf(AuthenticationException.class);
    assertThatThrownBy(() -> client.claimKeys(claimRequest))
        .isInstanceOf(AuthenticationException.class);
    assertThatThrownBy(() -> client.getKeyChanges("a", "b"))
        .isInstanceOf(AuthenticationException.class);
    assertThatThrownBy(() -> client.uploadRoomKeyBackup("v", (EncryptionRequest) null))
        .isInstanceOf(NullPointerException.class);

    MatrixClient serverError =
        client(
            new ArrayList<>(),
            new Response(200, LOGIN_OK),
            new Response(403, "{\"errcode\":\"M_FORBIDDEN\",\"error\":\"denied\"}"));
    serverError.login(new PasswordCredentials("@alice:matrix.org", "password"));
    var errorRequest = new KeysQueryRequest(Map.of(), null);
    assertThatExceptionOfType(MatrixServerException.class)
        .isThrownBy(() -> serverError.queryKeys(errorRequest))
        .extracting(MatrixServerException::getErrcode)
        .isEqualTo("M_FORBIDDEN");
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
