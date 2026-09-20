package io.github.fherbreteau.matrix.model;

import java.util.Optional;

/**
 * Injectable store for the authenticated session, so callers decide how (and whether) sessions
 * persist. Implementations must not log access tokens.
 */
public interface SessionStore {

  /** Saves or replaces the current session. */
  void save(Session session);

  /** Returns the current session, if any. */
  Optional<Session> current();

  /** Clears the stored session. */
  void clear();

  /** Creates an empty {@link SessionStore}. */
  static SessionStore create() {
    return new InMemorySessionStore();
  }
}
