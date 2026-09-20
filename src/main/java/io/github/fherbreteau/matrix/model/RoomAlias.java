package io.github.fherbreteau.matrix.model;

/** A Matrix room alias, of the form {@code #localpart:serverName}. */
public record RoomAlias(String value) {

  /** Creates a validated room alias. */
  public RoomAlias {
    RoomId.validate(value, '#');
  }

  /**
   * Creates a room alias from its string representation.
   *
   * @throws IllegalArgumentException if the value is not a valid room alias
   */
  public static RoomAlias of(String value) {
    return new RoomAlias(value);
  }

  @Override
  public String toString() {
    return value;
  }
}
