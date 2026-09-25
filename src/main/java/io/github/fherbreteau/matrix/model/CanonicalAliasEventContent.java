package io.github.fherbreteau.matrix.model;

import io.github.fherbreteau.matrix.json.JsonValue;

/**
 * Content of an {@code m.room.canonical_alias} state event, including the original JSON.
 *
 * @see <a href="https://spec.matrix.org/latest/client-server-api/#mroomcanonical_alias">Matrix
 *     specification</a>
 */
public record CanonicalAliasEventContent(String alias, JsonValue altAliases, JsonValue raw)
    implements EventContent {

  /** Parses the canonical alias fields while retaining unknown fields. */
  public static CanonicalAliasEventContent from(JsonValue content) {
    return new CanonicalAliasEventContent(
        EventFields.string(content, "alias"), EventFields.field(content, "alt_aliases"), content);
  }
}
