package io.github.fherbreteau.matrix.model;

/** The presence state of a user, as defined by the Matrix specification. */
public enum Presence {

  /** The user is online. */
  ONLINE("online"),

  /** The user is offline. */
  OFFLINE("offline"),

  /** The user is online but idle. */
  UNAVAILABLE("unavailable");

  private final String value;

  Presence(String value) {
    this.value = value;
  }

  /**
   * Returns the wire value sent to the homeserver.
   *
   * @return {@code online}, {@code offline} or {@code unavailable}
   */
  public String value() {
    return value;
  }

  /**
   * Resolves a presence value from its wire representation.
   *
   * @param value the wire value, as returned by the homeserver
   * @return the matching presence
   * @throws IllegalArgumentException if the value is not a spec-defined presence
   */
  public static Presence from(String value) {
    for (Presence presence : values()) {
      if (presence.value.equals(value)) {
        return presence;
      }
    }
    throw new IllegalArgumentException("Unknown presence: " + value);
  }
}
