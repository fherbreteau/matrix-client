package io.github.fherbreteau.matrix.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.InstanceOfAssertFactories.BOOLEAN;
import static org.assertj.core.api.InstanceOfAssertFactories.list;

import io.github.fherbreteau.matrix.json.JsonParser;
import java.util.List;
import org.junit.jupiter.api.Test;

class RoomTest {

  @Test
  void roomIdConstructor() {
    Room room = new Room("!a:b");
    assertThat(room).extracting(Room::getRoomId).isEqualTo("!a:b");
    assertThat(room).extracting(Room::getEvents, list(RoomEvent.class)).isEmpty();
  }

  @Test
  void eventsConstructor() {
    var event = new RoomEvent("$e", "@u:b", "m.room.message", null, JsonParser.parse("{}"));
    Room room = new Room("!a:b", List.of(event));
    assertThat(room).extracting(Room::getRoomId).isEqualTo("!a:b");
    assertThat(room)
        .extracting(Room::getEvents, list(RoomEvent.class))
        .hasSize(1)
        .singleElement()
        .satisfies(
            parsed -> {
              assertThat(parsed).extracting(RoomEvent::eventId).isEqualTo("$e");
              assertThat(parsed).extracting(RoomEvent::sender).isEqualTo("@u:b");
              assertThat(parsed).extracting(RoomEvent::type).isEqualTo("m.room.message");
              assertThat(parsed).extracting(e -> e.content().toJson()).isEqualTo("{}");
            });
  }

  @Test
  void roomEventFromParsesFields() {
    RoomEvent event =
        RoomEvent.from(
            JsonParser.parse(
                "{\"event_id\":\"$e1\",\"sender\":\"@u:b\",\"type\":\"m.room.message\","
                    + "\"content\":{\"body\":\"hi\"}}"));
    assertThat(event)
        .extracting(RoomEvent::eventId, RoomEvent::sender, RoomEvent::type)
        .containsExactly("$e1", "@u:b", "m.room.message");
    assertThat(event).extracting(RoomEvent::stateKey).isNull();
    assertThat(event).extracting(RoomEvent::isState, BOOLEAN).isFalse();
    assertThat(event.content().asObject().get("body").asString()).isEqualTo("hi");
  }

  @Test
  void roomEventFromParsesStateEvents() {
    RoomEvent event =
        RoomEvent.from(
            JsonParser.parse(
                "{\"event_id\":\"$s1\",\"sender\":\"@u:b\",\"type\":\"m.room.name\","
                    + "\"state_key\":\"\",\"content\":{\"name\":\"X\"}}"));
    assertThat(event).extracting(RoomEvent::stateKey).isEqualTo("");
    assertThat(event).extracting(RoomEvent::isState, BOOLEAN).isTrue();
  }

  @Test
  void roomEventFromPreservesUnknownFields() {
    RoomEvent event =
        RoomEvent.from(
            JsonParser.parse(
                "{\"type\":\"org.example.custom\",\"content\":{\"x\":1},\"unsigned\":{\"age\":5}}"));
    assertThat(event).extracting(RoomEvent::type).isEqualTo("org.example.custom");
    assertThat(event.content().asObject().get("x").asDouble()).isEqualTo(1.0);
  }

  @Test
  void roomEventFromRejectsNonObject() {
    assertThatIllegalArgumentException().isThrownBy(() -> RoomEvent.from(JsonParser.parse("[]")));
  }
}
