package io.github.fherbreteau.matrix.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RoomTest {

    @Test
    void roomIdConstructor() {
        Room room = new Room("!a:b");
        assertThat(room.getRoomId()).isEqualTo("!a:b");
        assertThat(room.getEvents()).isEmpty();
    }

    @Test
    void eventsConstructor() {
        var event = new RoomEvent("$e", "@u:b", "m.room.message", "{}");
        Room room = new Room("!a:b", List.of(event));
        assertThat(room.getRoomId()).isEqualTo("!a:b");
        assertThat(room.getEvents()).hasSize(1);
        assertThat(room.getEvents().get(0).eventId()).isEqualTo("$e");
        assertThat(room.getEvents().get(0).sender()).isEqualTo("@u:b");
        assertThat(room.getEvents().get(0).type()).isEqualTo("m.room.message");
        assertThat(room.getEvents().get(0).content()).isEqualTo("{}");
    }
}
