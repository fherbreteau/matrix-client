package io.github.fherbreteau.matrix.model.events;

/**
 * Encrypted media source represented by an {@link EncryptedMediaFile} descriptor.
 *
 * @see <a href="https://spec.matrix.org/latest/client-server-api/#definition-encryptedfile">Matrix
 *     encrypted-file specification</a>
 * @see <a href="https://spec.matrix.org/latest/client-server-api/#mimage">Matrix image message
 *     specification</a>
 * @see <a href="https://spec.matrix.org/latest/client-server-api/#mfile">Matrix file message
 *     specification</a>
 * @see <a href="https://spec.matrix.org/latest/client-server-api/#maudio">Matrix audio message
 *     specification</a>
 * @see <a href="https://spec.matrix.org/latest/client-server-api/#mvideo">Matrix video message
 *     specification</a>
 */
public record EncryptedMediaSource(EncryptedMediaFile file) implements MessageMediaSource {}
