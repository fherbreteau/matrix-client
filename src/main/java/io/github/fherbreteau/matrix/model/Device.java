package io.github.fherbreteau.matrix.model;

import io.github.fherbreteau.matrix.json.JsonValue;

/**
 * One device registered for the current Matrix account.
 *
 * @see <a href="https://spec.matrix.org/latest/client-server-api/#get_matrixclientv3devices">Matrix
 *     specification</a>
 */
public record Device(
    DeviceId deviceId, String displayName, String lastSeenIp, Long lastSeenTs, JsonValue raw) {

  /** Parses a device response and retains any unknown fields. */
  public static Device from(JsonValue value) {
    if (value == null || !value.isObject()) {
      throw new IllegalArgumentException("Device must be a JSON object");
    }
    JsonValue id = value.asObject().get("device_id");
    if (id == null || !id.isString()) {
      throw new IllegalArgumentException("Device must contain device_id");
    }
    JsonValue displayName = value.asObject().get("display_name");
    JsonValue lastSeenIp = value.asObject().get("last_seen_ip");
    JsonValue lastSeenTs = value.asObject().get("last_seen_ts");
    return new Device(
        DeviceId.of(id.asString()),
        displayName != null && displayName.isString() ? displayName.asString() : null,
        lastSeenIp != null && lastSeenIp.isString() ? lastSeenIp.asString() : null,
        lastSeenTs != null && lastSeenTs.isNumber() ? lastSeenTs.asLong() : null,
        value);
  }
}
