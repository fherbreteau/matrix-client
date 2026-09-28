package io.github.fherbreteau.matrix.model;

import io.github.fherbreteau.matrix.json.JsonObject;
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
    if (content == null || !content.isObject()) {
      return null;
    }
    JsonObject object = content.asObject();
    JsonValue alias = object.get("alias");
    if ((alias != null && !alias.isNull() && !alias.isString())
        || EventFields.hasWrongStringArray(object, "alt_aliases")) {
      return null;
    }
    return new CanonicalAliasEventContent(
        EventFields.string(content, "alias"), EventFields.field(content, "alt_aliases"), content);
  }
}
