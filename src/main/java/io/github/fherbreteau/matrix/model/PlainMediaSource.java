package io.github.fherbreteau.matrix.model;

/** Unencrypted media source represented by a content URI. */
public record PlainMediaSource(String url) implements MessageMediaSource {}
