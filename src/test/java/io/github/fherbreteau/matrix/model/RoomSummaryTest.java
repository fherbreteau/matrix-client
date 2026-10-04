package io.github.fherbreteau.matrix.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import io.github.fherbreteau.matrix.json.JsonParser;
import io.github.fherbreteau.matrix.json.JsonValue;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class RoomSummaryTest {
  @Test
  void parsesTypedFieldsAndRetainsUnknownFields() {
    JsonValue json =
        JsonParser.parse(
            "{\"room_id\":\"!room:example.org\",\"allowed_room_ids\":[\"!parent:example.org\"],\"guest_can_join\":false,\"num_joined_members\":8,\"world_readable\":true,\"name\":\"Example\",\"x_unknown\":42}");
    RoomSummary summary = RoomSummary.from(json);
    assertThat(summary.roomId()).isEqualTo(RoomId.of("!room:example.org"));
    assertThat(summary.allowedRoomIds()).containsExactly(RoomId.of("!parent:example.org"));
    assertThat(summary.name()).isEqualTo("Example");
    assertThat(summary.raw().asObject().get("x_unknown").asLong()).isEqualTo(42);
    assertThatIllegalArgumentException().isThrownBy(() -> RoomSummary.from(JsonParser.parse("{}")));
    assertThatIllegalArgumentException()
        .isThrownBy(() -> RoomSummary.from(JsonParser.parse("{\"room_id\":\"!x:y\"}")));
  }

  @Test
  void listsAreImmutable() {
    List<RoomId> allowed = new ArrayList<>(List.of(RoomId.of("!parent:example.org")));
    RoomSummary summary =
        new RoomSummary(
            RoomId.of("!room:example.org"),
            allowed,
            null,
            null,
            null,
            false,
            null,
            null,
            null,
            0,
            null,
            null,
            null,
            false,
            null);
    allowed.clear();
    assertThat(summary.allowedRoomIds()).hasSize(1);
    assertThatExceptionOfType(UnsupportedOperationException.class)
        .isThrownBy(() -> summary.allowedRoomIds().clear());
  }
}
