package io.github.fherbreteau.matrix.model;

import java.util.ArrayList;
import java.util.List;

/** A Matrix room as seen by the client. */
public final class Room {

  private final String roomId;
  private final List<RoomEvent> events;

  /**
   * Creates a room with the given identifier and no events.
   *
   * @param roomId the room identifier
   * @see <a href="https://spec.matrix.org/latest/client-server-api/#room-event-format">Matrix
   *     specification</a>
   */
  public Room(String roomId) {
    this(roomId, new ArrayList<>());
  }

  /**
   * Creates a room with the given identifier and events.
   *
   * @param roomId the room identifier
   * @param events the initial events of the room
   */
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
