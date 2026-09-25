package io.github.fherbreteau.matrix.model;

import io.github.fherbreteau.matrix.json.JsonObject;
import io.github.fherbreteau.matrix.json.JsonParser;
import io.github.fherbreteau.matrix.json.JsonValue;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;

final class AtomicJsonFile {

  private static final Set<PosixFilePermission> OWNER_ONLY =
      EnumSet.of(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE);

  private final Path path;
  private final ReentrantLock lock = new ReentrantLock();

  AtomicJsonFile(Path path) {
    this.path = path.toAbsolutePath().normalize();
  }

  JsonValue read() {
    lock.lock();
    try {
      if (!Files.exists(path)) {
        return null;
      }
      return JsonParser.parse(Files.readString(path, StandardCharsets.UTF_8));
    } catch (IOException exception) {
      throw new IllegalStateException("Unable to read persistence file", exception);
    } finally {
      lock.unlock();
    }
  }

  void write(JsonValue value) {
    lock.lock();
    Path temporary = null;
    try {
      Path parent = path.getParent();
      if (parent != null) {
        Files.createDirectories(parent);
      }
      if (parent == null) {
        parent = Path.of(".").toAbsolutePath().normalize();
      }
      temporary = parent.resolve("." + path.getFileName() + "." + UUID.randomUUID() + ".tmp");
      createRestrictedFile(temporary);
      Files.writeString(temporary, value.toJson(), StandardCharsets.UTF_8);
      moveIntoPlace(temporary);
      restrictFile(path);
    } catch (IOException exception) {
      throw new IllegalStateException("Unable to atomically write persistence file", exception);
    } finally {
      if (temporary != null) {
        try {
          Files.deleteIfExists(temporary);
        } catch (IOException _) {
          temporary.toFile().deleteOnExit();
        }
      }
      lock.unlock();
    }
  }

  void delete() {
    lock.lock();
    try {
      Files.deleteIfExists(path);
    } catch (IOException exception) {
      throw new IllegalStateException("Unable to delete persistence file", exception);
    } finally {
      lock.unlock();
    }
  }

  private void moveIntoPlace(Path temporary) throws IOException {
    try {
      Files.move(
          temporary, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
    } catch (AtomicMoveNotSupportedException _) {
      Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING);
    }
  }

  private static void createRestrictedFile(Path file) throws IOException {
    try {
      Files.createFile(file, PosixFilePermissions.asFileAttribute(OWNER_ONLY));
    } catch (UnsupportedOperationException _) {
      Files.createFile(file);
    }
  }

  private static void restrictFile(Path file) throws IOException {
    if (Files.getFileAttributeView(file, PosixFileAttributeView.class) == null) {
      return;
    }
    Files.setPosixFilePermissions(file, OWNER_ONLY);
  }

  static JsonObject objectOrEmpty(JsonValue value) {
    if (value == null) {
      return new JsonObject();
    }
    if (!value.isObject()) {
      throw new IllegalStateException("Persistence file must contain a JSON object");
    }
    return value.asObject();
  }

  static String requiredString(JsonObject object, String key) {
    JsonValue value = object.get(key);
    if (value == null || !value.isString()) {
      throw new IllegalStateException("Persistence file is missing string field " + key);
    }
    return value.asString();
  }

  static String string(JsonObject object, String key) {
    JsonValue value = object.get(key);
    return value != null && value.isString() ? value.asString() : null;
  }

  static Long longValue(JsonObject object, String key) {
    JsonValue value = object.get(key);
    return value != null && value.isNumber() ? value.asLong() : null;
  }

  static void putNullable(JsonObject object, String key, String value) {
    if (value != null) {
      object.put(key, value);
    }
  }

  static void putNullable(JsonObject object, String key, Long value) {
    if (value != null) {
      object.put(key, value);
    }
  }
}
