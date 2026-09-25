package io.github.fherbreteau.matrix.model;

/**
 * Event-format selection for `/sync` responses: client events (the default) or federation-format
 * events. Most clients should use {@link #CLIENT}.
 *
 * @see <a href="https://spec.matrix.org/latest/client-server-api/#syncing">Matrix specification</a>
 */
public enum EventFormat {

  /** Client event format (the default). */
  CLIENT("client"),

  /** Federation event format. */
  FEDERATION("federation");

  private final String value;

  EventFormat(String value) {
    this.value = value;
  }

  /**
   * Returns the wire value used in the `event_format` filter field.
   *
   * @return the event-format wire value
   */
  public String value() {
    return value;
  }
}
