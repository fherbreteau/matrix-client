package io.github.fherbreteau.matrix.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.fherbreteau.matrix.json.JsonParser;
import io.github.fherbreteau.matrix.json.JsonValue;
import java.util.List;
import org.junit.jupiter.api.Test;

class SyncModelsTest {

  @Test
  void eventFilterSerializesTypesAndUsers() {
    var filter =
        EventFilter.builder()
            .types(List.of("m.room.message", "org.example.custom"))
            .notTypes(List.of("m.room.encrypted"))
            .senders(List.of(UserId.of("@alice:matrix.org")))
            .notSenders(List.of(UserId.of("@bot:matrix.org")))
            .limit(20)
            .containsUrl(true)
            .build();
    assertThat(filter.toJson().toJson())
        .isEqualTo(
            "{\"types\":[\"m.room.message\",\"org.example.custom\"],"
                + "\"not_types\":[\"m.room.encrypted\"],\"senders\":[\"@alice:matrix.org\"],"
                + "\"not_senders\":[\"@bot:matrix.org\"],\"limit\":20,\"contains_url\":true}");
  }

  @Test
  void roomEventFilterValidatesLazyMemberOptions() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> RoomEventFilter.builder().includeRedundantMembers(true).build());
    var filter =
        RoomEventFilter.builder()
            .lazyLoadMembers(true)
            .includeRedundantMembers(true)
            .rooms(List.of(RoomId.of("!a:b")))
            .build();
    assertThat(filter.toJson().asObject().get("lazy_load_members").asBoolean()).isTrue();
    assertThat(filter.toJson().asObject().get("include_redundant_members").asBoolean()).isTrue();
  }

  @Test
  void matrixFilterIncludesNestedRoomFiltersAndFormat() {
    var filter =
        MatrixFilter.builder()
            .eventFields(List.of("type", "content", "event_id"))
            .eventFormat(EventFormat.FEDERATION)
            .presence(EventFilter.builder().types(List.of("m.presence")).build())
            .accountData(EventFilter.builder().types(List.of("m.tag")).build())
            .roomTimeline(
                RoomEventFilter.builder().types(List.of("m.room.message")).limit(30).build())
            .roomState(RoomEventFilter.builder().lazyLoadMembers(true).build())
            .roomEphemeral(EventFilter.builder().types(List.of("m.typing")).build())
            .roomAccountData(EventFilter.builder().types(List.of("m.fully_read")).build())
            .build();
    var obj = filter.toJson().asObject();
    assertThat(obj.get("event_format").asString()).isEqualTo("federation");
    assertThat(obj.get("event_fields").asArray().size()).isEqualTo(3);
    assertThat(obj.get("room").asObject().get("timeline").asObject().get("limit").asLong())
        .isEqualTo(30);
    assertThat(
            obj.get("room").asObject().get("state").asObject().get("lazy_load_members").asBoolean())
        .isTrue();
  }

  @Test
  void inlineSyncFiltersAreOpaqueJsonStrings() {
    var options = SyncOptions.initial(25000, "{\"room\":{\"timeline\":{\"limit\":10}}}");
    assertThat(options.toQuery().get("filter").asString())
        .isEqualTo("{\"room\":{\"timeline\":{\"limit\":10}}}");
    var filterId = SyncOptions.initial(0, "123");
    assertThat(filterId.toQuery().get("filter").asString()).isEqualTo("123");
  }

  @Test
  void syncOptionsSerializeOptionalParameters() {
    var options = new SyncOptions("opaque-token", "filter-id", 30000L, true, "offline", true);
    assertThat(options.toQuery().toJson())
        .isEqualTo(
            "{\"since\":\"opaque-token\",\"filter\":\"filter-id\",\"timeout\":30000,"
                + "\"full_state\":true,\"set_presence\":\"offline\",\"use_state_after\":true}");
    assertThat(SyncOptions.defaults().toQuery().asObject().size()).isZero();
    assertThat(new SyncOptions(null, null, 0L, false, null, null).toQuery().has("timeout"))
        .isFalse();
    assertThat(new SyncOptions(null, null, null, null, "online", null).toQuery().toJson())
        .isEqualTo("{\"set_presence\":\"online\"}");
    assertThat(new SyncOptions(null, null, null, null, null, false).toQuery().toJson())
        .isEqualTo("{\"use_state_after\":false}");
  }

  @Test
  void incrementalSyncRequiresAStartingToken() {
    assertThatIllegalArgumentException().isThrownBy(() -> SyncOptions.incremental(null, 0, null));
    assertThatIllegalArgumentException().isThrownBy(() -> SyncOptions.incremental(" ", 0, null));
  }

  @Test
  void syncResponseParsesRoomsAndPreservesUnknowns() {
    var response =
        SyncResponse.from(
            JsonParser.parse(
                """
                {
                  "next_batch":"opaque-next",
                  "presence":{"events":[{"type":"m.presence","sender":"@a:b","content":{"presence":"online"}}]},
                  "account_data":{"events":[]},
                  "to_device":{"events":[{"type":"m.room_key","content":{"key":"secret"}}]},
                  "device_lists":{"changed":["@b:b"],"left":[]},
                  "device_one_time_keys_count":{"signed_curve25519":20},
                  "future_top_level":{"preserved":true},
                  "rooms":{
                    "join":{"!a:b":{"timeline":{"events":[{"event_id":"$e","sender":"@a:b","type":"m.room.message","room_id":"!a:b","origin_server_ts":1234,"content":{"body":"hi"}}]},"state":{"events":[{"event_id":"$state","sender":"@a:b","type":"m.room.name","state_key":"","content":{"name":"joined"}}]},"state_after":{"events":[{"event_id":"$after","sender":"@a:b","type":"m.room.topic","state_key":"","content":{"topic":"snapshot"}}]},"ephemeral":{"events":[]},"account_data":{"events":[]}}},
                    "invite":{"!c:b":{"invite_state":{"events":[{"type":"m.room.member","state_key":"@alice:b","content":{"membership":"invite"}}]}}},
                    "knock":{"!k:b":{"knock_state":{"events":[{"type":"m.room.member","state_key":"@alice:b","content":{"membership":"knock"}}]}}},
                    "leave":{"!d:b":{"timeline":{"events":[]},"state":{"events":[]}}}
                  }
                }
                """));
    assertThat(response.nextBatch()).isEqualTo("opaque-next");
    assertThat(response.joinedRooms()).containsKey("!a:b");
    assertThat(response.joinedRooms().get("!a:b").timeline()).hasSize(1);
    assertThat(response.joinedRooms().get("!a:b").timeline().getFirst().originServerTs())
        .isEqualTo(1234L);
    assertThat(response.joinedRooms().get("!a:b").state()).hasSize(1);
    assertThat(response.joinedRooms().get("!a:b").stateAfter()).hasSize(1);
    assertThat(response.joinedRooms().get("!a:b").knockState()).isEmpty();
    assertThat(response.invitedRooms().get("!c:b").inviteState()).hasSize(1);
    assertThat(response.knockedRooms().get("!k:b").knockState()).hasSize(1);
    assertThat(response.leftRooms()).containsKey("!d:b");
    assertThat(response.toDevice().asObject().get("events").asArray().size()).isEqualTo(1);
    assertThat(response.deviceOneTimeKeysCount().asObject().get("signed_curve25519").asLong())
        .isEqualTo(20);
    assertThat(
            response
                .raw()
                .asObject()
                .get("future_top_level")
                .asObject()
                .get("preserved")
                .asBoolean())
        .isTrue();
  }

  @Test
  void syncResponseRejectsMalformedStateAfter() {
    JsonValue malformed =
        JsonParser.parse(
            "{\"next_batch\":\"t1\",\"rooms\":{\"join\":{\"!a:b\":{\"state_after\":{}}}}}");
    assertThatThrownBy(() -> SyncResponse.from(malformed))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("state_after");
  }

  @Test
  void syncResponseRequiresTheOpaqueNextBatchToken() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> SyncResponse.from(JsonParser.parse("{}")));
    assertThatIllegalArgumentException()
        .isThrownBy(() -> SyncResponse.from(JsonParser.parse("[]")));
  }
}
