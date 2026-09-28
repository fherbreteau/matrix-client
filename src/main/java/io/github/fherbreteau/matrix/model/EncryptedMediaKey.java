package io.github.fherbreteau.matrix.model;

import io.github.fherbreteau.matrix.json.JsonValue;
import java.util.List;

/**
 * Parsed JWK fields used by Matrix encrypted media, retaining the original JSON.
 *
 * @see <a
 *     href="https://spec.matrix.org/latest/client-server-api/#definition-encryptedfile_jwk">Matrix
 *     specification</a>
 */
public record EncryptedMediaKey(
    String algorithm,
    boolean extractable,
    String keyData,
    List<String> keyOperations,
    String keyType,
    JsonValue raw) {

  static EncryptedMediaKey from(JsonValue value) {
    if (value == null || !value.isObject()) {
      return null;
    }
    String alg = EventFields.string(value, "alg");
    JsonValue ext = EventFields.field(value, "ext");
    String key = EventFields.string(value, "k");
    JsonValue operationsValue = EventFields.field(value, EventFields.ENCRYPTED_KEY_OPERATIONS);
    List<String> operations = EventFields.stringList(operationsValue);
    String type = EventFields.string(value, "kty");
    if (EventFields.hasWrongEncryptedKey(value)) {
      return null;
    }
    if (alg == null
        || ext == null
        || !ext.isBoolean()
        || key == null
        || operations == null
        || type == null) {
      return null;
    }
    if (!"A256CTR".equals(alg)
        || !ext.asBoolean()
        || !operations.contains("encrypt")
        || !operations.contains("decrypt")
        || !"oct".equals(type)) {
      return null;
    }
    return new EncryptedMediaKey(alg, ext.asBoolean(), key, operations, type, value);
  }
}
