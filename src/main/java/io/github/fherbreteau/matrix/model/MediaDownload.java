package io.github.fherbreteau.matrix.model;

import java.io.InputStream;

/**
 * Downloaded media (or a thumbnail) from the content repository: the MIME type and {@code
 * Content-Disposition} reported by the homeserver, plus the body as a stream so large media is
 * never fully buffered by the client. Callers must close the instance.
 *
 * @see <a href="https://spec.matrix.org/latest/client-server-api/#downloading-content">Matrix
 *     specification</a>
 */
public record MediaDownload(String contentType, String contentDisposition, InputStream body)
    implements AutoCloseable {

  /**
   * Closes the media body stream.
   *
   * @see <a href="https://spec.matrix.org/latest/client-server-api/#downloading-content">Matrix
   *     specification</a>
   */
  @Override
  public void close() {
    try {
      body.close();
    } catch (Exception _) {
      // closing twice or a closed stream is harmless
    }
  }
}
