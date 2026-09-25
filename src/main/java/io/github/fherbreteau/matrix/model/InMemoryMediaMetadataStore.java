package io.github.fherbreteau.matrix.model;

import io.github.fherbreteau.matrix.json.JsonValue;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Thread-safe in-memory implementation of {@link MediaMetadataStore}.
 *
 * @see <a href="https://spec.matrix.org/latest/client-server-api/#content-repository">Matrix
 *     specification</a>
 */
public final class InMemoryMediaMetadataStore implements MediaMetadataStore {

  private final Map<String, JsonValue> values = new HashMap<>();

  @Override
  public synchronized void put(String contentUri, JsonValue metadata) {
    validateUri(contentUri);
    if (metadata == null) {
      throw new IllegalArgumentException("metadata is required");
    }
    values.put(contentUri, metadata);
  }

  @Override
  public synchronized Optional<JsonValue> get(String contentUri) {
    validateUri(contentUri);
    return Optional.ofNullable(values.get(contentUri));
  }

  @Override
  public synchronized void remove(String contentUri) {
    validateUri(contentUri);
    values.remove(contentUri);
  }

  private static void validateUri(String contentUri) {
    if (contentUri == null || contentUri.isBlank()) {
      throw new IllegalArgumentException("content URI is required");
    }
  }
}
