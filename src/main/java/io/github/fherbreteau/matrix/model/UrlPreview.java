package io.github.fherbreteau.matrix.model;

import io.github.fherbreteau.matrix.json.JsonObject;
import io.github.fherbreteau.matrix.json.JsonValue;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * OpenGraph metadata returned for a URL preview.
 *
 * @see <a
 *     href="https://spec.matrix.org/latest/client-server-api/#get_matrixclientv1mediapreview_url">Matrix
 *     specification</a>
 */
public record UrlPreview(
    MxcUri imageUri, Long imageSize, Map<String, JsonValue> properties, JsonValue raw) {

  /** Copies preview properties into an immutable insertion-ordered map. */
  public UrlPreview {
    properties = Collections.unmodifiableMap(new LinkedHashMap<>(properties));
  }

  /**
   * Parses OpenGraph preview metadata.
   *
   * @param value the response JSON
   * @return the parsed preview
   * @throws IllegalArgumentException if the response is not a JSON object or known field types are
   *     invalid
   */
  public static UrlPreview from(JsonValue value) {
    if (value == null || !value.isObject()) {
      throw new IllegalArgumentException("URL preview must be a JSON object");
    }
    JsonObject object = value.asObject();
    JsonValue image = object.get("og:image");
    MxcUri imageUri = null;
    if (image != null) {
      if (!image.isString()) {
        throw new IllegalArgumentException("og:image must be a string");
      }
      imageUri = MxcUri.parse(image.asString());
    }
    JsonValue size = object.get("matrix:image:size");
    Long imageSize = null;
    if (size != null) {
      if (!size.isNumber()) {
        throw new IllegalArgumentException("matrix:image:size must be an integer");
      }
      try {
        imageSize = size.asBigDecimal().longValueExact();
      } catch (ArithmeticException exception) {
        throw new IllegalArgumentException("matrix:image:size must be a long integer", exception);
      }
    }
    Map<String, JsonValue> properties = new LinkedHashMap<>();
    object.entrySet().stream()
        .filter(
            entry -> entry.getKey().startsWith("og:") || entry.getKey().equals("matrix:image:size"))
        .forEach(entry -> properties.put(entry.getKey(), entry.getValue()));
    return new UrlPreview(imageUri, imageSize, properties, value);
  }
}
