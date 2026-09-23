package io.github.fherbreteau.matrix.model;

/** A Matrix device identifier: an opaque string without colons or whitespace. */
public record DeviceId(String value) {

  /** Creates a validated device identifier. */
  public DeviceId {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException("identifier is required");
    }
    if (value.indexOf(':') >= 0 || value.chars().anyMatch(Character::isWhitespace)) {
      throw new IllegalArgumentException(
          "identifier must not contain colons or whitespace: " + value);
    }
  }

  /**
   * Creates a device identifier from its string representation.
   *
   * @throws IllegalArgumentException if the value is not a valid device identifier
   * @see <a href="https://spec.matrix.org/latest/appendices/#event-ids">Matrix specification</a>
   */
  public static DeviceId of(String value) {
    return new DeviceId(value);
  }

  @Override
  public String toString() {
    return value;
  }
}
