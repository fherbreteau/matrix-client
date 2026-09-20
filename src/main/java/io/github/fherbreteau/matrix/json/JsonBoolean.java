package io.github.fherbreteau.matrix.json;

/** A JSON boolean value. */
public final class JsonBoolean implements JsonValue {

  public static final JsonBoolean TRUE = new JsonBoolean(true);
  public static final JsonBoolean FALSE = new JsonBoolean(false);

  private final boolean value;

  private JsonBoolean(boolean value) {
    this.value = value;
  }

  /**
   * Returns the {@link JsonBoolean} constant for the given value.
   *
   * @param value the boolean value
   * @return the {@link JsonBoolean} constant for the value
   */
  public static JsonBoolean of(boolean value) {
    return value ? TRUE : FALSE;
  }

  @Override
  public boolean isBoolean() {
    return true;
  }

  @Override
  public boolean asBoolean() {
    return value;
  }

  @Override
  public String toJson() {
    return Boolean.toString(value);
  }
}
