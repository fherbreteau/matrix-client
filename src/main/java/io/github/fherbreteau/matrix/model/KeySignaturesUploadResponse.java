package io.github.fherbreteau.matrix.model;

import static io.github.fherbreteau.matrix.model.ImmutableUtils.immutableMap;

import io.github.fherbreteau.matrix.error.DiscoveryException;
import io.github.fherbreteau.matrix.json.JsonObject;
import io.github.fherbreteau.matrix.json.JsonValue;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Optional per-key signature upload failures.
 *
 * @see <a
 *     href="https://spec.matrix.org/latest/client-server-api/#post_matrixclientv3keyssignaturesupload">Matrix
 *     specification</a>
 */
public record KeySignaturesUploadResponse(Map<UserId, Map<String, Failure>> failures) {
  /**
   * Creates a signature upload result.
   *
   * @param failures optional nested signature failures
   */
  public KeySignaturesUploadResponse {
    failures = immutableMap(failures);
  }

  /** Failure to apply one uploaded signature. */
  public record Failure(String errcode, String error) {}

  /**
   * Validate and parse a (/keys/signatures/upload) response body.
   *
   * @param body the parsed response body
   * @return the validated signature upload response
   * @throws DiscoveryException if the body is not a JSON object or does not contain a {@code
   *     versions} array of strings
   */
  public static KeySignaturesUploadResponse from(JsonValue body) {
    var response = ModelJson.object(body, "response");
    return new KeySignaturesUploadResponse(signatureFailures(response.get("failures")));
  }

  private static Map<UserId, Map<String, Failure>> signatureFailures(JsonValue value) {
    if (value == null) {
      return Map.of();
    }
    JsonObject users = value.asObject();
    Map<UserId, Map<String, Failure>> result = new LinkedHashMap<>();
    users
        .entrySet()
        .forEach(
            userEntry -> {
              Map<String, Failure> keys = new LinkedHashMap<>();
              userEntry
                  .getValue()
                  .asObject()
                  .entrySet()
                  .forEach(
                      keyEntry -> {
                        JsonObject failure = keyEntry.getValue().asObject();
                        keys.put(
                            keyEntry.getKey(),
                            new Failure(
                                ModelJson.requiredString(failure, "errcode", "failure"),
                                ModelJson.optionalString(failure, "error")));
                      });
              result.put(UserId.of(userEntry.getKey()), keys);
            });
    return result;
  }
}
