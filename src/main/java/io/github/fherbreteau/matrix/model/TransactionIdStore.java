package io.github.fherbreteau.matrix.model;

import java.util.Optional;

/**
 * Generates and remembers idempotent transaction IDs for logical operations. Implementations must
 * return the same ID for a repeated operation key, including after process restart when backed by
 * persistent storage. Call {@link #complete(String)} only after the remote operation has succeeded
 * and its result has been durably processed.
 *
 * @see <a href="https://spec.matrix.org/latest/client-server-api/#transaction-identifiers">Matrix
 *     specification</a>
 */
public interface TransactionIdStore {

  /**
   * Returns the transaction ID associated with an operation key, creating one if none exists.
   *
   * @param operationKey a stable identifier for one logical operation
   * @return the existing or newly generated transaction ID
   */
  String getOrCreate(String operationKey);

  /**
   * Removes the mapping once the operation has completed successfully.
   *
   * @param operationKey the logical operation key
   */
  void complete(String operationKey);

  /**
   * Returns the transaction ID associated with the key, if one has been allocated.
   *
   * @param operationKey the logical operation key
   * @return the stored transaction ID, if present
   */
  Optional<String> find(String operationKey);

  /**
   * Creates a new in-memory transaction ID store.
   *
   * @return a new in-memory store
   */
  static TransactionIdStore inMemory() {
    return new InMemoryTransactionIdStore();
  }
}
