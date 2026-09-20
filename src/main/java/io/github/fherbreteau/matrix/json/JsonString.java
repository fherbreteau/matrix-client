package io.github.fherbreteau.matrix.json;

/** A JSON string value. */
public final class JsonString implements JsonValue {

  private final String value;

  private JsonString(String value) {
    this.value = value;
  }

  /** Creates a JSON string value. */
  public static JsonString of(String value) {
    return new JsonString(value);
  }

  @Override
  public boolean isString() {
    return true;
  }

  @Override
  public String asString() {
    return value;
  }

  @Override
  public String toJson() {
    var sb = new StringBuilder("\"");
    for (int i = 0; i < value.length(); i++) {
      char c = value.charAt(i);
      switch (c) {
        case '"' -> sb.append("\\\"");
        case '\\' -> sb.append("\\\\");
        case '\n' -> sb.append("\\n");
        case '\r' -> sb.append("\\r");
        case '\t' -> sb.append("\\t");
        case '\b' -> sb.append("\\b");
        case '\f' -> sb.append("\\f");
        default -> {
          if (c < 0x20) {
            sb.append(String.format("\\u%04x", (int) c));
          } else {
            sb.append(c);
          }
        }
      }
    }
    return sb.append('"').toString();
  }
}
