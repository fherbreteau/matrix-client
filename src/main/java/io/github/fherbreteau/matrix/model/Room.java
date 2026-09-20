package io.github.fherbreteau.matrix.model;

import java.util.ArrayList;
import java.util.List;

/**
 * A Matrix room as seen by the client.
 */
public final class Room {

    private final String roomId;
    private final List<RoomEvent> events;

    public Room(String roomId) {
        this(roomId, new ArrayList<>());
    }

    public Room(String roomId, List<RoomEvent> events) {
        this.roomId = roomId;
        this.events = events;
    }

    public String getRoomId() {
        return roomId;
    }

    public List<RoomEvent> getEvents() {
        return events;
    }
}
