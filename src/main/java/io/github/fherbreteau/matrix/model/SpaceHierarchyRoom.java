package io.github.fherbreteau.matrix.model;

import io.github.fherbreteau.matrix.json.JsonObject;
import io.github.fherbreteau.matrix.json.JsonValue;
import java.util.ArrayList;
import java.util.List;

/**
 * A room entry in a space hierarchy response.
 *
 * @see <a
 *     href="https://spec.matrix.org/latest/client-server-api/#get_matrixclientv1roomsroomidhierarchy">Matrix
 *     specification</a>
 */
public record SpaceHierarchyRoom(
    RoomId roomId,
    List<RoomId> allowedRoomIds,
    String avatarUrl,
    String canonicalAlias,
    List<SpaceChildStateEvent> childrenState,
    String encryption,
    boolean guestCanJoin,
    String joinRule,
    String name,
    long numJoinedMembers,
    String roomType,
    String roomVersion,
    String topic,
    boolean worldReadable,
    JsonValue raw) {

  /** Copies hierarchy collections to immutable lists. */
  public SpaceHierarchyRoom {
    allowedRoomIds = List.copyOf(allowedRoomIds);
    childrenState = List.copyOf(childrenState);
  }

  /**
   * Parses a hierarchy room.
   *
   * @param value hierarchy room JSON
   * @return parsed hierarchy room
   * @throws IllegalArgumentException if required fields are absent or malformed
   */
  public static SpaceHierarchyRoom from(JsonValue value) {
    JsonObject object = ModelJson.object(value, "space hierarchy room");
    JsonValue childrenValue = object.get("children_state");
    Boolean guest = ModelJson.bool(object, "guest_can_join");
    Long members = ModelJson.number(object, "num_joined_members");
    Boolean readable = ModelJson.bool(object, "world_readable");
    if (guest == null
        || members == null
        || readable == null
        || childrenValue == null
        || !childrenValue.isArray()) {
      throw new IllegalArgumentException("space hierarchy room is missing required fields");
    }
    String roomId = ModelJson.requiredString(object, "room_id", "space hierarchy room");
    List<RoomId> allowed =
        ModelJson.strings(object.get("allowed_room_ids"), "allowed_room_ids").stream()
            .map(RoomId::of)
            .toList();
    List<SpaceChildStateEvent> children = new ArrayList<>();
    for (int index = 0; index < childrenValue.asArray().size(); index++) {
      children.add(SpaceChildStateEvent.from(childrenValue.asArray().get(index)));
    }
    return new SpaceHierarchyRoom(
        RoomId.of(roomId),
        allowed,
        ModelJson.string(object, "avatar_url"),
        ModelJson.string(object, "canonical_alias"),
        children,
        ModelJson.string(object, "encryption"),
        guest,
        ModelJson.string(object, "join_rule"),
        ModelJson.string(object, "name"),
        members,
        ModelJson.string(object, "room_type"),
        ModelJson.string(object, "room_version"),
        ModelJson.string(object, "topic"),
        readable,
        value);
  }
}
