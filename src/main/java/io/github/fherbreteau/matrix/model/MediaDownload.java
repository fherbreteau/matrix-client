package io.github.fherbreteau.matrix.model;

import java.io.InputStream;

/**
 * Downloaded media (or a thumbnail) from the content repository: the MIME type and {@code
 * Content-Disposition} reported by the homeserver, plus the body as a stream so large media is
 * never fully buffered by the client. Callers must close the stream.
 */
public final class MediaDownload implements AutoCloseable {

  private final String contentType;
  private final String contentDisposition;
  private final InputStream body;

  /**
   * Creates a media download response.
   *
   * @param contentType the MIME type reported by the homeserver
   * @param contentDisposition the content disposition of the response
   * @param body the media body stream
   */
  public MediaDownload(String contentType, String contentDisposition, InputStream body) {
    this.contentType = contentType;
    this.contentDisposition = contentDisposition;
    this.body = body;
  }

  /**
   * Returns the MIME type the homeserver reports for the media.
   *
   * @return the {@code Content-Type} of the response, if present
   */
  public String contentType() {
    return contentType;
  }

  /**
   * Returns the {@code Content-Disposition} of the response ({@code inline} or {@code attachment},
   * possibly with a filename).
   *
   * @return the content disposition, if present
   */
  public String contentDisposition() {
    return contentDisposition;
  }

  /**
   * Returns the media body; the stream must be closed by the caller.
   *
   * @return the media body stream
   */
  public InputStream body() {
    return body;
  }

  @Override
  public void close() {
    try {
      body.close();
    } catch (Exception _) {
      // closing twice or a closed stream is harmless
    }
  }
}
