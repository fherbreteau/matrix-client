package io.github.fherbreteau.matrix.endpoint;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.fherbreteau.matrix.error.AuthenticationException;
import io.github.fherbreteau.matrix.error.DiscoveryException;
import io.github.fherbreteau.matrix.error.MatrixServerException;
import io.github.fherbreteau.matrix.json.JsonParser;
import io.github.fherbreteau.matrix.model.MatrixFilter;
import io.github.fherbreteau.matrix.model.PasswordCredentials;
import io.github.fherbreteau.matrix.model.RoomEventFilter;
import io.github.fherbreteau.matrix.model.SyncOptions;
import io.github.fherbreteau.matrix.model.SyncResponse;
import io.github.fherbreteau.matrix.model.SyncTokenStore;
import io.github.fherbreteau.matrix.transport.HttpTransport.Request;
import io.github.fherbreteau.matrix.transport.HttpTransport.Response;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class SyncClientTest {

  private static final String LOGIN_OK =
      "{\"user_id\":\"@alice:matrix.org\",\"access_token\":\"secret-token\",\"device_id\":\"DEV\"}";
  private static final String BASIC_SYNC =
      "{\"next_batch\":\"opaque-1\",\"rooms\":{\"join\":{},\"invite\":{},\"leave\":{}}}";

  @Test
  void createFilterPostsTheDefinitionAndParsesItsId() {
    var requests = new ArrayList<Request>();
    MatrixClient client =
        MatrixClient.builder("https://matrix.example.org")
            .transport(
                recording(
                    requests,
                    new Response(200, LOGIN_OK),
                    new Response(200, "{\"filter_id\":\"123\"}")))
            .build();
    client.login(new PasswordCredentials("@alice:matrix.org", "secret"));
    var filter =
        MatrixFilter.builder()
            .roomTimeline(
                RoomEventFilter.builder().types(List.of("m.room.message")).limit(10).build())
            .build();
    assertThat(client.createFilter(filter)).isEqualTo("123");
    assertThat(requests.getLast().url())
        .isEqualTo(
            "https://matrix.example.org/_matrix/client/v3/user/%40alice%3Amatrix.org/filter");
    assertThat(requests.getLast().body()).contains("m.room.message");
    assertThat(requests.getLast().headers())
        .containsEntry(Request.AUTHORIZATION_HEADER, "Bearer secret-token");
  }

  @Test
  void serverSideFilterBodyIsPreservedWhenPosted() {
    var requests = new ArrayList<Request>();
    MatrixClient client =
        MatrixClient.builder("https://matrix.example.org")
            .transport(
                recording(
                    requests,
                    new Response(200, LOGIN_OK),
                    new Response(200, "{\"filter_id\":\"f1\"}")))
            .build();
    client.login(new PasswordCredentials("@alice:matrix.org", "secret"));
    var filter =
        MatrixFilter.builder()
            .rawOption("extension_field", JsonParser.parse("{\"x\":true}"))
            .build();
    assertThat(client.createFilter(filter)).isEqualTo("f1");
    assertThat(requests.getLast().body()).contains("extension_field");
  }

  @Test
  void getFilterPreservesUnknownFieldsAndAuthenticates() {
    var requests = new ArrayList<Request>();
    MatrixClient client =
        MatrixClient.builder("https://matrix.example.org")
            .transport(
                recording(
                    requests,
                    new Response(200, LOGIN_OK),
                    new Response(
                        200, "{\"event_format\":\"client\",\"future_filter_option\":true}")))
            .build();
    client.login(new PasswordCredentials("@alice:matrix.org", "secret"));
    MatrixFilter filter = client.getFilter("filter id");
    assertThat(filter.toJson().asObject().get("future_filter_option").asBoolean()).isTrue();
    assertThat(requests.getLast().headers())
        .containsEntry(Request.AUTHORIZATION_HEADER, "Bearer secret-token");
  }

  @Test
  void syncPerformsInitialSyncAndPersistsTheToken() {
    var requests = new ArrayList<Request>();
    var store = new RecordingSyncTokenStore();
    MatrixClient client =
        MatrixClient.builder("https://matrix.example.org")
            .transport(
                recording(requests, new Response(200, LOGIN_OK), new Response(200, BASIC_SYNC)))
            .syncTokenStore(store)
            .build();
    client.login(new PasswordCredentials("@alice:matrix.org", "secret"));
    SyncResponse response = client.sync(25000, (String) null);
    assertThat(response.nextBatch()).isEqualTo("opaque-1");
    assertThat(store.current()).contains("opaque-1");
    assertThat(requests.getLast().url())
        .contains("/_matrix/client/v3/sync?timeout=25000&full_state=false");
    assertThat(requests.getLast().headers())
        .containsEntry(Request.AUTHORIZATION_HEADER, "Bearer secret-token");
  }

  @Test
  void syncWithTypedFilterStartsInitialSyncWhenStoreIsEmpty() {
    var requests = new ArrayList<Request>();
    MatrixClient client =
        MatrixClient.builder("https://matrix.example.org")
            .transport(
                recording(requests, new Response(200, LOGIN_OK), new Response(200, BASIC_SYNC)))
            .build();
    client.login(new PasswordCredentials("@alice:matrix.org", "secret"));
    var filter =
        MatrixFilter.builder().roomTimeline(RoomEventFilter.builder().limit(3).build()).build();
    client.syncWithFilter(100, filter);
    assertThat(requests.getLast().url()).contains("filter=%7B").doesNotContain("since=");
  }

  @Test
  void syncUsesThePreviousOpaqueTokenAndTypedFilter() {
    var requests = new ArrayList<Request>();
    var store = new RecordingSyncTokenStore();
    store.save("opaque/since");
    MatrixClient client =
        MatrixClient.builder("https://matrix.example.org")
            .transport(
                recording(requests, new Response(200, LOGIN_OK), new Response(200, BASIC_SYNC)))
            .syncTokenStore(store)
            .build();
    client.login(new PasswordCredentials("@alice:matrix.org", "secret"));
    var filter =
        MatrixFilter.builder()
            .roomTimeline(RoomEventFilter.builder().types(List.of("m.room.message")).build())
            .build();
    client.syncWithFilter(30000, filter);
    assertThat(requests.getLast().url()).contains("since=opaque%2Fsince").contains("filter=%7B");
  }

  @Test
  void typedFilterUsesInlineJsonAfterAnIncrementalTokenExists() {
    var requests = new ArrayList<Request>();
    MatrixClient client =
        MatrixClient.builder("https://matrix.example.org")
            .transport(
                recording(
                    requests,
                    new Response(200, LOGIN_OK),
                    new Response(200, BASIC_SYNC),
                    new Response(200, BASIC_SYNC)))
            .build();
    client.login(new PasswordCredentials("@sync:test.org", "secret"));
    client.sync(0, (String) null);
    MatrixFilter filter =
        MatrixFilter.builder()
            .roomTimeline(RoomEventFilter.builder().types(List.of("m.room.message")).build())
            .build();
    client.syncWithFilter(0, filter);
    assertThat(requests.getLast().url()).contains("since=opaque-1").contains("filter=%7B");
  }

  @Test
  void syncDoesNotAdvanceStoreOnServerError() {
    var store = new RecordingSyncTokenStore();
    store.save("previous");
    MatrixClient client =
        MatrixClient.builder("https://matrix.example.org")
            .transport(
                queued(
                    new Response(200, LOGIN_OK),
                    new Response(500, "{\"errcode\":\"M_UNKNOWN\",\"error\":\"failed\"}")))
            .syncTokenStore(store)
            .build();
    client.login(new PasswordCredentials("@alice:matrix.org", "secret"));
    assertThatExceptionOfType(MatrixServerException.class)
        .isThrownBy(() -> client.sync(0, (String) null));
    assertThat(store.current()).contains("previous");
  }

  @Test
  void syncRejectsMalformedResponseWithoutPersistingToken() {
    var store = new RecordingSyncTokenStore();
    MatrixClient client =
        MatrixClient.builder("https://matrix.example.org")
            .transport(queued(new Response(200, LOGIN_OK), new Response(200, "{\"rooms\":{}}")))
            .syncTokenStore(store)
            .build();
    client.login(new PasswordCredentials("@alice:matrix.org", "secret"));
    assertThatThrownBy(() -> client.sync(0, (String) null))
        .isInstanceOf(IllegalArgumentException.class);
    assertThat(store.current()).isEmpty();
  }

  @Test
  void logoutClearsTheSyncTokenStore() {
    var store = new RecordingSyncTokenStore();
    store.save("saved-before-logout");
    MatrixClient client =
        MatrixClient.builder("https://matrix.example.org")
            .transport(queued(new Response(200, LOGIN_OK), new Response(200, "{}")))
            .syncTokenStore(store)
            .build();
    client.login(new PasswordCredentials("@sync:test.org", "secret"));
    client.logout();
    assertThat(store.current()).isEmpty();
  }

  @Test
  void syncWithoutSessionRaisesAuthenticationException() {
    MatrixClient client =
        MatrixClient.builder("https://matrix.example.org")
            .transport(stub -> new Response(200, "{}"))
            .build();
    assertThatThrownBy(() -> client.sync(0, (String) null))
        .isInstanceOf(AuthenticationException.class);
  }

  @Test
  void inMemorySyncStoreCanClearAndReplaceItsToken() {
    SyncTokenStore store = SyncTokenStore.inMemory();
    store.save("first");
    assertThat(store.current()).contains("first");
    store.clear();
    assertThat(store.current()).isEmpty();
    store.save("second");
    assertThat(store.current()).contains("second");
  }

  @Test
  void syncStoreCanBeClearedForAFirstSync() {
    var store = new RecordingSyncTokenStore();
    store.save("opaque");
    assertThat(store.current()).contains("opaque");
    store.clear();
    assertThat(store.current()).isEmpty();
    store.save("opaque");
    MatrixClient client =
        MatrixClient.builder("https://matrix.example.org")
            .transport(queued(new Response(200, LOGIN_OK), new Response(200, BASIC_SYNC)))
            .syncTokenStore(store)
            .build();
    client.login(new PasswordCredentials("@alice:matrix.org", "secret"));
    client.clearSyncToken();
    assertThat(client.syncToken()).isEmpty();
    client.sync(0, (String) null);
    assertThat(client.syncToken()).contains("opaque-1");
  }

  @Test
  void serverSideFilterResponseMustContainFilterId() {
    MatrixClient client =
        MatrixClient.builder("https://matrix.example.org")
            .transport(queued(new Response(200, LOGIN_OK), new Response(200, "{}")))
            .build();
    client.login(new PasswordCredentials("@alice:matrix.org", "secret"));
    var filter = MatrixFilter.builder().build();
    assertThatThrownBy(() -> client.createFilter(filter))
        .isInstanceOf(DiscoveryException.class)
        .hasMessageContaining("filter_id");
  }

  @Test
  void invalidSavedFilterResponseIsRejected() {
    MatrixClient client =
        MatrixClient.builder("https://matrix.example.org")
            .transport(queued(new Response(200, LOGIN_OK), new Response(200, "[]")))
            .build();
    client.login(new PasswordCredentials("@alice:matrix.org", "secret"));
    assertThatThrownBy(() -> client.getFilter("bad")).isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void injectedSyncTokenStoreIsUsedAndCleared() {
    var store = new RecordingTokenStore();
    MatrixClient client =
        MatrixClient.builder("https://matrix.example.org")
            .transport(queued(new Response(200, LOGIN_OK), new Response(200, BASIC_SYNC)))
            .syncTokenStore(store)
            .build();
    client.login(new PasswordCredentials("@alice:matrix.org", "secret"));
    store.save("restored-from-persistence");
    client.sync(0, (String) null);
    assertThat(store.current()).contains("opaque-1");
    assertThat(client.syncToken()).contains("opaque-1");
    client.clearSyncToken();
    assertThat(store.current()).isEmpty();
  }

  @Test
  void typedFilterFallsBackToInitialSyncWhenThereIsNoSavedToken() {
    var requests = new ArrayList<Request>();
    MatrixClient client =
        MatrixClient.builder("https://matrix.example.org")
            .transport(
                recording(requests, new Response(200, LOGIN_OK), new Response(200, BASIC_SYNC)))
            .build();
    client.login(new PasswordCredentials("@sync:test.org", "secret"));
    var filter =
        MatrixFilter.builder().roomTimeline(RoomEventFilter.builder().limit(3).build()).build();
    client.syncWithFilter(100, filter);
    assertThat(requests.getLast().url()).contains("filter=%7B").doesNotContain("since=");
  }

  @Test
  void syncOptionsCanSelectNewSpecParameters() {
    var options = new SyncOptions("t", "f", 1L, false, "online", true);
    assertThat(options.toQuery().asObject().get("use_state_after").asBoolean()).isTrue();
    assertThat(options.toQuery().asObject().get("since").asString()).isEqualTo("t");
  }

  private static final class RecordingTokenStore implements SyncTokenStore {

    private String token;

    @Override
    public java.util.Optional<String> current() {
      return java.util.Optional.ofNullable(token);
    }

    @Override
    public void save(String token) {
      this.token = token;
    }

    @Override
    public void clear() {
      token = null;
    }
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

  private static final class RecordingSyncTokenStore implements SyncTokenStore {

    private String token;

    @Override
    public java.util.Optional<String> current() {
      return java.util.Optional.ofNullable(token);
    }

    @Override
    public void save(String token) {
      this.token = token;
    }

    @Override
    public void clear() {
      token = null;
    }
  }
}
