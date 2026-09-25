package io.github.fherbreteau.matrix.model;

import io.github.fherbreteau.matrix.json.JsonValue;
import java.util.Optional;

/**
 * Optional store for application-defined metadata associated with a Matrix content URI. This
 * abstraction does not cache or persist media bytes.
 *
 * @see <a href="https://spec.matrix.org/latest/client-server-api/#content-repository">Matrix
 *     specification</a>
 */
public interface MediaMetadataStore {

  /**
   * Stores metadata for a content URI.
   *
   * @param contentUri the URI identifying the media
   * @param metadata arbitrary JSON metadata
   */
  void put(String contentUri, JsonValue metadata);

  /**
   * Returns metadata associated with a content URI.
   *
   * @param contentUri the URI identifying the media
   * @return stored metadata, if available
   */
  Optional<JsonValue> get(String contentUri);

  /**
   * Removes metadata associated with a content URI.
   *
   * @param contentUri the URI identifying the media
   */
  void remove(String contentUri);

  /**
   * Creates an empty thread-safe in-memory metadata store.
   *
   * @return a new empty metadata store
   */
  static MediaMetadataStore inMemory() {
    return new InMemoryMediaMetadataStore();
  }
}
