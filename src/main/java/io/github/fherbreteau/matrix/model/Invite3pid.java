package io.github.fherbreteau.matrix.model;

import io.github.fherbreteau.matrix.json.JsonObject;
import io.github.fherbreteau.matrix.json.JsonValue;

/**
 * A third-party identity to invite to a room at creation time, resolved through an identity server.
 * Each field is required by the {@code invite_3pid} entry of the room creation request.
 *
 * @see <a
 *     href="https://spec.matrix.org/latest/client-server-api/#post_matrixclientv3createroom">Matrix
 *     specification</a>
 */
public record Invite3pid(String idServer, String idAccessToken, String medium, String address) {

  /** Creates a validated third-party invite. */
  public Invite3pid {
    requireNonBlank(idServer, "id_server");
    requireNonBlank(idAccessToken, "id_access_token");
    requireNonBlank(medium, "medium");
    requireNonBlank(address, "address");
  }

  /**
   * Creates a third-party invite from its parts.
   *
   * @throws IllegalArgumentException if any field is blank
   */
  public static Invite3pid of(
      String idServer, String idAccessToken, String medium, String address) {
    return new Invite3pid(idServer, idAccessToken, medium, address);
  }

  /**
   * Serializes the invite as an {@code invite_3pid} entry.
   *
   * @return the invite entry as a JSON object
   */
  public JsonValue toJson() {
    return new JsonObject()
        .put("id_server", idServer)
        .put("id_access_token", idAccessToken)
        .put("medium", medium)
        .put("address", address);
  }

  private static void requireNonBlank(String value, String name) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(name + " is required");
    }
  }
}
