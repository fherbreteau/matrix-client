package io.github.fherbreteau.matrix.model;

import io.github.fherbreteau.matrix.json.JsonObject;
import io.github.fherbreteau.matrix.json.JsonValue;
import java.util.List;

/**
 * A summary of a room returned by the room-summary endpoint. Unknown fields remain in {@link
 * #raw()}.
 *
 * @see <a
 *     href="https://spec.matrix.org/latest/client-server-api/#get_matrixclientv1room_summaryroomidoralias">Matrix
 *     specification</a>
 */
public record RoomSummary(
    RoomId roomId,
    List<RoomId> allowedRoomIds,
    String avatarUrl,
    String canonicalAlias,
    String encryption,
    boolean guestCanJoin,
    String joinRule,
    String membership,
    String name,
    long numJoinedMembers,
    String roomType,
    String roomVersion,
    String topic,
    boolean worldReadable,
    JsonValue raw) {

  /** Creates a summary with immutable allowed room IDs. */
  public RoomSummary {
    allowedRoomIds = List.copyOf(allowedRoomIds);
  }

  /**
   * Parses a room summary.
   *
   * @param value room summary JSON
   * @return parsed room summary
   * @throws IllegalArgumentException if required fields are absent or malformed
   */
  public static RoomSummary from(JsonValue value) {
    JsonObject object = ModelJson.object(value, "room summary");
    String roomId = ModelJson.requiredString(object, "room_id", "room summary");
    Boolean guest = ModelJson.bool(object, "guest_can_join");
    Long members = ModelJson.number(object, "num_joined_members");
    Boolean readable = ModelJson.bool(object, "world_readable");
    if (guest == null || members == null || readable == null) {
      throw new IllegalArgumentException("room summary is missing required fields");
    }
    List<RoomId> allowed =
        ModelJson.strings(object.get("allowed_room_ids"), "allowed_room_ids").stream()
            .map(RoomId::of)
            .toList();
    return new RoomSummary(
        RoomId.of(roomId),
        allowed,
        ModelJson.string(object, "avatar_url"),
        ModelJson.string(object, "canonical_alias"),
        ModelJson.string(object, "encryption"),
        guest,
        ModelJson.string(object, "join_rule"),
        ModelJson.string(object, "membership"),
        ModelJson.string(object, "name"),
        members,
        ModelJson.string(object, "room_type"),
        ModelJson.string(object, "room_version"),
        ModelJson.string(object, "topic"),
        readable,
        value);
  }
}
