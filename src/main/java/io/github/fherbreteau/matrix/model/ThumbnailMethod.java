package io.github.fherbreteau.matrix.model;

/**
 * The resize method requested for a thumbnail, as defined by the Matrix specification: {@code
 * scale} keeps the aspect ratio, {@code crop} matches the requested aspect ratio.
 *
 * @see <a
 *     href="https://spec.matrix.org/latest/client-server-api/#get_matrixclientv1mediathumbnailservernamemediaid">Matrix
 *     specification</a>
 */
public enum ThumbnailMethod {

  /** Keep the aspect ratio of the original media. */
  SCALE("scale"),

  /** Match the requested aspect ratio, cropping as needed. */
  CROP("crop");

  private final String value;

  ThumbnailMethod(String value) {
    this.value = value;
  }

  /**
   * Returns the query-parameter value expected by the homeserver.
   *
   * @return {@code scale} or {@code crop}
   * @see <a
   *     href="https://spec.matrix.org/latest/client-server-api/#get_matrixclientv1mediathumbnailservernamemediaid">Matrix
   *     specification</a>
   */
  public String value() {
    return value;
  }
}
