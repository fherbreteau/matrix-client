package io.github.fherbreteau.matrix.transport;

/** Signals that a media transfer exceeded its configured size limit. */
public final class MediaSizeLimitException extends RuntimeException {

  /**
   * Creates an exception for the configured maximum media size.
   *
   * @param maximumBytes the maximum permitted size in bytes
   */
  public MediaSizeLimitException(long maximumBytes) {
    super("Media transfer exceeds the configured maximum size of " + maximumBytes + " bytes");
  }
}
