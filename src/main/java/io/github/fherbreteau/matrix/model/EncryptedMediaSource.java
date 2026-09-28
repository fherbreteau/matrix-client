package io.github.fherbreteau.matrix.model;

/** Encrypted media source represented by an {@link EncryptedMediaFile} descriptor. */
public record EncryptedMediaSource(EncryptedMediaFile file) implements MessageMediaSource {}
