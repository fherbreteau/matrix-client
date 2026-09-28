package io.github.fherbreteau.matrix.model;

/** Media source represented by exactly one plaintext URL or encrypted-file descriptor. */
public sealed interface MessageMediaSource permits PlainMediaSource, EncryptedMediaSource {}
