package io.github.fherbreteau.matrix.model;

/** An event inside a room. */
public record RoomEvent(String eventId, String sender, String type, String content) {}
