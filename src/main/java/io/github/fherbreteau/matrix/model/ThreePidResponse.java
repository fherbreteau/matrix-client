package io.github.fherbreteau.matrix.model;

import io.github.fherbreteau.matrix.json.JsonArray;
import io.github.fherbreteau.matrix.json.JsonObject;
import io.github.fherbreteau.matrix.json.JsonValue;
import java.util.ArrayList;
import java.util.List;

/**
 * The third-party identifiers associated with the current Matrix account.
 *
 * @see <a
 *     href="https://spec.matrix.org/latest/client-server-api/#get_matrixclientv3account3pid">Matrix
 *     specification</a>
 */
public record ThreePidResponse(List<ThreePid> threepids, JsonValue raw) {

  /** Creates an immutable result. */
  public ThreePidResponse {
    threepids = List.copyOf(threepids);
  }

  /** Parses the associated third-party identifiers response. */
  public static ThreePidResponse from(JsonValue value) {
    if (value == null || !value.isObject()) {
      throw new IllegalArgumentException("Third-party identifier response must be a JSON object");
    }
    JsonValue threepidsValue = value.asObject().get("threepids");
    if (threepidsValue == null || !threepidsValue.isArray()) {
      throw new IllegalArgumentException("Third-party identifier response must contain threepids");
    }
    List<ThreePid> threepids = new ArrayList<>();
    JsonArray array = threepidsValue.asArray();
    for (int index = 0; index < array.size(); index++) {
      threepids.add(ThreePid.from(array.get(index)));
    }
    return new ThreePidResponse(threepids, value);
  }

  /** A third-party identifier associated with the Matrix account. */
  public record ThreePid(
      String address, String medium, Long addedAt, Long validatedAt, JsonValue raw) {

    private static ThreePid from(JsonValue value) {
      if (value == null || !value.isObject()) {
        throw new IllegalArgumentException("Third-party identifier must be a JSON object");
      }
      JsonObject object = value.asObject();
      return new ThreePid(
          requiredString(object, "address"),
          requiredString(object, "medium"),
          requiredLong(object, "added_at"),
          requiredLong(object, "validated_at"),
          value);
    }

    private static String requiredString(JsonObject object, String field) {
      JsonValue value = object.get(field);
      if (value == null || !value.isString()) {
        throw new IllegalArgumentException("Third-party identifier must contain " + field);
      }
      return value.asString();
    }

    private static Long requiredLong(JsonObject object, String field) {
      JsonValue value = object.get(field);
      if (value == null || !value.isNumber()) {
        throw new IllegalArgumentException("Third-party identifier must contain " + field);
      }
      return value.asLong();
    }
  }
}
