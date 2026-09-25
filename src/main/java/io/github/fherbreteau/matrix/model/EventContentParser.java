package io.github.fherbreteau.matrix.model;

import io.github.fherbreteau.matrix.json.JsonValue;

/**
 * Parses event content for one Matrix event type into an application model.
 *
 * @param <T> parsed event-content model
 */
@FunctionalInterface
public interface EventContentParser<T> {

  /**
   * Parses the raw content object.
   *
   * @param content the raw event content, which may be absent or malformed
   * @return typed content, or {@code null} to trigger raw fallback
   */
  T parse(JsonValue content);
}
