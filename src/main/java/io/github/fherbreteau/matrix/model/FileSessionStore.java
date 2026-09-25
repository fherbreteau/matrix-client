package io.github.fherbreteau.matrix.model;

import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantLock;

/**
 * File-backed session store that atomically replaces the serialized session after each update.
 * Reads and writes are synchronized within this store instance; separate instances and processes
 * that target the same path are not coordinated. Concurrent read-modify-write operations from
 * multiple processes may overwrite one another.
 *
 * <p>Writes use a temporary file in the destination directory and an atomic rename when supported
 * by the file system. The session file is restricted to owner read/write permissions on POSIX file
 * systems. Atomic rename does not guarantee that file contents have reached durable storage after
 * sudden power loss.
 *
 * <p>Session files contain access and refresh tokens in plaintext. Protect the containing storage
 * and do not share session files with untrusted users.
 *
 * @see <a href="https://spec.matrix.org/latest/client-server-api/#login">Matrix specification</a>
 */
public final class FileSessionStore implements SessionStore {

  private static final String ACCESS_TOKEN = "access_token";

  private final AtomicJsonFile file;
  private final ReentrantLock lock = new ReentrantLock();

  /**
   * Creates a file-backed session store at the given path.
   *
   * @param path the file used to persist the session
   */
  public FileSessionStore(Path path) {
    this.file = new AtomicJsonFile(path);
  }

  @Override
  public void save(Session session) {
    lock.lock();
    try {
      var object = new io.github.fherbreteau.matrix.json.JsonObject();
      object.put("user_id", session.userId());
      object.put(ACCESS_TOKEN, session.accessToken());
      AtomicJsonFile.putNullable(object, "refresh_token", session.refreshToken());
      AtomicJsonFile.putNullable(object, "expires_in_ms", session.expiresInMs());
      AtomicJsonFile.putNullable(object, "device_id", session.deviceId());
      AtomicJsonFile.putNullable(object, "home_server", session.homeserver());
      file.write(object);
    } finally {
      lock.unlock();
    }
  }

  @Override
  public Optional<Session> current() {
    lock.lock();
    try {
      var object = AtomicJsonFile.objectOrEmpty(file.read());
      if (!object.has(ACCESS_TOKEN)) {
        return Optional.empty();
      }
      var session =
          new Session(
              AtomicJsonFile.requiredString(object, "user_id"),
              AtomicJsonFile.requiredString(object, ACCESS_TOKEN),
              AtomicJsonFile.string(object, "refresh_token"),
              AtomicJsonFile.longValue(object, "expires_in_ms"),
              AtomicJsonFile.string(object, "device_id"),
              AtomicJsonFile.string(object, "home_server"),
              object);
      return Optional.of(session);
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
