package io.github.fherbreteau.matrix.model;

import io.github.fherbreteau.matrix.json.JsonObject;
import io.github.fherbreteau.matrix.json.JsonValue;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantLock;

/**
 * File-backed application metadata keyed by Matrix content URI. The file uses atomic replacement
 * and owner-only POSIX permissions when available. Access from multiple threads is synchronized per
 * instance; different instances or processes targeting the same file are not coordinated. Atomic
 * rename protects against partial writes but does not guarantee power-loss durability.
 *
 * @see <a href="https://spec.matrix.org/latest/client-server-api/#content-repository">Matrix
 *     specification</a>
 */
public final class FileMediaMetadataStore implements MediaMetadataStore {

  private final AtomicJsonFile file;
  private final ReentrantLock lock = new ReentrantLock();

  /**
   * Creates a file-backed media metadata store.
   *
   * @param path the file used to persist the metadata
   */
  public FileMediaMetadataStore(Path path) {
    this.file = new AtomicJsonFile(path);
  }

  @Override
  public void put(String contentUri, JsonValue metadata) {
    validate(contentUri, metadata);
    lock.lock();
    try {
      JsonObject values = AtomicJsonFile.objectOrEmpty(file.read());
      values.put(contentUri, metadata);
      file.write(values);
    } finally {
      lock.unlock();
    }
  }

  @Override
  public Optional<JsonValue> get(String contentUri) {
    validateUri(contentUri);
    lock.lock();
    try {
      JsonObject values = AtomicJsonFile.objectOrEmpty(file.read());
      return Optional.ofNullable(values.get(contentUri));
    } finally {
      lock.unlock();
    }
  }

  @Override
  public void remove(String contentUri) {
    validateUri(contentUri);
    lock.lock();
    try {
      JsonObject existing = AtomicJsonFile.objectOrEmpty(file.read());
      var remainingValues = new LinkedHashMap<String, JsonValue>();
      existing.entrySet().stream()
          .filter(entry -> !entry.getKey().equals(contentUri))
          .forEach(entry -> remainingValues.put(entry.getKey(), entry.getValue()));
      var remaining = new JsonObject(remainingValues);
      if (remaining.size() != existing.size()) {
        if (remaining.size() == 0) {
          file.delete();
        } else {
          file.write(remaining);
        }
      }
    } finally {
      lock.unlock();
    }
  }

  private static void validate(String contentUri, JsonValue metadata) {
    validateUri(contentUri);
    if (metadata == null) {
      throw new IllegalArgumentException("metadata is required");
    }
  }

  private static void validateUri(String contentUri) {
    if (contentUri == null || contentUri.isBlank()) {
      throw new IllegalArgumentException("content URI is required");
    }
  }
}
