package io.github.fherbreteau.matrix.model;

/**
 * Media source represented by exactly one plaintext URL or encrypted-file descriptor.
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
public sealed interface MessageMediaSource permits PlainMediaSource, EncryptedMediaSource {}
