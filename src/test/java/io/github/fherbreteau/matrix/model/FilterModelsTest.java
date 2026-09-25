package io.github.fherbreteau.matrix.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import io.github.fherbreteau.matrix.json.JsonParser;
import java.util.List;
import org.junit.jupiter.api.Test;

class FilterModelsTest {

  @Test
  void eventFilterCoversAllTypedOptions() {
    var filter =
        EventFilter.builder()
            .types(List.of("m.room.message"))
            .notTypes(List.of("m.room.encrypted"))
            .senders(List.of(UserId.of("@a:b")))
            .notSenders(List.of(UserId.of("@bot:b")))
            .rooms(List.of(RoomId.of("!a:b")))
            .notRooms(List.of(RoomId.of("!x:b")))
            .containsUrl(true)
            .limit(10)
            .lazyLoadMembers(true)
            .includeRedundantMembers(true)
            .rawOption("unread_thread_notifications", JsonParser.parse("true"))
            .build();
    var obj = filter.toJson().asObject();
    assertThat(obj.get("types").asArray().size()).isEqualTo(1);
    assertThat(obj.get("not_types").asArray().size()).isEqualTo(1);
    assertThat(obj.get("senders").asArray().get(0).asString()).isEqualTo("@a:b");
    assertThat(obj.get("not_senders").asArray().get(0).asString()).isEqualTo("@bot:b");
    assertThat(obj.get("rooms").asArray().get(0).asString()).isEqualTo("!a:b");
    assertThat(obj.get("not_rooms").asArray().get(0).asString()).isEqualTo("!x:b");
    assertThat(obj.get("contains_url").asBoolean()).isTrue();
    assertThat(obj.get("limit").asLong()).isEqualTo(10);
    assertThat(obj.get("lazy_load_members").asBoolean()).isTrue();
    assertThat(obj.get("include_redundant_members").asBoolean()).isTrue();
    assertThat(obj.get("unread_thread_notifications").asBoolean()).isTrue();
  }

  @Test
  void eventFilterRejectsInvalidLimitsAndLazyLoadCombinations() {
    assertThatIllegalArgumentException().isThrownBy(() -> EventFilter.builder().limit(0));
    assertThatIllegalArgumentException()
        .isThrownBy(() -> EventFilter.builder().includeRedundantMembers(true).build());
    assertThatIllegalArgumentException()
        .isThrownBy(
            () ->
                EventFilter.builder().lazyLoadMembers(false).includeRedundantMembers(true).build());
  }

  @Test
  void roomEventFilterCoversAllTypedOptions() {
    var filter =
        RoomEventFilter.builder()
            .limit(15)
            .senders(List.of(UserId.of("@a:b")))
            .notSenders(List.of(UserId.of("@bot:b")))
            .types(List.of("m.room.message"))
            .notTypes(List.of("m.room.encrypted"))
            .rooms(List.of(RoomId.of("!a:b")))
            .notRooms(List.of(RoomId.of("!x:b")))
            .containsUrl(false)
            .lazyLoadMembers(true)
            .includeRedundantMembers(true)
            .rawRoomOption("custom", JsonParser.parse("1"))
            .build();
    var obj = filter.toJson().asObject();
    assertThat(obj.get("limit").asLong()).isEqualTo(15);
    assertThat(obj.get("senders").asArray().get(0).asString()).isEqualTo("@a:b");
    assertThat(obj.get("not_senders").asArray().get(0).asString()).isEqualTo("@bot:b");
    assertThat(obj.get("types").asArray().get(0).asString()).isEqualTo("m.room.message");
    assertThat(obj.get("not_types").asArray().get(0).asString()).isEqualTo("m.room.encrypted");
    assertThat(obj.get("rooms").asArray().get(0).asString()).isEqualTo("!a:b");
    assertThat(obj.get("not_rooms").asArray().get(0).asString()).isEqualTo("!x:b");
    assertThat(obj.get("contains_url").asBoolean()).isFalse();
    assertThat(obj.get("lazy_load_members").asBoolean()).isTrue();
    assertThat(obj.get("include_redundant_members").asBoolean()).isTrue();
    assertThat(obj.get("custom").asLong()).isEqualTo(1);
  }

  @Test
  void roomEventFilterAllowsZeroAndRejectsNegativeLimit() {
    assertThat(RoomEventFilter.builder().limit(0).build().toJson().asObject().get("limit").asLong())
        .isZero();
    assertThatIllegalArgumentException()
        .isThrownBy(() -> RoomEventFilter.builder().limit(-1).build());
  }

  @Test
  void matrixFilterCoversAllTopLevelAndNestedSections() {
    var filter =
        MatrixFilter.builder()
            .eventFields(List.of("type", "content"))
            .eventFormat(EventFormat.CLIENT)
            .presence(EventFilter.builder().types(List.of("m.presence")).build())
            .accountData(EventFilter.builder().types(List.of("m.tag")).build())
            .room(JsonParser.parse("{\"include_leave\":true}"))
            .roomTimeline(RoomEventFilter.builder().limit(5).build())
            .roomState(RoomEventFilter.builder().types(List.of("m.room.name")).build())
            .roomEphemeral(EventFilter.builder().types(List.of("m.typing")).build())
            .roomAccountData(EventFilter.builder().types(List.of("m.fully_read")).build())
            .rawOption("extension", JsonParser.parse("true"))
            .build();
    var root = filter.toJson().asObject();
    assertThat(root.get("event_fields").asArray().size()).isEqualTo(2);
    assertThat(root.get("event_format").asString()).isEqualTo("client");
    assertThat(root.get("presence").asObject().get("types").asArray().get(0).asString())
        .isEqualTo("m.presence");
    assertThat(root.get("account_data").asObject().get("types").asArray().get(0).asString())
        .isEqualTo("m.tag");
    var room = root.get("room").asObject();
    assertThat(room.get("include_leave").asBoolean()).isTrue();
    assertThat(room.get("timeline").asObject().get("limit").asLong()).isEqualTo(5);
    assertThat(room.get("state").asObject().get("types").asArray().get(0).asString())
        .isEqualTo("m.room.name");
    assertThat(room.get("ephemeral").asObject().get("types").asArray().get(0).asString())
        .isEqualTo("m.typing");
    assertThat(room.get("account_data").asObject().get("types").asArray().get(0).asString())
        .isEqualTo("m.fully_read");
    assertThat(root.get("extension").asBoolean()).isTrue();
  }

  @Test
  void emptyMatrixFilterSerializesEmptyObject() {
    assertThat(MatrixFilter.builder().build().toJson().toJson()).isEqualTo("{}");
  }
}
