package io.github.fherbreteau.matrix.model;

import io.github.fherbreteau.matrix.error.DiscoveryException;

/**
 * A Matrix content URI ({@code mxc://<server-name>/<media-id>}) referencing media in the content
 * repository. Media identifiers may only contain URL-safe characters ({@code [A-Za-z0-9_-]}); a URI
 * whose media ID contains other characters (such as {@code /} or {@code ..}) is rejected to prevent
 * path traversal.
 */
public record MxcUri(String serverName, String mediaId) {

  private static final String SCHEME = "mxc://";

  /**
   * Creates and validates a Matrix content URI.
   *
   * @throws IllegalArgumentException if the server name or media ID is missing or invalid
   */
  public MxcUri {
    if (serverName == null || serverName.isBlank()) {
      throw new IllegalArgumentException("server name is required");
    }
    if (mediaId == null || mediaId.isBlank()) {
      throw new IllegalArgumentException("media ID is required");
    }
    if (!mediaId.matches("[A-Za-z0-9_-]+")) {
      throw new IllegalArgumentException("media ID may only contain [A-Za-z0-9_-]: " + mediaId);
    }
  }

  /**
   * Parses an {@code mxc://} URI.
   *
   * @param uri the URI to parse, in the {@code mxc://<server>/<mediaId>} form
   * @return the parsed content URI
   * @throws DiscoveryException if the URI is not a valid {@code mxc://} URI
   */
  public static MxcUri parse(String uri) {
    if (uri == null || !uri.startsWith(SCHEME)) {
      throw new DiscoveryException("Not a mxc:// URI: " + uri);
    }
    String withoutScheme = uri.substring(SCHEME.length());
    int slash = withoutScheme.indexOf('/');
    if (slash <= 0 || slash == withoutScheme.length() - 1) {
      throw new DiscoveryException("mxc:// URI must be mxc://<server>/<mediaId>: " + uri);
    }
    try {
      return new MxcUri(withoutScheme.substring(0, slash), withoutScheme.substring(slash + 1));
    } catch (IllegalArgumentException e) {
      throw new DiscoveryException(e.getMessage());
    }
  }

  /**
   * Creates a Matrix content URI from its parts.
   *
   * @param serverName the server name
   * @param mediaId the media identifier
   * @return the parsed content URI
   */
  public static MxcUri of(String serverName, String mediaId) {
    return new MxcUri(serverName, mediaId);
  }

  @Override
  public String toString() {
    return SCHEME + serverName + "/" + mediaId;
  }
}
