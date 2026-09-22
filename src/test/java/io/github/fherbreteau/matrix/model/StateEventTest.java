package io.github.fherbreteau.matrix.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import io.github.fherbreteau.matrix.json.JsonParser;
import java.util.List;
import org.junit.jupiter.api.Test;

class StateEventTest {

  @Test
  void emptyStateKeyIsTheDefault() {
    var event = StateEvent.of("m.room.name", JsonParser.parse("{\"name\":\"The Room\"}"));
    assertThat(event.type()).isEqualTo("m.room.name");
    assertThat(event.stateKey()).isEmpty();
    assertThat(event.content().asObject().get("name").asString()).isEqualTo("The Room");
  }

  @Test
  void explicitStateKeysAreKept() {
    var event =
        StateEvent.of(
            "m.room.member", "@bob:matrix.org", JsonParser.parse("{\"membership\":\"join\"}"));
    assertThat(event.stateKey()).isEqualTo("@bob:matrix.org");
  }

  @Test
  void serializesToTheInitialStateEntryShape() {
    var event = StateEvent.of("m.room.name", JsonParser.parse("{\"name\":\"X\"}"));
    assertThat(event.toJson().toJson())
        .isEqualTo("{\"type\":\"m.room.name\",\"state_key\":\"\",\"content\":{\"name\":\"X\"}}");
  }

  @Test
  void nullStateKeyAndContentAreNormalized() {
    var event = new StateEvent("m.room.topic", null, null);
    assertThat(event.stateKey()).isEmpty();
    assertThat(event.content().asObject().names()).isEmpty();
  }

  @Test
  void rejectsBlankTypes() {
    assertThatIllegalArgumentException().isThrownBy(() -> StateEvent.of(null, null, null));
    assertThatIllegalArgumentException().isThrownBy(() -> StateEvent.of(" ", null, null));
    assertThatIllegalArgumentException()
        .isThrownBy(() -> StateEvent.of("", JsonParser.parse("{}")));
  }

  @Test
  void initialStateConfiguresTheRoomAtCreation() {
    var creation =
        RoomCreation.builder()
            .initialState(
                List.of(
                    StateEvent.of("m.room.name", JsonParser.parse("{\"name\":\"The Room\"}")),
                    StateEvent.of(
                        "m.room.history_visibility",
                        JsonParser.parse("{\"history_visibility\":\"world_readable\"}"))))
            .build();
    String body = creation.toJson().toJson();
    assertThat(body)
        .isEqualTo(
            "{\"initial_state\":[{\"type\":\"m.room.name\",\"state_key\":\"\","
                + "\"content\":{\"name\":\"The Room\"}},{\"type\":\"m.room.history_visibility\","
                + "\"state_key\":\"\",\"content\":{\"history_visibility\":\"world_readable\"}}]}");
  }
}
