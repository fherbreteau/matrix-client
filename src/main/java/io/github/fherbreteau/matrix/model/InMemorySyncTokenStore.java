package io.github.fherbreteau.matrix.model;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

/** Thread-safe in-memory {@link SyncTokenStore} implementation. */
public final class InMemorySyncTokenStore implements SyncTokenStore {

  private final AtomicReference<String> token = new AtomicReference<>();

  @Override
  public Optional<String> current() {
    return Optional.ofNullable(token.get());
  }

  @Override
  public void save(String token) {
    if (token == null || token.isBlank()) {
      throw new IllegalArgumentException("sync token is required");
    }
    this.token.set(token);
  }

  @Override
  public void clear() {
    token.set(null);
  }
}
