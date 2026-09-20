package io.github.fherbreteau.matrix.model;

import java.util.Optional;

/**
 * Injectable store for the authenticated session, so callers decide how (and whether) sessions
 * persist. Implementations must not log access tokens.
 */
public interface SessionStore {

  /**
   * Saves or replaces the current session.
   *
   * @param session the session to store
   */
  void save(Session session);

  /**
   * Returns the current session, if any.
   *
   * @return the current session, or empty when none
   */
  Optional<Session> current();

  /** Clears the stored session. */
  void clear();

  /**
   * Creates an empty {@link SessionStore}.
   *
   * @return an empty in-memory session store
   */
  static SessionStore create() {
    return new InMemorySessionStore();
  }
}
