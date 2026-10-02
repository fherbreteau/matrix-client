package io.github.fherbreteau.matrix.model;

import io.github.fherbreteau.matrix.json.JsonValue;

/**
 * Result of account deactivation or a third-party identifier deletion/unbinding operation.
 *
 * @see <a
 *     href="https://spec.matrix.org/latest/client-server-api/#post_matrixclientv3accountdeactivate">Matrix
 *     specification</a>
 */
public record AccountOperationResponse(String idServerUnbindResult, JsonValue raw) {

  /** Parses an account operation response. */
  public static AccountOperationResponse from(JsonValue value) {
    if (value == null || !value.isObject()) {
      throw new IllegalArgumentException("Account operation response must be a JSON object");
    }
    JsonValue result = value.asObject().get("id_server_unbind_result");
    if (result == null || !result.isString()) {
      throw new IllegalArgumentException(
          "Account operation response must contain id_server_unbind_result");
    }
    return new AccountOperationResponse(result.asString(), value);
  }
}
