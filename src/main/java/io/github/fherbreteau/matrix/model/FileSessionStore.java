package io.github.fherbreteau.matrix.model;

import io.github.fherbreteau.matrix.json.JsonObject;
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

  private static final String HOME_SERVER = "home_server";
  private static final String DEVICE_ID = "device_id";
  private static final String EXPIRES_IN_MS = "expires_in_ms";
  private static final String REFRESH_TOKEN = "refresh_token";
  private static final String USER_ID = "user_id";
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
      var object = new JsonObject();
      object.put(USER_ID, session.userId());
      object.put(ACCESS_TOKEN, session.accessToken());
      AtomicJsonFile.putNullable(object, REFRESH_TOKEN, session.refreshToken());
      AtomicJsonFile.putNullable(object, EXPIRES_IN_MS, session.expiresInMs());
      AtomicJsonFile.putNullable(object, DEVICE_ID, session.deviceId());
      AtomicJsonFile.putNullable(object, HOME_SERVER, session.homeserver());
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
              AtomicJsonFile.requiredString(object, USER_ID),
              AtomicJsonFile.requiredString(object, ACCESS_TOKEN),
              AtomicJsonFile.string(object, REFRESH_TOKEN),
              AtomicJsonFile.longValue(object, EXPIRES_IN_MS),
              AtomicJsonFile.string(object, DEVICE_ID),
              AtomicJsonFile.string(object, HOME_SERVER),
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
