package io.github.fherbreteau.matrix.model;

import io.github.fherbreteau.matrix.json.JsonObject;
import io.github.fherbreteau.matrix.json.JsonValue;
import java.util.Objects;

/**
 * Optional details for a content report.
 *
 * @see <a href="https://spec.matrix.org/latest/client-server-api/#reporting-content">Matrix
 *     specification</a>
 */
public record ContentReport(String reason) {

  private static final String REASON_FIELD = "reason";

  /**
   * Creates a report without a reason.
   *
   * @return an empty report
   */
  public static ContentReport empty() {
    return new ContentReport(null);
  }

  /**
   * Creates a report with a reason.
   *
   * @param reason the report reason
   * @return the report
   */
  public static ContentReport withReason(String reason) {
    return new ContentReport(Objects.requireNonNull(reason, REASON_FIELD));
  }

  /**
   * Serializes the report body.
   *
   * @return report details as JSON
   */
  public JsonObject toJson() {
    JsonObject body = new JsonObject();
    if (reason != null) {
      body.put(REASON_FIELD, reason);
    }
    return body;
  }

  /**
   * Parses report details from JSON.
   *
   * @param value the report details JSON
   * @return the report details
   * @throws IllegalArgumentException if the reason has an invalid type
   */
  public static ContentReport from(JsonValue value) {
    if (value == null || !value.isObject()) {
      throw new IllegalArgumentException("content report must be a JSON object");
    }
    JsonValue reasonValue = value.asObject().get(REASON_FIELD);
    if (reasonValue != null && !reasonValue.isString()) {
      throw new IllegalArgumentException("report reason must be a string");
    }
    return new ContentReport(reasonValue == null ? null : reasonValue.asString());
  }
}
