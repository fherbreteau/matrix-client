package io.github.fherbreteau.matrix.model;

import java.util.Optional;

/**
 * An injectable store for an opaque `/sync` `next_batch` token. The token is persisted as an atomic
 * value so a process can resume from the last fully received sync response after restarting.
 */
public interface SyncTokenStore {

  /**
   * Returns the last stored sync token.
   *
   * @return the token or empty if no sync has completed
   */
  Optional<String> current();

  /**
   * Saves the next-batch token after a complete sync response has been received.
   *
   * @param token the opaque sync token
   */
  void save(String token);

  /** Clears the token, causing the next sync call to perform an initial sync. */
  void clear();

  /**
   * Creates an empty in-memory token store.
   *
   * @return a new empty token store
   */
  static SyncTokenStore inMemory() {
    return new InMemorySyncTokenStore();
  }
}
