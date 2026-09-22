package io.github.fherbreteau.matrix.model;

/** A Matrix event identifier, of the form {@code $opaqueId}. */
public record EventId(String value) {

  /** Creates a validated event identifier. */
  public EventId {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException("identifier is required");
    }
    if (value.charAt(0) != '$') {
      throw new IllegalArgumentException("identifier must start with '$': " + value);
    }
    if (value.length() < 2) {
      throw new IllegalArgumentException("identifier must have a non-empty localpart: " + value);
    }
  }

  /**
   * Creates an event identifier from its string representation.
   *
   * @throws IllegalArgumentException if the value is not a valid event identifier
   */
  public static EventId of(String value) {
    return new EventId(value);
  }

  @Override
  public String toString() {
    return value;
  }
}
