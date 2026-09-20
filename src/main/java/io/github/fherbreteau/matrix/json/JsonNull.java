package io.github.fherbreteau.matrix.json;

/** The JSON {@code null} value. */
public final class JsonNull implements JsonValue {

  public static final JsonNull INSTANCE = new JsonNull();

  private JsonNull() {}

  @Override
  public boolean isNull() {
    return true;
  }

  @Override
  public String toJson() {
    return "null";
  }
}
