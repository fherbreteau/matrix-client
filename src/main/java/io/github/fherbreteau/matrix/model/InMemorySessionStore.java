package io.github.fherbreteau.matrix.model;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Thread-safe in-memory {@link SessionStore} keeping at most one session.
 *
 * @see SessionStore
 */
public final class InMemorySessionStore implements SessionStore {

  private final AtomicReference<Session> session = new AtomicReference<>();

  @Override
  public void save(Session session) {
    this.session.set(session);
  }

  @Override
  public Optional<Session> current() {
    return Optional.ofNullable(session.get());
  }

  @Override
  public void clear() {
    session.set(null);
  }
}
