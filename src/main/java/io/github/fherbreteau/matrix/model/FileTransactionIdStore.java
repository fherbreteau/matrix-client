package io.github.fherbreteau.matrix.model;

import io.github.fherbreteau.matrix.json.JsonObject;
import io.github.fherbreteau.matrix.json.JsonValue;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.LinkedHashMap;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * File-backed idempotency-key to transaction-ID mapping. Data writes use atomic replacement and are
 * synchronized within this store instance. File locks coordinate updates between store instances
 * and processes that target the same path. Transaction IDs are retained across restarts until
 * {@link #complete(String)} removes them. Atomic rename prevents partial JSON; it cannot guarantee
 * durable storage after sudden power loss.
 *
 * <p>The file may reveal application operation keys and should be protected accordingly.
 *
 * @see <a href="https://spec.matrix.org/latest/client-server-api/#transaction-identifiers">Matrix
 *     specification</a>
 */
public final class FileTransactionIdStore implements TransactionIdStore {

  private static final ConcurrentHashMap<Path, ReentrantLock> PROCESS_LOCKS =
      new ConcurrentHashMap<>();

  private final AtomicJsonFile file;
  private final Path lockPath;
  private final ReentrantLock processLock;
  private final ReentrantLock instanceLock = new ReentrantLock();

  /**
   * Creates a file-backed transaction ID store at the given path.
   *
   * @param path the file used to persist mappings
   */
  public FileTransactionIdStore(Path path) {
    this.file = new AtomicJsonFile(path);
    Path normalized = path.toAbsolutePath().normalize();
    this.lockPath = normalized.resolveSibling(normalized.getFileName() + ".lock");
    this.processLock = PROCESS_LOCKS.computeIfAbsent(normalized, _ -> new ReentrantLock());
  }

  @Override
  public String getOrCreate(String operationKey) {
    validateKey(operationKey);
    return withLock(
        () -> {
          var mappings = AtomicJsonFile.objectOrEmpty(file.read());
          var existing = mappings.get(operationKey);
          if (existing != null) {
            if (!existing.isString()) {
              throw new IllegalStateException("Transaction ID mapping must be a string");
            }
            return existing.asString();
          }
          String transactionId = UUID.randomUUID().toString();
          mappings.put(operationKey, transactionId);
          file.write(mappings);
          return transactionId;
        });
  }

  @Override
  public void complete(String operationKey) {
    validateKey(operationKey);
    withLock(
        () -> {
          var mappings = AtomicJsonFile.objectOrEmpty(file.read());
          if (mappings.has(operationKey)) {
            var remaining = new LinkedHashMap<String, JsonValue>();
            mappings.entrySet().stream()
                .filter(entry -> !entry.getKey().equals(operationKey))
                .forEach(entry -> remaining.put(entry.getKey(), entry.getValue()));
            if (remaining.isEmpty()) {
              file.delete();
            } else {
              file.write(new JsonObject(remaining));
            }
          }
          return null;
        });
  }

  @Override
  public Optional<String> find(String operationKey) {
    validateKey(operationKey);
    return withLock(
        () -> {
          var existing = AtomicJsonFile.objectOrEmpty(file.read()).get(operationKey);
          if (existing == null) {
            return Optional.empty();
          }
          if (!existing.isString()) {
            throw new IllegalStateException("Transaction ID mapping must be a string");
          }
          return Optional.of(existing.asString());
        });
  }

  private <T> T withLock(java.util.function.Supplier<T> action) {
    instanceLock.lock();
    processLock.lock();
    try {
      Path parent = lockPath.getParent();
      if (parent != null) {
        Files.createDirectories(parent);
      }
    } catch (IOException exception) {
      processLock.unlock();
      instanceLock.unlock();
      throw new IllegalStateException("Unable to create transaction ID lock directory", exception);
    }
    try (FileChannel channel =
            FileChannel.open(lockPath, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
        FileLock fileLock = channel.lock()) {
      if (!fileLock.isValid()) {
        throw new IllegalStateException("Unable to acquire transaction ID lock");
      }
      return action.get();
    } catch (IOException exception) {
      throw new IllegalStateException("Unable to lock transaction ID persistence file", exception);
    } finally {
      processLock.unlock();
      instanceLock.unlock();
    }
  }

  private static void validateKey(String operationKey) {
    if (operationKey == null || operationKey.isBlank()) {
      throw new IllegalArgumentException("operation key is required");
    }
  }
}
