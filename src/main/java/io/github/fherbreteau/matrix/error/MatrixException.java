package io.github.fherbreteau.matrix.error;

/** Base exception for all Matrix client errors. */
public class MatrixException extends RuntimeException {

  private final String errcode;

  /**
   * Creates a Matrix exception with the given error code and message.
   *
   * @param errcode the Matrix error code, if any
   * @param message the error message
   */
  public MatrixException(String errcode, String message) {
    super(message);
    this.errcode = errcode;
  }

  /**
   * Creates a Matrix exception with the given error code, message and cause.
   *
   * @param errcode the Matrix error code, if any
   * @param message the error message
   * @param cause the underlying cause
   */
  public MatrixException(String errcode, String message, Throwable cause) {
    super(message, cause);
    this.errcode = errcode;
  }

  /**
   * Returns the Matrix error code (for example {@code M_FORBIDDEN}), if any.
   *
   * @return the Matrix error code, or {@code null} when absent
   */
  public String getErrcode() {
    return errcode;
  }
}
