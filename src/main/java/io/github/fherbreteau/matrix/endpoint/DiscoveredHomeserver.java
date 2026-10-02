package io.github.fherbreteau.matrix.endpoint;

import io.github.fherbreteau.matrix.json.JsonValue;

/**
 * The result of homeserver discovery: the validated homeserver URL, the optional identity server
 * URL, the raw {@code /.well-known/matrix/client} payload (all unknown fields preserved) and
 * whether the explicit base URL was used as a fallback.
 */
public record DiscoveredHomeserver(
    String homeserverUrl,
    String identityServerUrl,
    JsonValue wellKnown,
    boolean usedFallback,
    DiscoveryOutcome outcome,
    String failureReason) {

  /**
   * Retains the former constructor for source compatibility.
   *
   * @param homeserverUrl the homeserver URL
   * @param identityServerUrl the optional identity server URL
   * @param wellKnown the raw discovery payload
   * @param usedFallback whether a fallback URL was used
   */
  public DiscoveredHomeserver(
      String homeserverUrl, String identityServerUrl, JsonValue wellKnown, boolean usedFallback) {
    this(
        homeserverUrl,
        identityServerUrl,
        wellKnown,
        usedFallback,
        usedFallback ? DiscoveryOutcome.IGNORE : DiscoveryOutcome.DISCOVERED,
        null);
  }
}
