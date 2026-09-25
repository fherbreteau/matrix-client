package io.github.fherbreteau.matrix.model;

import io.github.fherbreteau.matrix.json.JsonObject;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantLock;

/**
 * File-backed store for an opaque `/sync` `next_batch` token. Updates use atomic replacement and
 * synchronized access within this store instance. Separate instances and processes targeting the
 * same path are not coordinated. Concurrent updates from multiple processes may overwrite one
 * another. Atomic replacement prevents partially written JSON but cannot guarantee durability
 * against sudden power loss.
 *
 * @see <a href="https://spec.matrix.org/latest/client-server-api/#syncing">Matrix specification</a>
 */
public final class FileSyncTokenStore implements SyncTokenStore {

  private static final String NEXT_BATCH = "next_batch";

  private final AtomicJsonFile file;
  private final ReentrantLock lock = new ReentrantLock();

  /**
   * Creates a file-backed sync-token store at the given path.
   *
   * @param path the file used to persist the token
   */
  public FileSyncTokenStore(Path path) {
    this.file = new AtomicJsonFile(path);
  }

  @Override
  public Optional<String> current() {
    lock.lock();
    try {
      var object = AtomicJsonFile.objectOrEmpty(file.read());
      return Optional.ofNullable(AtomicJsonFile.string(object, NEXT_BATCH));
    } finally {
      lock.unlock();
    }
  }

  @Override
  public void save(String token) {
    if (token == null || token.isBlank()) {
      throw new IllegalArgumentException("sync token is required");
    }
    lock.lock();
    try {
      var object = new JsonObject();
      object.put(NEXT_BATCH, token);
      file.write(object);
    } finally {
      lock.unlock();
    }
  }

  @Override
  public void clear() {
    lock.lock();
    try {
      file.delete();
    } finally {
      lock.unlock();
    }
  }
}
