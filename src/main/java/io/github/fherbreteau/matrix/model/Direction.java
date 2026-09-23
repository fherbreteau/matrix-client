package io.github.fherbreteau.matrix.model;

/**
 * The direction to walk when paginating room history: {@code b} (backwards, older events) or {@code
 * f} (forwards, newer events).
 */
public enum Direction {

  /** Paginate backwards, towards older events. */
  BACKWARD("b"),

  /** Paginate forwards, towards newer events. */
  FORWARD("f");

  private final String value;

  Direction(String value) {
    this.value = value;
  }

  /**
   * Returns the query-parameter value expected by the homeserver.
   *
   * @return {@code b} for backwards pagination, {@code f} for forwards
   */
  public String value() {
    return value;
  }
}
