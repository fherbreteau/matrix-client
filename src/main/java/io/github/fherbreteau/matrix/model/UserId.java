package io.github.fherbreteau.matrix.model;

/** A Matrix user identifier, of the form {@code @localpart:serverName}. */
public record UserId(String value) {

  /** Creates a validated user identifier. */
  public UserId {
    RoomId.validate(value, '@');
  }

  /**
   * Creates a user identifier from its string representation.
   *
   * @throws IllegalArgumentException if the value is not a valid user identifier
   */
  public static UserId of(String value) {
    return new UserId(value);
  }

  @Override
  public String toString() {
    return value;
  }
}
