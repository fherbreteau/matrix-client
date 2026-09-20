package io.github.fherbreteau.matrix.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.list;

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
        var event = new RoomEvent("$e", "@u:b", "m.room.message", "{}");
        Room room = new Room("!a:b", List.of(event));
        assertThat(room).extracting(Room::getRoomId).isEqualTo("!a:b");
        assertThat(room).extracting(Room::getEvents, list(RoomEvent.class)).hasSize(1);
        assertThat(room).extracting(Room::getEvents, list(RoomEvent.class))
                .singleElement()
                .extracting(RoomEvent::eventId)
                .isEqualTo("$e");
        assertThat(room).extracting(Room::getEvents, list(RoomEvent.class))
                .singleElement()
                .extracting(RoomEvent::sender)
                .isEqualTo("@u:b");
        assertThat(room).extracting(Room::getEvents, list(RoomEvent.class))
                .singleElement()
                .extracting(RoomEvent::type)
                .isEqualTo("m.room.message");
        assertThat(room).extracting(Room::getEvents, list(RoomEvent.class))
                .singleElement()
                .extracting(RoomEvent::content)
                .isEqualTo("{}");
    }
}
