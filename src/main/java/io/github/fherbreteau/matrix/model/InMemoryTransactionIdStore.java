package io.github.fherbreteau.matrix.model;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Thread-safe in-memory implementation of {@link TransactionIdStore}.
 *
 * @see <a href="https://spec.matrix.org/latest/client-server-api/#transaction-identifiers">Matrix
 *     specification</a>
 */
public final class InMemoryTransactionIdStore implements TransactionIdStore {

  private final Map<String, String> ids = new HashMap<>();

  @Override
  public synchronized String getOrCreate(String operationKey) {
    validateKey(operationKey);
    return ids.computeIfAbsent(operationKey, _ -> UUID.randomUUID().toString());
  }

  @Override
  public synchronized void complete(String operationKey) {
    validateKey(operationKey);
    ids.remove(operationKey);
  }

  @Override
  public synchronized Optional<String> find(String operationKey) {
    validateKey(operationKey);
    return Optional.ofNullable(ids.get(operationKey));
  }

  private static void validateKey(String operationKey) {
    if (operationKey == null || operationKey.isBlank()) {
      throw new IllegalArgumentException("operation key is required");
    }
  }
}
