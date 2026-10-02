package io.github.fherbreteau.matrix.endpoint;

/** Outcome of the Matrix well-known server-discovery flow. */
public enum DiscoveryOutcome {
  DISCOVERED,
  IGNORE,
  FAIL_PROMPT,
  FAIL_ERROR
}
