package io.github.fherbreteau.matrix.model;

/**
 * Unencrypted media source represented by a content URI.
 *
 * @see <a href="https://spec.matrix.org/latest/client-server-api/#mimage">Matrix image message
 *     specification</a>
 * @see <a href="https://spec.matrix.org/latest/client-server-api/#mfile">Matrix file message
 *     specification</a>
 * @see <a href="https://spec.matrix.org/latest/client-server-api/#maudio">Matrix audio message
 *     specification</a>
 * @see <a href="https://spec.matrix.org/latest/client-server-api/#mvideo">Matrix video message
 *     specification</a>
 */
public record PlainMediaSource(String url) implements MessageMediaSource {}
