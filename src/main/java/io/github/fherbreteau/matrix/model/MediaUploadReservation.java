package io.github.fherbreteau.matrix.model;

import io.github.fherbreteau.matrix.json.JsonObject;
import io.github.fherbreteau.matrix.json.JsonValue;
import java.math.BigDecimal;

/**
 * An MXC URI reserved for a later content upload.
 *
 * @see <a href="https://spec.matrix.org/latest/client-server-api/#post_matrixmediav1create">Matrix
 *     specification</a>
 */
public record MediaUploadReservation(MxcUri contentUri, Long unusedExpiresAt, JsonValue raw) {

  /**
   * Parses a media upload reservation response.
   *
   * @param value the response JSON
   * @return the parsed reservation
   * @throws IllegalArgumentException if required fields are absent or malformed
   */
  public static MediaUploadReservation from(JsonValue value) {
    if (value == null || !value.isObject()) {
      throw new IllegalArgumentException("media upload reservation must be a JSON object");
    }
    JsonObject object = value.asObject();
    JsonValue contentUriValue = object.get("content_uri");
    if (contentUriValue == null || !contentUriValue.isString()) {
      throw new IllegalArgumentException("media upload reservation must contain content_uri");
    }
    JsonValue expiryValue = object.get("unused_expires_at");
    Long expiry = null;
    if (expiryValue != null) {
      if (!expiryValue.isNumber()) {
        throw new IllegalArgumentException("unused_expires_at must be an integer");
      }
      try {
        BigDecimal expiryNumber = expiryValue.asBigDecimal();
        expiry = expiryNumber.longValueExact();
      } catch (ArithmeticException exception) {
        throw new IllegalArgumentException("unused_expires_at must be a long integer", exception);
      }
    }
    return new MediaUploadReservation(MxcUri.parse(contentUriValue.asString()), expiry, value);
  }
}
