package io.github.fherbreteau.matrix.model;

/** A Matrix room identifier, of the form {@code !localpart:serverName}. */
public record RoomId(String value) {

  /** Creates a validated room identifier. */
  public RoomId {
    validate(value, '!');
  }

  /**
   * Creates a room identifier from its string representation.
   *
   * @throws IllegalArgumentException if the value is not a valid room identifier
   */
  public static RoomId of(String value) {
    return new RoomId(value);
  }

  static void validate(String value, char prefix) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException("identifier is required");
    }
    if (value.charAt(0) != prefix) {
      throw new IllegalArgumentException("identifier must start with '" + prefix + "': " + value);
    }
    int colon = value.indexOf(':');
    if (colon < 2 || colon == value.length() - 1) {
      throw new IllegalArgumentException(
          "identifier must contain a non-empty localpart and server name: " + value);
    }
  }

  @Override
  public String toString() {
    return value;
  }
}
